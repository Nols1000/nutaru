package com.github.nols1000.nutaru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Onboarding: Welcome + Profile + Plan + Recovery reveal, with Pack and Agent
 * as visible "skip — configure later" placeholders (wired in their respective
 * slices).
 *
 * Step flow: WELCOME → PROFILE → PLAN (persists profile + weight + target) →
 * PACK (skip) → AGENT (skip) → RECOVERY (persists mnemonic ack) → [onComplete]
 * → Home.
 *
 * Data is committed at the PLAN step so the "skip Pack / skip Agent" paths
 * still complete onboarding with profile + targets set even if the app is
 * killed during a placeholder. `effective_from` and `created_at` use today's
 * day start from [DayBoundsProvider] so the target is effective immediately.
 * The RECOVERY step persists the acknowledgment flag last, so a kill before
 * that point relaunches to the reveal screen (see [relaunchRoute]).
 *
 * All DB writes are dispatched off the UI thread, matching the existing App.
 */
@Composable
fun OnboardingFlow(
    repository: NutaruRepository,
    mnemonic: List<String>,
    dayBounds: DayBoundsProvider,
    onComplete: () -> Unit,
    wordIndexPicker: WordIndexPicker = DefaultWordIndexPicker,
    packManager: com.github.nols1000.nutaru.pack.PackManager? = null,
    locale: LocaleProvider = DefaultLocaleProvider,
) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var profile by remember { mutableStateOf<Profile?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (step) {
            OnboardingStep.WELCOME -> WelcomeScreen(onBegin = { step = OnboardingStep.PROFILE })
            OnboardingStep.PROFILE -> ProfileScreen(
                onContinue = { p ->
                    profile = p
                    step = OnboardingStep.PLAN
                },
            )
            OnboardingStep.PLAN -> {
                val p = profile
                if (p == null) {
                    // Defensive: the flow sets `profile` before reaching PLAN.
                    // Re-enter the profile step instead of crashing.
                    ProfileScreen(onContinue = { np -> profile = np; step = OnboardingStep.PLAN })
                } else {
                    PlanScreen(
                        profile = p,
                        onContinue = { target, source ->
                            scope.launch {
                                val today = dayBounds.todayBounds().first
                                withContext(Dispatchers.Default) {
                                    repository.saveOnboardingProfile(p, nowMillis = today)
                                    repository.saveTarget(target, today, source, today)
                                }
                                step = OnboardingStep.PACK
                            }
                        },
                    )
                }
            }
            OnboardingStep.PACK -> PackStep(
                packManager = packManager,
                locale = locale,
                onBrowse = { step = OnboardingStep.PACK_BROWSE },
                onSkip = { step = OnboardingStep.AGENT },
            )
            OnboardingStep.PACK_BROWSE -> if (packManager != null) {
                PacksScreen(
                    packManager = packManager,
                    sideLoadPicker = null,
                    onDismiss = { step = OnboardingStep.AGENT },
                )
            } else {
                PlaceholderScreen(
                    title = "Food packs",
                    body = "Install regional food packs later in Settings.",
                    onSkip = { step = OnboardingStep.AGENT },
                )
            }
            OnboardingStep.AGENT -> PlaceholderScreen(
                title = "Intelligent local assistant",
                body = "Enable the on-device assistant later in Settings. nutaru works " +
                    "fully in nutrition-only mode until then.",
                onSkip = { step = OnboardingStep.RECOVERY },
            )
            OnboardingStep.RECOVERY -> RecoveryRevealScreen(
                mnemonic = mnemonic,
                wordIndexPicker = wordIndexPicker,
                onAcknowledged = {
                    scope.launch {
                        withContext(Dispatchers.Default) { repository.acknowledgeMnemonic() }
                        onComplete()
                    }
                },
            )
        }
    }
}

private enum class OnboardingStep { WELCOME, PROFILE, PLAN, PACK, PACK_BROWSE, AGENT, RECOVERY }

/** Locale fallback used by previews / when no platform locale is wired. */
private object DefaultLocaleProvider : LocaleProvider {
    override fun regionTag(): String = ""
}

/**
 * Onboarding Pack step (issue-07 criterion 5 + 6): suggests the locale-matching
 * pack with a one-tap install, offers "browse more" (→ [PacksScreen]), and a
 * skip that completes onboarding without installing anything.
 *
 * When [packManager] is null (previews / no network seam wired), the step
 * degrades to a skip-only placeholder so the rest of onboarding still works. */
@Composable
private fun PackStep(
    packManager: com.github.nols1000.nutaru.pack.PackManager?,
    locale: LocaleProvider,
    onBrowse: () -> Unit,
    onSkip: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var suggested by remember { mutableStateOf<com.github.nols1000.nutaru.pack.CatalogEntry?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.0) }

    LaunchedEffect(packManager) {
        if (packManager == null) return@LaunchedEffect
        // Suggest the locale-matching pack; fall back to the first catalog entry.
        scope.launch {
            val cat = runCatching { withContext(Dispatchers.Default) { packManager.catalog() } }.getOrNull()
            val region = locale.regionTag().uppercase()
            suggested = cat?.packs?.firstOrNull { it.region.equals(region, ignoreCase = true) }
                ?: cat?.packs?.firstOrNull()
        }
    }

    Text("Food packs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Install a starter pack of common foods for your region, or skip and " +
                    "add packs later in Settings. You can log manually right away either way.",
                style = MaterialTheme.typography.bodyMedium,
            )
            suggested?.let { s ->
                HorizontalDivider()
                Text(s.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${s.itemCount} items · ${s.license}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (installing) {
                    LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                }
                if (!installing) {
                    Button(
                        onClick = {
                            installing = true; progress = 0.0; status = null
                            scope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    packManager!!.install(s) { progress = it }
                                }
                                installing = false
                                status = when (result) {
                                    is com.github.nols1000.nutaru.pack.PackInstallResult.Success ->
                                        "Installed ${result.pack.itemCount} items."
                                    is com.github.nols1000.nutaru.pack.PackInstallResult.Failure ->
                                        result.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Install \"${s.name}\"") }
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
    OutlinedButton(onClick = onBrowse, modifier = Modifier.fillMaxWidth(), enabled = packManager != null) {
        Text("Browse more packs")
    }
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Skip — add later")
    }
}

@Composable
private fun WelcomeScreen(onBegin: () -> Unit) {
    Text("nutaru", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(
        "Local-first nutrition tracking with an intelligent local assistant. " +
            "No account. No cloud. No subscription.",
        style = MaterialTheme.typography.titleMedium,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Stays on your device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Your profile, weight, and food log are encrypted on this device. " +
                    "Nothing is uploaded, ever. You can export or erase everything anytime.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Button(onClick = onBegin, modifier = Modifier.fillMaxWidth()) {
        Text("Begin")
    }
}

@Composable
private fun ProfileScreen(onContinue: (Profile) -> Unit) {
    var ageText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf<Sex?>(null) }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf<Goal?>(null) }
    var activity by remember { mutableStateOf<ActivityLevel?>(null) }

    val age = ageText.toIntOrNull()
    val height = heightText.toIntOrNull()
    val weight = weightText.toDoubleOrNull()

    val ageValid = age != null && age in 13..100
    val heightValid = height != null && height in 100..250
    val weightValid = weight != null && weight in 30.0..300.0
    val formValid = ageValid && sex != null && heightValid && weightValid && goal != null && activity != null

    Text("Your profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Text("Stored only on this device.", style = MaterialTheme.typography.bodySmall)

    OutlinedTextField(
        value = ageText,
        onValueChange = { ageText = it.filter { c -> c.isDigit() } },
        label = { Text("Age (years)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = ageText.isNotEmpty() && !ageValid,
        supportingText = { if (ageText.isNotEmpty() && !ageValid) Text("Enter an age between 13 and 100") },
        modifier = Modifier.fillMaxWidth(),
    )

    ChoiceGroup(
        label = "Sex",
        options = Sex.entries.map { it.name to it },
        selected = sex,
        onSelect = { sex = it },
    )

    OutlinedTextField(
        value = heightText,
        onValueChange = { heightText = it.filter { c -> c.isDigit() } },
        label = { Text("Height (cm)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = heightText.isNotEmpty() && !heightValid,
        supportingText = { if (heightText.isNotEmpty() && !heightValid) Text("Enter a height between 100 and 250 cm") },
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = weightText,
        onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("Current weight (kg)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = weightText.isNotEmpty() && !weightValid,
        supportingText = { if (weightText.isNotEmpty() && !weightValid) Text("Enter a weight between 30 and 300 kg") },
        modifier = Modifier.fillMaxWidth(),
    )

    ChoiceGroup(
        label = "Goal",
        options = Goal.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } to it },
        selected = goal,
        onSelect = { goal = it },
    )

    ChoiceGroup(
        label = "Activity level",
        options = ActivityLevel.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } to it },
        selected = activity,
        onSelect = { activity = it },
    )

    Button(
        onClick = {
            if (formValid) {
                onContinue(Profile(age, sex!!, height, weight, goal!!, activity!!))
            }
        },
        enabled = formValid,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Continue") }
}

@Composable
private fun <T> ChoiceGroup(
    label: String,
    options: List<Pair<String, T>>,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        options.forEach { (text, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(selected = selected == value, onClick = { onSelect(value) })
                Text(text)
            }
        }
    }
}

@Composable
private fun PlanScreen(
    profile: Profile,
    onContinue: (MacroTarget, String) -> Unit,
) {
    val reasoning = remember(profile) { TargetCalc.calculate(profile) }
    var kcalText by remember { mutableStateOf(reasoning.target.kcal.toInt().toString()) }
    var proteinText by remember { mutableStateOf(reasoning.target.proteinG.toInt().toString()) }
    var carbsText by remember { mutableStateOf(reasoning.target.carbsG.toInt().toString()) }
    var fatText by remember { mutableStateOf(reasoning.target.fatG.toInt().toString()) }
    var overridden by remember { mutableStateOf(false) }

    Text("Your daily plan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("How this is calculated", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            ReasoningRow("BMR (Mifflin-St Jeor)", "${reasoning.bmr.toInt()} kcal")
            ReasoningRow("Activity factor", "× ${reasoning.activityFactor}")
            ReasoningRow("TDEE", "${reasoning.tdee.toInt()} kcal")
            val deltaLabel = when {
                reasoning.goalDeltaKcal < 0 -> "${reasoning.goalDeltaKcal.toInt()} kcal (${profile.goal.name.lowercase()})"
                reasoning.goalDeltaKcal > 0 -> "+${reasoning.goalDeltaKcal.toInt()} kcal (${profile.goal.name.lowercase()})"
                else -> "0 kcal (maintain)"
            }
            ReasoningRow("Goal adjustment", deltaLabel)
            HorizontalDivider()
            ReasoningRow("Daily target", "${reasoning.target.kcal.toInt()} kcal")
            Text(
                "Protein ${reasoning.proteinPerKg} g/kg \u2192 ${reasoning.target.proteinG.toInt()} g; " +
                    "fat 25% of kcal \u2192 ${reasoning.target.fatG.toInt()} g; " +
                    "carbs remainder \u2192 ${reasoning.target.carbsG.toInt()} g.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    Text("Adjust (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    OutlinedTextField(
        value = kcalText,
        onValueChange = { kcalText = it.filter { c -> c.isDigit() }; overridden = true },
        label = { Text("kcal") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OverrideField("Protein (g)", proteinText, Modifier.weight(1f)) { proteinText = it; overridden = true }
        OverrideField("Carbs (g)", carbsText, Modifier.weight(1f)) { carbsText = it; overridden = true }
        OverrideField("Fat (g)", fatText, Modifier.weight(1f)) { fatText = it; overridden = true }
    }

    if (overridden) {
        OutlinedButton(
            onClick = {
                kcalText = reasoning.target.kcal.toInt().toString()
                proteinText = reasoning.target.proteinG.toInt().toString()
                carbsText = reasoning.target.carbsG.toInt().toString()
                fatText = reasoning.target.fatG.toInt().toString()
                overridden = false
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset to calculated") }
    }

    val targetKcal = kcalText.toDoubleOrNull()
    val targetValid = targetKcal != null && targetKcal > 0.0 &&
        proteinText.toDoubleOrNull() != null &&
        carbsText.toDoubleOrNull() != null &&
        fatText.toDoubleOrNull() != null

    Button(
        onClick = {
            if (targetValid) {
                val target = MacroTarget(
                    kcal = targetKcal,
                    proteinG = proteinText.toDouble(),
                    carbsG = carbsText.toDouble(),
                    fatG = fatText.toDouble(),
                )
                onContinue(target, if (overridden) "manual" else "algorithm")
            }
        },
        enabled = targetValid,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Continue") }
}

@Composable
private fun ReasoningRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OverrideField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun PlaceholderScreen(title: String, body: String, onSkip: () -> Unit) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Text(
                "This step lands in a later update.",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Skip \u2014 configure later")
    }
}
