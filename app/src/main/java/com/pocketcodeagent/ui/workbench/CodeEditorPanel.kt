package com.pocketcodeagent.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

private val EditorBg = Color(0xFF0D0D0F)
private val LineNrBg = Color(0xFF111115)
private val LineNrColor = Color(0xFF444450)

@Composable
fun CodeEditorPanel(
    viewModel: WorkspaceViewModel,
    fileUri: String?,
    fileName: String?,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(fileUri) {
        fileUri?.let { viewModel.loadFileContent(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBg)
    ) {
        // File name bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13131A))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Code, null, tint = SlateBlue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = fileName ?: "Keine Datei ausgewählt",
                color = if (fileName != null) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            if (viewModel.isOpenFileLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = CalmSage,
                    strokeWidth = 2.dp
                )
            }
            if (fileUri != null && !viewModel.isOpenFileLoading) {
                IconButton(
                    onClick = { viewModel.saveFileContent(fileUri, viewModel.openFileContent) },
                    modifier = Modifier.size(32.dp),
                    enabled = !viewModel.isOpenFileSaving
                ) {
                    Icon(
                        Icons.Default.Save,
                        null,
                        tint = if (!viewModel.isOpenFileSaving) CalmSage else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

        if (fileUri == null) {
            PanelPlaceholder(
                icon = Icons.Default.Code,
                title = "Keine Datei offen",
                subtitle = "Klicke im Files-Tab auf eine Datei."
            )
            return@Column
        }

        if (viewModel.isOpenFileLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CalmSage)
            }
            return@Column
        }

        // Code view with line numbers
        val lines = viewModel.openFileContent.lines()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Line numbers gutter
            Column(
                modifier = Modifier
                    .background(LineNrBg)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                lines.forEachIndexed { i, _ ->
                    Text(
                        text = "${i + 1}",
                        color = LineNrColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            // Code content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                lines.forEach { line ->
                    Text(
                        text = line.ifEmpty { " " },
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
