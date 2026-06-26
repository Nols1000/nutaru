package com.github.nols1000.nutaru

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.github.nols1000.nutaru.crypto.Bip39
import com.github.nols1000.nutaru.crypto.MnemonicStore
import com.github.nols1000.nutaru.db.EncryptedDatabase
import com.github.nols1000.nutaru.pack.AndroidPackFetcher
import com.github.nols1000.nutaru.pack.PackManager
import com.github.nols1000.nutaru.pack.SideLoadPicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : FragmentActivity() {

    private lateinit var pickPackLauncher: ActivityResultLauncher<Array<String>>
    private var pendingPickCallback: ((ByteArray, String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // First launch: generate + persist mnemonic. Subsequent launches: reload.
        // Same mnemonic → same Argon2id key → existing encrypted DB reopens.
        var mnemonic = MnemonicStore.load()
        if (mnemonic == null) {
            mnemonic = Bip39.generateMnemonic()
            MnemonicStore.store(mnemonic)
        }

        val db = EncryptedDatabase.open(path = "nutaru.db", mnemonic = mnemonic)
        val repository = NutaruRepository(db).also { it.seedFoods() } // idempotent across relaunches
        val packManager = PackManager(repository, AndroidPackFetcher) { System.currentTimeMillis() }

        // OS file picker for side-loading .pack files (issue-07 criterion 8).
        pickPackLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val cb = pendingPickCallback
            pendingPickCallback = null
            if (uri == null || cb == null) return@registerForActivityResult
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@registerForActivityResult
            val name = uri.lastPathSegment?.substringAfterLast('/')?.let { Uri.decode(it) } ?: "pack.pack"
            cb(bytes, name)
        }

        setContent {
            App(
                repository = repository,
                mnemonic = mnemonic,
                dayBounds = AndroidDayBoundsProvider,
                biometricGate = AndroidBiometricGate(this),
                packManager = packManager,
                locale = AndroidLocaleProvider,
                sideLoadPicker = AndroidSideLoadPicker(this, pickPackLauncher) { cb -> pendingPickCallback = cb },
            )
        }
    }
}

/** Launches the SAF document picker for `*.pack` files. */
private class AndroidSideLoadPicker(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<Array<String>>,
    private val stashCallback: ((ByteArray, String) -> Unit) -> Unit,
) : SideLoadPicker {
    override fun pickPackFile(onPicked: (ByteArray, String) -> Unit) {
        stashCallback(onPicked)
        launcher.launch(arrayOf("application/octet-stream", "*/*"))
    }
}

/** Device region from `java.util.Locale` → catalog region tag (e.g. "US", "GB"). */
private object AndroidLocaleProvider : LocaleProvider {
    override fun regionTag(): String = Locale.getDefault().country
}

/**
 * AndroidX BiometricPrompt wiring for [BiometricGate]. Falls back to device
 * credential (PIN/pattern/password) when no biometric hardware is enrolled,
 * so the mnemonic re-view is gated on every device (issue-04, criterion 3).
 *
 * BiometricPrompt requires a `FragmentActivity` host and an executor; both are
 * bound here so the common UI stays free of Android imports.
 */
private class AndroidBiometricGate(private val activity: FragmentActivity) : BiometricGate {

    override fun authenticate(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val canAuth = BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometric/credential available. Fail closed: do not reveal the
            // mnemonic without auth. TODO(issue-14): surface a user-readable
            // message and an enrollment prompt.
            onFailure()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure()
                override fun onAuthenticationFailed() { /* per-attempt; callback waits for error or success */ }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("View recovery mnemonic")
                .setDescription("Authenticate to view your 12-word recovery mnemonic.")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build(),
        )
    }
}

/**
 * Local-timezone day windows via `java.util.Calendar` so the diary's "today"
 * matches the user's wall clock and the `DatePicker` (UTC-midnight) round-trip
 * lands on the right local day.
 *
 * Day offsets are computed with `Calendar.add(DAY_OF_MONTH, n)` so a day window
 * is DST-correct (a day may be 23 or 25 hours — the end is the *next* local
 * midnight, not a flat +24h). The UTC↔local conversions feed Material3's
 * `DatePicker`, which speaks UTC-midnight millis. `daysBetween` uses Julian Day
 * Numbers from the local date fields (not raw millis) so a DST transition
 * straddling two midnights can't corrupt the count.
 *
 * Implementation lives in androidApp (not commonMain) so KMP `commonMain` never
 * imports `java.util.Calendar`.
 */
object AndroidDayBoundsProvider : DayBoundsProvider {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    override fun todayBounds(): LongRange = boundsForOffset(0)

    override fun boundsForDate(iso: String): LongRange {
        val parts = iso.split('-')
        if (parts.size != 3) return todayBounds()
        val year = parts[0].toIntOrNull() ?: return todayBounds()
        val month = parts[1].toIntOrNull() ?: return todayBounds()
        val day = parts[2].toIntOrNull() ?: return todayBounds()
        return boundsFor(year, month - 1, day)
    }

    override fun boundsForOffset(offsetDays: Int): LongRange {
        val cal = Calendar.getInstance().apply {
            clear()
            val now = Calendar.getInstance()
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, now.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, offsetDays)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start until cal.timeInMillis
    }

    override fun utcMidnightForOffset(offsetDays: Int): Long {
        // Local date at the offset, expressed as UTC midnight of that Y/M/D.
        val local = boundsForOffset(offsetDays)
        val cal = Calendar.getInstance().apply { clear(); timeInMillis = local.first }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return Calendar.getInstance(utc).apply {
            clear()
            set(y, m, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    override fun offsetForUtcMidnight(millis: Long): Int {
        // The picker returns UTC midnight; interpret it as a UTC date, then find
        // the local-day offset whose local date matches that Y/M/D.
        val utcCal = Calendar.getInstance(utc).apply { clear(); timeInMillis = millis }
        val y = utcCal.get(Calendar.YEAR)
        val m = utcCal.get(Calendar.MONTH)
        val d = utcCal.get(Calendar.DAY_OF_MONTH)
        val localStart = boundsFor(y, m, d).first
        return daysBetween(boundsForOffset(0).first, localStart)
    }

    override fun formatDay(millis: Long): String {
        val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))
        return if (millis in boundsForOffset(0)) "Today, $date" else date
    }

    private fun boundsFor(year: Int, monthZeroIndexed: Int, day: Int): LongRange {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, monthZeroIndexed, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start until cal.timeInMillis
    }

    private fun daysBetween(aStartMillis: Long, bStartMillis: Long): Int {
        val a = Calendar.getInstance().apply { timeInMillis = aStartMillis }
        val b = Calendar.getInstance().apply { timeInMillis = bStartMillis }
        return julianDay(b) - julianDay(a)
    }

    /** Julian Day Number from local calendar date fields — DST-immune day count. */
    private fun julianDay(cal: Calendar): Int {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(repository = null, mnemonic = null, dayBounds = AndroidDayBoundsProvider)
}