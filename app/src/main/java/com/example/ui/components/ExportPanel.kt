package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExportFormat
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KineticLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExportPanel(
    selectedFormat: ExportFormat,
    exportedPreviewText: String,
    onSelectFormat: (ExportFormat) -> Unit,
    onSaveToDevice: (ExportFormat, String) -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onShareScript: (ExportFormat, String) -> Unit,
    onImportSrtFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val codeVerticalScroll = rememberScrollState()
    val codeHorizontalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .padding(vertical = 8.dp)
            .testTag("export_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Format Selector Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ExportFormat.entries) { format ->
                val isSelected = format == selectedFormat
                Surface(
                    color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .width(164.dp)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else StudioCardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectFormat(format) }
                        .testTag("export_format_${format.extension}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = format.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ElectricCyan else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = format.subtitle,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Live Generated File Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .padding(horizontal = 12.dp)
                .background(ObsidianBg, RoundedCornerShape(10.dp))
                .border(1.dp, StudioCardBorder, RoundedCornerShape(10.dp))
                .padding(10.dp)
                .verticalScroll(codeVerticalScroll)
                .horizontalScroll(codeHorizontalScroll)
                .testTag("export_code_preview")
        ) {
            Text(
                text = exportedPreviewText.ifBlank { "# Chưa có câu phụ đề nào trong timeline hiện tại" },
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = KineticLime
            )
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onSaveToDevice(selectedFormat, exportedPreviewText) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("export_save_file_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Lưu .${selectedFormat.extension.uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { onCopyToClipboard(exportedPreviewText) },
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(StudioCardBorder)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.testTag("export_copy_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Sao chép phụ đề",
                    tint = TextPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Sao chép", fontSize = 12.sp, color = TextPrimary)
            }

            OutlinedButton(
                onClick = { onShareScript(selectedFormat, exportedPreviewText) },
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(StudioCardBorder)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.testTag("export_share_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Chia sẻ phụ đề",
                    tint = TextPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Chia sẻ", fontSize = 12.sp, color = TextPrimary)
            }

            OutlinedButton(
                onClick = onImportSrtFile,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(ElectricCyan.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.testTag("export_import_srt_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "Import SRT or VTT",
                    tint = ElectricCyan,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
