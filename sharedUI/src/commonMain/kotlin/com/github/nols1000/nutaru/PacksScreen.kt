package com.github.nols1000.nutaru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.github.nols1000.nutaru.pack.Catalog
import com.github.nols1000.nutaru.pack.CatalogEntry
import com.github.nols1000.nutaru.pack.PackFetchException
import com.github.nols1000.nutaru.pack.PackInstallResult
import com.github.nols1000.nutaru.pack.PackManager
import com.github.nols1000.nutaru.pack.SideLoadPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One pack's install state — what the list row renders. The catalog entry is
 * the network shape; [installing] / [progress] are the live install telemetry. */
private data class PackRowState(
    val entry: CatalogEntry,
    val installed: Boolean,
    val installing: Boolean = false,
    val progress: Double = 0.0,
    val error: String? = null,
)

/**
 * Settings → Packs management (issue-07 criteria 7 + 8): browse the catalog,
 * install (background download with progress, app stays usable), uninstall
 * (removes only that pack's products), update, and side-load via the OS file
 * picker.
 *
 * Catalog fetch + install run off the main thread via [PackManager]; progress
 * flows back as state so the UI re-renders the in-flight row without blocking.
 * The dialog stays open across an install so the user can keep browsing — the
 * app remains usable (criterion 2).
 */
@Composable
fun PacksScreen(
    packManager: PackManager,
    sideLoadPicker: SideLoadPicker?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<PackRowState>>(emptyList()) }
    var sideLoadStatus by remember { mutableStateOf<String?>(null) }

    // Initial load: installed packs (DB, cheap) then catalog (network).
    LaunchedEffect(Unit) {
        val installed = withContext(Dispatchers.Default) { packManager.installedPacks() }
        val installedIds = installed.map { it.id }.toSet()
        try {
            val cat = withContext(Dispatchers.Default) { packManager.catalog() }
            catalog = cat
            rows = cat.packs.map { e -> PackRowState(e, installed = e.id in installedIds) }
        } catch (e: PackFetchException) {
            catalogError = e.message ?: "Could not load the pack catalog."
        }
        // Even on catalog failure, show what's installed so uninstall/update work.
        if (rows.isEmpty()) {
            rows = installed.map { p ->
                PackRowState(
                    entry = CatalogEntry(
                        id = p.id, name = p.name, version = p.version, region = p.region ?: "",
                        itemCount = p.itemCount, byteSize = p.byteSize, sha256 = p.sha256 ?: "",
                        url = "", license = p.license ?: "", attribution = p.attribution ?: "",
                    ),
                    installed = true,
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Food packs") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                catalogError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (catalog == null && catalogError == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Loading catalog…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                rows.forEachIndexed { index, _ ->
                    PackRow(
                        state = rows[index],
                        onInstall = { entry ->
                            scope.launch {
                                rows = rows.toMutableList().also { it[index] = it[index].copy(installing = true, progress = 0.0, error = null) }
                                val result = withContext(Dispatchers.Default) {
                                    packManager.install(entry) { p ->
                                        rows = rows.toMutableList().also { it[index] = it[index].copy(progress = p) }
                                    }
                                }
                                rows = rows.toMutableList().also {
                                    it[index] = when (result) {
                                        is PackInstallResult.Success -> it[index].copy(installed = true, installing = false, progress = 1.0)
                                        is PackInstallResult.Failure -> it[index].copy(installing = false, error = result.message)
                                    }
                                }
                            }
                        },
                        onUninstall = { entry ->
                            scope.launch {
                                withContext(Dispatchers.Default) { packManager.uninstall(entry.id) }
                                rows = rows.toMutableList().also { it[index] = it[index].copy(installed = false, progress = 0.0) }
                            }
                        },
                    )
                }

                HorizontalDivider()
                Text("Side-load", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = {
                        if (sideLoadPicker == null) {
                            sideLoadStatus = "File picker unavailable on this device."
                            return@OutlinedButton
                        }
                        sideLoadStatus = null
                        sideLoadPicker.pickPackFile { bytes, name ->
                            scope.launch {
                                val result = withContext(Dispatchers.Default) { packManager.sideLoad(bytes, name) }
                                sideLoadStatus = when (result) {
                                    is PackInstallResult.Success -> "Imported \"${name}\" (${result.pack.itemCount} items)."
                                    is PackInstallResult.Failure -> "Side-load failed: ${result.message}"
                                }
                                // Refresh the installed view.
                                rows = rows.toMutableList().also { lst ->
                                    val id = (result as? PackInstallResult.Success)?.pack?.id
                                    if (id != null && lst.none { it.entry.id == id }) {
                                        // Side-loaded pack isn't in the catalog; surface it as installed.
                                        lst.add(PackRowState(
                                            entry = CatalogEntry(id, name, "1.0.0", "", 0, 0L, "", "", "", ""),
                                            installed = true,
                                        ))
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Import pack file…") }
                sideLoadStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun PackRow(
    state: PackRowState,
    onInstall: (CatalogEntry) -> Unit,
    onUninstall: (CatalogEntry) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.entry.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${state.entry.itemCount} items · ${formatBytes(state.entry.byteSize)} · ${state.entry.license}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.entry.attribution.isNotBlank()) {
                Text(state.entry.attribution, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (state.installing) {
                LinearProgressIndicator(
                    progress = { state.progress.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.installed && !state.installing) {
                    Button(onClick = { onInstall(state.entry) }) { Text("Install") }
                }
                if (state.installed && !state.installing) {
                    OutlinedButton(onClick = { onInstall(state.entry) }) { Text("Update") }
                    OutlinedButton(onClick = { onUninstall(state.entry) }) { Text("Uninstall") }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
