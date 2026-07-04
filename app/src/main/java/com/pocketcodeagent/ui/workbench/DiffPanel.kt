package com.pocketcodeagent.ui.workbench

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchStatus
import com.pocketcodeagent.domain.export.PatchMarkdownExporter
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

private val AddedBg    = Color(0xFF0D2216)
private val RemovedBg  = Color(0xFF240D0D)
private val AddedText  = Color(0xFF3FB950)
private val RemovedText = Color(0xFFF85149)

@Composable
fun DiffPanel(
    viewModel: WorkspaceViewModel,
    patches: List<FilePatch>,
    onApplyChange: (FilePatch) -> Unit,
    onRejectChange: (FilePatch) -> Unit,
    onApplyAll: () -> Unit,
    onRejectAll: () -> Unit,
    onUndoLastApply: () -> Unit,
    onConfirmDelete: (FilePatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExportDone by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13131A))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Compare, null, tint = WarmCopper, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "${patches.size} Änderung(en)",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))

            // Export Markdown button
            if (patches.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        val md = PatchMarkdownExporter.exportToMarkdown(patches)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Patches", md))
                        Toast.makeText(context, "Patches als Markdown kopiert", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !viewModel.isApplyingPatch,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export MD", fontSize = 11.sp)
                }
                Spacer(Modifier.width(6.dp))
            }

            if (viewModel.isApplyingPatch) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = SlateBlue,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            if (patches.isNotEmpty()) {
                OutlinedButton(
                    onClick = onUndoLastApply,
                    enabled = !viewModel.isApplyingPatch,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("Undo", fontSize = 11.sp)
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onRejectAll,
                    enabled = !viewModel.isApplyingPatch,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCopper),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("Reject all", fontSize = 11.sp)
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onApplyAll,
                    enabled = !viewModel.isApplyingPatch,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Apply safe", fontSize = 11.sp)
                }
            }
        }

        HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

        if (patches.isEmpty()) {
            PanelPlaceholder(
                icon = Icons.Default.CheckCircle,
                title = "Keine ausstehenden Änderungen",
                subtitle = "Der Agent hat noch keine Dateiänderungen vorgeschlagen."
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(patches, key = { _, p -> p.path }) { _, patch ->
                DiffCard(
                    patch = patch,
                    onApply = { onApplyChange(patch) },
                    onReject = { onRejectChange(patch) },
                    onConfirmDelete = { onConfirmDelete(patch) },
                    isApplying = viewModel.isApplyingPatch
                )
            }
        }
    }
}

@Composable
private fun DiffCard(
    patch: FilePatch,
    onApply: () -> Unit,
    onReject: () -> Unit,
    onConfirmDelete: () -> Unit,
    isApplying: Boolean
) {
    var showFull by remember(patch.id) { mutableStateOf(false) }
    val canApply = patch.status != FilePatchStatus.APPLIED &&
        patch.status != FilePatchStatus.REJECTED &&
        (patch.action != FilePatchAction.DELETE || patch.deleteConfirmed)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    patch.path,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgePill(patch.action.name, actionColor(patch.action))
                BadgePill(patch.status.name, statusColor(patch.status))
                val additions = patch.additions ?: patch.newText.lineCountOrZero()
                val deletions = patch.deletions ?: patch.oldText.lineCountOrZero()
                Text(
                    "+$additions / -$deletions",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            patch.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    error,
                    color = WarmCopper,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            val oldLines = patch.oldText.linesOrEmpty()
            val newLines = if (patch.action == FilePatchAction.DELETE) emptyList() else patch.newText.linesOrEmpty()
            val lineLimit = if (showFull) Int.MAX_VALUE else 20

            if (oldLines.isNotEmpty()) {
                oldLines.take(lineLimit).forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RemovedBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("\u2212", color = RemovedText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(line, color = RemovedText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
                if (!showFull && oldLines.size > 20) {
                    Text("${oldLines.size - 20} weitere entfernte Zeilen", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                }
            }

            if (newLines.isNotEmpty()) {
                newLines.take(lineLimit).forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AddedBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+", color = AddedText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(line, color = AddedText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
                if (!showFull && newLines.size > 20) {
                    Text("${newLines.size - 20} weitere neue Zeilen", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            if (patch.action == FilePatchAction.DELETE && !patch.deleteConfirmed) {
                OutlinedButton(
                    onClick = onConfirmDelete,
                    enabled = !isApplying,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCopper),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm delete first", fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isApplying && patch.status != FilePatchStatus.APPLIED,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCopper),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ablehnen", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { showFull = !showFull },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showFull) "Compact" else "View full", fontSize = 12.sp)
                }
                Button(
                    onClick = onApply,
                    enabled = !isApplying && canApply,
                    colors = ButtonDefaults.buttonColors(containerColor = CalmSage),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (patch.action == FilePatchAction.DELETE) "Apply delete" else "Apply",
                        fontSize = 12.sp,
                        color = Color(0xFF0E0E10),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(5.dp)
    ) {
        Text(
            label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun actionColor(action: FilePatchAction): Color = when (action) {
    FilePatchAction.CREATE -> AddedText
    FilePatchAction.MODIFY -> SlateBlue
    FilePatchAction.DELETE -> RemovedText
}

private fun statusColor(status: FilePatchStatus): Color = when (status) {
    FilePatchStatus.PENDING -> TextSecondary
    FilePatchStatus.APPLIED -> CalmSage
    FilePatchStatus.REJECTED -> Color(0xFF777783)
    FilePatchStatus.CONFLICT -> WarmCopper
    FilePatchStatus.FAILED -> RemovedText
}

private fun String?.linesOrEmpty(): List<String> {
    if (this.isNullOrEmpty()) return emptyList()
    return lines()
}

private fun String?.lineCountOrZero(): Int {
    return linesOrEmpty().size
}
