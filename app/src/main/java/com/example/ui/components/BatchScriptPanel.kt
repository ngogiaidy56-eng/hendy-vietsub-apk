package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaptionSegmentEntity
import com.example.data.formatTimecodeShort
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KineticLime
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BatchScriptPanel(
    segments: List<CaptionSegmentEntity>,
    selectedSegmentId: Long?,
    playheadMs: Long,
    onSelectAndSeek: (CaptionSegmentEntity) -> Unit,
    onEditSegment: (CaptionSegmentEntity) -> Unit,
    onShiftAllSegments: (Long) -> Unit,
    onAddCue: () -> Unit,
    onOpenSmartScript: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fastCount = segments.count { it.charactersPerSecond > 20f }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .testTag("batch_script_panel")
    ) {
        // Top Batch Sync & CPS Readability Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Danh Sách Phụ Đề & Kiểm Tra Tốc Độ Đọc (CPS)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (fastCount == 0) {
                        "Tất cả ${segments.size} câu đều đạt tốc độ đọc tối ưu (≤20 CPS)"
                    } else {
                        "Có $fastCount câu vượt 20 CPS • Nên tách nhỏ câu dài"
                    },
                    fontSize = 11.sp,
                    color = if (fastCount == 0) EmeraldReady else AmberWarning
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BatchHeaderChip(
                    label = "-250ms Tất cả",
                    onClick = { onShiftAllSegments(-250L) },
                    testTag = "batch_shift_minus_btn"
                )
                BatchHeaderChip(
                    label = "+250ms Tất cả",
                    onClick = { onShiftAllSegments(250L) },
                    testTag = "batch_shift_plus_btn"
                )
                BatchHeaderChip(
                    label = "Tự căn giờ",
                    accentColor = ElectricCyan,
                    onClick = onOpenSmartScript,
                    testTag = "batch_smart_script_btn"
                )
            }
        }

        if (segments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chưa có câu phụ đề nào",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BatchHeaderChip(
                            label = "+ Thêm câu",
                            accentColor = ElectricCyan,
                            onClick = onAddCue,
                            testTag = "empty_add_cue_btn"
                        )
                        BatchHeaderChip(
                            label = "Dán kịch bản",
                            accentColor = KineticLime,
                            onClick = onOpenSmartScript,
                            testTag = "empty_paste_script_btn"
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(segments, key = { _, seg -> seg.id }) { index, seg ->
                    val isSelected = seg.id == selectedSegmentId
                    val isUnderPlayhead = playheadMs in seg.startMs..seg.endMs
                    val cps = seg.charactersPerSecond
                    val cpsBadgeColor = when {
                        cps > 20f -> HotCoral
                        cps > 15f -> AmberWarning
                        else -> EmeraldReady
                    }

                    Surface(
                        color = if (isSelected || isUnderPlayhead) StudioSurfaceElevated else StudioSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = when {
                                    isSelected -> KineticLime
                                    isUnderPlayhead -> ElectricCyan
                                    else -> StudioCardBorder
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectAndSeek(seg) }
                            .testTag("batch_row_${index + 1}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Index number badge
                            Surface(
                                color = if (isUnderPlayhead) ElectricCyan else StudioSurface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnderPlayhead) StudioSurface else TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${formatTimecodeShort(seg.startMs)} → ${formatTimecodeShort(seg.endMs)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Surface(
                                        color = cpsBadgeColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${cps} CPS",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cpsBadgeColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = seg.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (seg.secondaryText.isNotBlank()) {
                                    Text(
                                        text = seg.secondaryText,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onEditSegment(seg) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("batch_edit_btn_${index + 1}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit cue ${index + 1}",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchHeaderChip(
    label: String,
    accentColor: androidx.compose.ui.graphics.Color = TextSecondary,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier
            .border(1.dp, StudioCardBorder, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
