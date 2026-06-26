package com.github.nols1000.nutaru

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Platform-supplied calendar bounds, in epoch millis.
 *
 * `todayBounds` returns the local-day window for "today" so the dashboard tile
 * matches the user's wall clock; `boundsForDate` parses a `yyyy-MM-dd`. The
 * diary day view also navigates by day offset and drives a Material3
 * `DatePicker` (which speaks UTC-midnight millis), so the platform owns the
 * offset↔bounds and UTC↔local conversions too. Keeping all of this behind an
 * interface means `commonMain` stays free of `java.util.Calendar` /
 * `kotlinx-datetime` — the platform owns timezones. The Android entry point
 * wires the concrete implementation.
 * TODO: replace with `kotlinx-datetime` if more date math lands in `commonMain`.
 */
interface DayBoundsProvider {
    fun todayBounds(): LongRange
    fun boundsForDate(iso: String): LongRange

    /** Local-day window for the day `offsetDays` from today (0 = today, -1 = yesterday). DST-correct. */
    fun boundsForOffset(offsetDays: Int): LongRange

    /** UTC-midnight millis for the local date `offsetDays` from today — the value Material3
     *  `DatePicker` expects as `initialSelectedDateMillis` / returns as `selectedDateMillis`. */
    fun utcMidnightForOffset(offsetDays: Int): Long

    /** Inverse of [utcMidnightForOffset]: given a `DatePicker` UTC-midnight result, the day offset
     *  from today whose local date matches. DST-correct via calendar-date fields, not raw millis. */
    fun offsetForUtcMidnight(millis: Long): Int

    /** Human-readable label for the app bar, e.g. "Today, Wed, Jun 26" or "Wed, Jun 25". */
    fun formatDay(millis: Long): String
}

/** Locale fallback for previews / when no platform locale is wired. */
private object DefaultAppLocaleProvider : LocaleProvider {
    override fun regionTag(): String = ""
}

/**
 * App entry point. Routes between onboarding, the forced recovery reveal, and
 * Home based on persisted state read on launch.
 *
 * Routing uses [relaunchRoute] (issue-04, criterion 5):
 *  - no profile yet              → [OnboardingFlow] (Welcome…Recovery)
 *  - profile saved, mnem not ack → forced [RecoveryRevealScreen] (app was
 *                                  killed before completing the reveal step)
 *  - profile saved, mnem acked   → Home
 *
 * Home's macro tile shows today's consumed macros against the target effective
 * today, so progress — not just raw totals — is the post-onboarding state.
 *
 * `repository == null` renders an empty dashboard — used by tooling previews
 * where no DB is available.
 *
 * `mnemonic == null` falls back to the onboarding/preview path: the platform
 * entry point always supplies it on launch (generated on first launch, reloaded
 * after). Passing null keeps the preview overload working without a mnemonic.
 */
@Composable
fun App(
    repository: NutaruRepository?,
    mnemonic: List<String>?,
    dayBounds: DayBoundsProvider,
    biometricGate: BiometricGate = AlwaysPassBiometricGate,
    packManager: com.github.nols1000.nutaru.pack.PackManager? = null,
    locale: LocaleProvider = DefaultAppLocaleProvider,
    sideLoadPicker: com.github.nols1000.nutaru.pack.SideLoadPicker? = null,
) {
    MaterialTheme {
        var launchRoute by remember { mutableStateOf<RelaunchRoute?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(repository) {
            if (repository != null) {
                val route = withContext(Dispatchers.Default) {
                    relaunchRoute(
                        hasProfile = repository.hasProfile(),
                        mnemonicAcknowledged = repository.isMnemonicAcknowledged(),
                    )
                }
                launchRoute = route
            }
        }

        when {
            repository == null -> DiaryScreen(repository = null, dayBounds = dayBounds, mnemonic = null, biometricGate = AlwaysPassBiometricGate, packManager = null, sideLoadPicker = null)
            launchRoute == null -> {
                // Brief loading state while profile + ack rows are read.
                Column(Modifier.fillMaxSize().safeContentPadding().padding(16.dp)) {
                    Text("nutaru", style = MaterialTheme.typography.headlineMedium)
                }
            }
            launchRoute == RelaunchRoute.ONBOARDING && mnemonic != null -> OnboardingFlow(
                repository = repository,
                mnemonic = mnemonic,
                dayBounds = dayBounds,
                onComplete = { launchRoute = RelaunchRoute.HOME },
                packManager = packManager,
                locale = locale,
            )
            launchRoute == RelaunchRoute.REVEAL && mnemonic != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecoveryRevealScreen(
                    mnemonic = mnemonic,
                    onAcknowledged = {
                        scope.launch {
                            withContext(Dispatchers.Default) { repository.acknowledgeMnemonic() }
                            launchRoute = RelaunchRoute.HOME
                        }
                    },
                )
            }
            else -> DiaryScreen(repository = repository, dayBounds = dayBounds, mnemonic = mnemonic, biometricGate = biometricGate, packManager = packManager, sideLoadPicker = sideLoadPicker)
        }
    }
}

/**
 * Pager center page. `Int.MAX_VALUE` pages centred here so swiping never hits
 * an edge in practice; page `p` maps to day offset `p - [PAGE_CENTER]` (0 = today).
 */
private const val PAGE_CENTER: Int = Int.MAX_VALUE / 2

/** One loaded day of the diary: raw entries (for edit/delete lookup), meal
 *  groups, the rolled-up day total, and the target effective that day. */
private data class DayData(
    val entries: List<DiaryEntry>,
    val groups: List<MealGroup>,
    val total: MacroTotals,
    val target: MacroTarget?,
)

/**
 * Diary day view (issue-05). A [HorizontalPager] of days centred on today,
 * a top app bar showing the current date (tap → [DatePickerDialog] for an
 * arbitrary jump), meal-grouped entry rows with per-meal subtotals, a day-total
 * card with kcal + P/C/F vs target and a progress bar, and long-press →
 * edit/delete on each entry.
 *
 * Each page loads its entries off the main thread and caches in `pages`; the
 * current page and its neighbours are prefetched so a swipe lands on already-
 * loaded content. Writes (add/edit/delete) invalidate + reload the current page.
 *
 * `repository == null` (tooling preview) renders a static empty day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryScreen(
    repository: NutaruRepository?,
    dayBounds: DayBoundsProvider,
    mnemonic: List<String>?,
    biometricGate: BiometricGate,
    packManager: com.github.nols1000.nutaru.pack.PackManager?,
    sideLoadPicker: com.github.nols1000.nutaru.pack.SideLoadPicker?,
) {
    val pagerState = rememberPagerState(initialPage = PAGE_CENTER, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()
    // TODO: unbounded cache — one entry per swiped-to day lives for the session.
    //   Fine in practice (users swipe a bounded range), but a multi-year jump via
    //   the date picker followed by heavy swiping could grow it. Upgrade path: LRU
    //   cap keyed on page index, evicting far-from-current pages.
    val pages = remember { mutableStateMapOf<Int, DayData>() }
    val foods = remember { mutableStateOf<List<ProductMacros>>(emptyList()) }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var actionEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showPacks by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    suspend fun loadPage(page: Int) {
        if (repository == null) return
        val offset = page - PAGE_CENTER
        val bounds = dayBounds.boundsForOffset(offset)
        val data = withContext(Dispatchers.Default) {
            val entries = repository.entriesForDay(bounds.first, bounds.last)
            val groups = MacroRollup.groupByMeal(entries)
            DayData(entries, groups, MacroRollup.dayTotal(groups), repository.targetFor(bounds.first))
        }
        pages[page] = data
    }

    fun refreshCurrent() {
        scope.launch { loadPage(pagerState.currentPage) }
    }

    LaunchedEffect(repository) {
        if (repository != null) {
            foods.value = withContext(Dispatchers.Default) { repository.allFoods() }
            loadPage(pagerState.currentPage)
        }
    }

    // Prefetch the current page + neighbours so a swipe lands on loaded content.
    LaunchedEffect(pagerState.currentPage, repository) {
        if (repository == null) return@LaunchedEffect
        val center = pagerState.currentPage
        for (p in intArrayOf(center - 1, center, center + 1)) {
            if (!pages.containsKey(p)) loadPage(p)
        }
    }

    val currentOffset = pagerState.currentPage - PAGE_CENTER
    val currentBounds = dayBounds.boundsForOffset(currentOffset)
    val dateLabel = dayBounds.formatDay(currentBounds.first)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        dateLabel,
                        modifier = Modifier.clickable { showDatePicker = true },
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Log food")
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            val data = pages[page]
            when {
                data != null -> DayPage(data, onEntryLongPress = { id ->
                    actionEntry = data.entries.firstOrNull { it.id == id }
                })
                repository != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> DayPage(
                    DayData(emptyList(), emptyList(), MacroTotals.ZERO, null),
                    onEntryLongPress = { },
                )
            }
        }
    }

    if (showForm) {
        LogFoodForm(
            foods = foods.value,
            editing = editing,
            onCommit = { productId, grams, mealType ->
                scope.launch {
                    if (repository != null) {
                        val edit = editing
                        if (edit != null) {
                            withContext(Dispatchers.Default) {
                                repository.updateEntry(
                                    id = edit.id,
                                    productId = productId,
                                    quantity = edit.quantity,
                                    unit = edit.unit,
                                    quantityGrams = grams,
                                    mealType = mealType,
                                    timestampMillis = edit.timestampMillis,
                                )
                            }
                        } else {
                            withContext(Dispatchers.Default) {
                                repository.logFood(
                                    productId = productId,
                                    quantity = 1.0,
                                    unit = "servings",
                                    quantityGrams = grams,
                                    mealType = mealType,
                                    source = "manual",
                                    timestampMillis = currentBounds.first + 60_000,
                                )
                            }
                        }
                        refreshCurrent()
                    }
                    showForm = false
                    editing = null
                }
            },
            onDismiss = { showForm = false; editing = null },
        )
    }

    actionEntry?.let { entry ->
        EntryActionsDialog(
            entry = entry,
            onEdit = { actionEntry = null; editing = entry; showForm = true },
            onDelete = { actionEntry = null; deleteEntry = entry },
            onDismiss = { actionEntry = null },
        )
    }

    deleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntry = null },
            title = { Text("Delete entry") },
            text = { Text("Delete \"${entry.name}\"? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteEntry = null
                    scope.launch {
                        if (repository != null) {
                            withContext(Dispatchers.Default) { repository.deleteEntry(entry.id) }
                            refreshCurrent()
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteEntry = null }) { Text("Cancel") } },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dayBounds.utcMidnightForOffset(currentOffset),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val offset = dayBounds.offsetForUtcMidnight(millis)
                        scope.launch { pagerState.scrollToPage(PAGE_CENTER + offset) }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showSettings && mnemonic != null) {
        SettingsScreen(
            mnemonic = mnemonic,
            biometricGate = biometricGate,
            onDismiss = { showSettings = false },
            onManagePacks = { showSettings = false; showPacks = true },
        )
    }

    if (showPacks && packManager != null) {
        PacksScreen(
            packManager = packManager,
            sideLoadPicker = sideLoadPicker,
            onDismiss = { showPacks = false },
        )
    }
}

@Composable
private fun DayPage(data: DayData, onEntryLongPress: (Long) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DayTotalCard(data.total, data.target)
        if (data.groups.isEmpty()) {
            Text(
                "No entries yet. Tap + to log a meal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            data.groups.forEach { MealSection(it, onEntryLongPress) }
        }
    }
}

@Composable
private fun DayTotalCard(totals: MacroTotals, target: MacroTarget?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Day total", style = MaterialTheme.typography.labelLarge)
            if (target == null) {
                Text(
                    "${totals.kcal.toInt()} kcal",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    "${totals.kcal.toInt()} / ${target.kcal.toInt()} kcal",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                val remaining = target.kcal - totals.kcal
                Text(
                    if (remaining >= 0) "${remaining.toInt()} kcal remaining"
                    else "${(-remaining).toInt()} kcal over",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MacroChip("P", totals.proteinG, target?.proteinG, Modifier.weight(1f))
                MacroChip("C", totals.carbsG, target?.carbsG, Modifier.weight(1f))
                MacroChip("F", totals.fatG, target?.fatG, Modifier.weight(1f))
            }
            if (target != null && target.kcal > 0.0) {
                LinearProgressIndicator(
                    progress = { normalizedProgress(totals.kcal.toFloat(), target.kcal.toFloat()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MealSection(group: MealGroup, onEntryLongPress: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(group.mealType.display, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${group.subtotal.kcal.toInt()} kcal · P${group.subtotal.proteinG.toInt()}/C${group.subtotal.carbsG.toInt()}/F${group.subtotal.fatG.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        group.rows.forEach { row ->
            EntryRow(row, onLongPress = { onEntryLongPress(row.entryId) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(row: MealRow, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onLongPress, onLongClick = onLongPress)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${row.quantity} ${row.unit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${row.macros.kcal.toInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
            )
            Text(
                "P${row.macros.proteinG.toInt()} C${row.macros.carbsG.toInt()} F${row.macros.fatG.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntryActionsDialog(
    entry: DiaryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${entry.quantity} ${entry.unit} · ${entry.mealType}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Edit") }
                    OutlinedButton(onClick = onDelete) { Text("Delete") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RowScope.MacroChip(label: String, grams: Double, targetG: Double?, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        if (targetG == null) {
            Text("${grams.toInt()} g", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("${grams.toInt()} / ${targetG.toInt()} g", style = MaterialTheme.typography.titleMedium)
            if (targetG > 0.0) {
                val over = grams > targetG
                Text(
                    if (over) "over" else "under",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Add/edit entry form (issue-05). Picks a seeded food, enters grams, and tags a
 * meal type (breakfast/lunch/dinner/snack) so the diary can group it.
 *
 * Replaces the tracer-bullet's free-form `yyyy-MM-dd` date field: the entry
 * lands on the currently viewed diary day (add) or keeps its original day
 * (edit), so no manual date entry is needed. Pre-fills every field when
 * [editing] is non-null (criterion 6).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogFoodForm(
    foods: List<ProductMacros>,
    editing: DiaryEntry?,
    onCommit: (productId: Long, grams: Double, mealType: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember {
        mutableStateOf(foods.firstOrNull { it.id == editing?.productId } ?: foods.firstOrNull())
    }
    var gramsText by remember { mutableStateOf((editing?.quantityGrams ?: 100.0).toString()) }
    var mealType by remember { mutableStateOf(editing?.mealType ?: "snack") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "Log food" else "Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Item", style = MaterialTheme.typography.labelLarge)
                foods.forEach { food ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = selected?.id == food.id,
                            onClick = { selected = food },
                        )
                        Text(food.name, modifier = Modifier.weight(1f))
                        Text(
                            "${food.kcalPer100g.toInt()} kcal/100g",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                        )
                    }
                }
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Quantity (grams)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Meal", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.ORDER.forEach { mt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mealType == mt.key,
                                onClick = { mealType = mt.key },
                            )
                            Text(mt.display)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val product = selected ?: return@TextButton
                    val grams = gramsText.toDoubleOrNull() ?: return@TextButton
                    if (grams <= 0.0) return@TextButton
                    onCommit(product.id, grams, mealType)
                },
            ) { Text(if (editing == null) "Commit" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Minimal Settings screen for the mnemonic re-view entry (issue-04, criterion
 * 3). The full Settings tab (theme, units, model swap, backup, hard-delete)
 * lands with issue-14; this is the slice the mnemonic-reveal issue needs.
 *
 * Tapping "View recovery mnemonic" runs the [BiometricGate] prompt; on
 * confirmation the 12 words are shown in a read-only card (copy disabled,
 * same as onboarding). On failure/cancel the words stay hidden.
 */
@Composable
private fun SettingsScreen(
    mnemonic: List<String>,
    biometricGate: BiometricGate,
    onDismiss: () -> Unit,
    onManagePacks: () -> Unit,
) {
    var revealed by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Food data", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = onManagePacks, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage food packs")
                }
                Text("Recovery", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = {
                        authError = false
                        biometricGate.authenticate(
                            onSuccess = { revealed = true },
                            onFailure = { authError = true },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("View recovery mnemonic") }

                if (authError) {
                    Text(
                        "Authentication cancelled. Try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (revealed) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            mnemonic.forEachIndexed { i, word ->
                                Text("${i + 1}. $word", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Text(
                        "Write these down again if needed. Copy is disabled.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
