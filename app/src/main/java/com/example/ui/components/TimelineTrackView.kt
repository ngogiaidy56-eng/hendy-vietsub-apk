package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaptionSegmentEntity
import com.example.data.formatTimecodeShort
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDim
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KineticLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TimelineTrackBg
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TimelineTrackView(
    segments: List<CaptionSegmentEntity>,
    selectedSegment: CaptionSegmentEntity?,
    playheadMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    timelineZoom: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomDelta: (Float) -> Unit,
    onSelectSegment: (CaptionSegmentEntity) -> Unit,
    onAddCaption: () -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onMergeWithNext: () -> Unit,
    onDuplicateSegment: () -> Unit,
    onDeleteSegment: () -> Unit,
    onNudgeTiming: (CaptionSegmentEntity, Long, Long) -> Unit,
    onToggleWordEmphasis: (CaptionSegmentEntity, Int) -> Unit,
    onOpenEditModal: (CaptionSegmentEntity) -> Unit,
    onOpenVoiceModal: () -> Unit,
    onOpenSmartScriptModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val dpPerSecond = (76f * timelineZoom).coerceIn(44f, 200f)
    val pxPerSecond = with(density) { dpPerSecond.dp.toPx() }
    val safeTotalMs = max(totalDurationMs, (segments.maxOfOrNull { it.endMs } ?: 12000L) + 1500L)
    val timelineWidthDp = ((safeTotalMs / 1000f) * dpPerSecond + 120f).dp

    // Auto-scroll timeline during playback so playhead stays visible
    LaunchedEffect(playheadMs, isPlaying) {
        if (isPlaying) {
            val targetPx = ((playheadMs / 1000f) * pxPerSecond).roundToInt()
            val viewport = scrollState.viewportSize
            if (viewport > 0 && (targetPx < scrollState.value || targetPx > scrollState.value + viewport - 120)) {
                scrollState.scrollTo((targetPx - viewport / 3).coerceAtLeast(0))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
    ) {
        // 1. Transport & Precision Timecode Deck
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurfaceElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onSeekTo(0L) },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("transport_rewind_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Jump to start",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    color = ElectricCyan,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("transport_play_pause_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = ObsidianBg,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTimecodeShort(playheadMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = " / ${formatTimecodeShort(safeTotalMs)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${segments.size} câu • Chạm/kéo thước để tua",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Playback speed pill
                Surface(
                    color = StudioSurface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .border(1.dp, StudioCardBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onCycleSpeed)
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("transport_speed_btn")
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KineticLime
                    )
                }

                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("timeline_undo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) TextPrimary else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("timeline_redo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) TextPrimary else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onZoomDelta(-0.25f) },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("timeline_zoom_out_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom out timeline",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onZoomDelta(0.25f) },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("timeline_zoom_in_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom in timeline",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Multi-Track Scrollable Timeline Canvas (Ruler + Audio Waveform + Subtitle Blocks)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .background(TimelineTrackBg)
                .horizontalScroll(scrollState)
                .testTag("timeline_scroll_container")
        ) {
            Box(
                modifier = Modifier
                    .width(timelineWidthDp)
                    .fillMaxHeight()
                    .pointerInput(pxPerSecond, safeTotalMs) {
                        detectTapGestures { offset ->
                            val tappedMs = ((offset.x / pxPerSecond) * 1000f).toLong().coerceIn(0L, safeTotalMs)
                            onSeekTo(tappedMs)
                        }
                    }
                    .pointerInput(pxPerSecond, safeTotalMs) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val draggedMs = ((change.position.x / pxPerSecond) * 1000f).toLong().coerceIn(0L, safeTotalMs)
                            onSeekTo(draggedMs)
                        }
                    }
            ) {
                // Time Ruler + Audio Waveform Track Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val widthPx = size.width
                    val rulerHeight = 24.dp.toPx()
                    val waveformTop = 26.dp.toPx()
                    val waveformHeight = 32.dp.toPx()
                    val totalSecs = (safeTotalMs / 1000L).toInt() + 1

                    // Ruler background strip
                    drawRect(
                        color = Color(0xFF141923),
                        topLeft = Offset(0f, 0f),
                        size = Size(widthPx, rulerHeight)
                    )

                    // Second & sub-second ticks
                    for (sec in 0..totalSecs) {
                        val x = sec * pxPerSecond
                        drawLine(
                            color = Color(0xFF495368),
                            start = Offset(x, rulerHeight - 9.dp.toPx()),
                            end = Offset(x, rulerHeight),
                            strokeWidth = 1.5f
                        )
                        // Half-second tick
                        val halfX = x + pxPerSecond * 0.5f
                        drawLine(
                            color = Color(0xFF2D3546),
                            start = Offset(halfX, rulerHeight - 5.dp.toPx()),
                            end = Offset(halfX, rulerHeight),
                            strokeWidth = 1f
                        )
                    }

                    // Audio Waveform Track background
                    drawRoundRect(
                        color = Color(0xFF121722),
                        topLeft = Offset(0f, waveformTop),
                        size = Size(widthPx, waveformHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    // Synthetic Vocal Audio Waveform Bars
                    val barStep = 6.dp.toPx()
                    val barCount = (widthPx / barStep).toInt()
                    val playheadX = (playheadMs / 1000f) * pxPerSecond
                    val centerWaveY = waveformTop + waveformHeight / 2f

                    for (i in 0 until barCount) {
                        val barX = i * barStep
                        val barMs = ((barX / pxPerSecond) * 1000f).toLong()
                        val isInsideSpeech = segments.any { barMs in it.startMs..it.endMs }
                        val rawAmp = if (isInsideSpeech) {
                            (0.35f + 0.6f * kotlin.math.abs(sin(i * 0.65f) * sin(i * 0.23f)))
                        } else {
                            0.12f
                        }
                        val barHalfH = (waveformHeight * 0.44f * rawAmp).coerceAtLeast(2f)
                        val barColor = when {
                            barX <= playheadX && isInsideSpeech -> ElectricCyan.copy(alpha = 0.85f)
                            isInsideSpeech -> ElectricCyanDim.copy(alpha = 0.55f)
                            else -> Color(0xFF293040)
                        }
                        drawLine(
                            color = barColor,
                            start = Offset(barX, centerWaveY - barHalfH),
                            end = Offset(barX, centerWaveY + barHalfH),
                            strokeWidth = 3f
                        )
                    }
                }

                // Second Labels on Ruler
                val totalSeconds = (safeTotalMs / 1000L).toInt() + 1
                for (sec in 0..totalSeconds) {
                    val xDp = (sec * dpPerSecond).dp
                    Text(
                        text = "%02d:%02d".format(sec / 60, sec % 60),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .offset(x = xDp + 3.dp, y = 4.dp)
                    )
                }

                // Subtitle Segment Blocks on Track (y = 64.dp)
                segments.forEach { segment ->
                    val startDp = ((segment.startMs / 1000f) * dpPerSecond).dp
                    val widthDp = (((segment.durationMs) / 1000f) * dpPerSecond).coerceAtLeast(36f).dp
                    val isSelected = selectedSegment?.id == segment.id
                    val isUnderPlayhead = playheadMs in segment.startMs..segment.endMs
                    val cps = segment.charactersPerSecond
                    val borderColor = when {
                        isSelected -> KineticLime
                        isUnderPlayhead -> ElectricCyan
                        cps > 20f -> HotCoral.copy(alpha = 0.8f)
                        else -> Color(0xFF334155)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = startDp, y = 64.dp)
                            .width(widthDp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF232D3F)
                                else if (isUnderPlayhead) Color(0xFF1C2738)
                                else Color(0xFF171D2B)
                            )
                            .border(
                                width = if (isSelected || isUnderPlayhead) 1.8.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectSegment(segment) }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .testTag("timeline_segment_${segment.id}")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = segment.text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) KineticLime else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${formatTimecodeShort(segment.startMs)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${cps}c/s",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = if (cps > 20f) HotCoral else ElectricCyan
                                )
                            }
                        }
                    }
                }

                // Playhead Vertical Needle + Top Diamond Handle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val playheadX = (playheadMs / 1000f) * pxPerSecond
                    val handlePath = Path().apply {
                        moveTo(playheadX - 7.dp.toPx(), 0f)
                        lineTo(playheadX + 7.dp.toPx(), 0f)
                        lineTo(playheadX + 7.dp.toPx(), 9.dp.toPx())
                        lineTo(playheadX, 16.dp.toPx())
                        lineTo(playheadX - 7.dp.toPx(), 9.dp.toPx())
                        close()
                    }
                    drawPath(path = handlePath, color = ElectricCyan)
                    drawLine(
                        color = ElectricCyan,
                        start = Offset(playheadX, 14.dp.toPx()),
                        end = Offset(playheadX, size.height),
                        strokeWidth = 2.2.dp.toPx()
                    )
                }
            }
        }

        // 3. Active Segment Word-Chip Karaoke Bar (Tap word to toggle Emphasis ★)
        val activeCue = selectedSegment ?: segments.firstOrNull { playheadMs in it.startMs..it.endMs }
        if (activeCue != null) {
            val words = remember(activeCue.wordsSerialized, activeCue.text, activeCue.startMs, activeCue.endMs) {
                activeCue.parsedWords()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable { onOpenEditModal(activeCue) }
                        .padding(end = 6.dp)
                        .testTag("quick_edit_active_cue_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Sửa câu phụ đề",
                            tint = ElectricCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Sửa câu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(words) { idx, wordTiming ->
                        val isSpoken = playheadMs in wordTiming.startMs..wordTiming.endMs
                        Surface(
                            color = when {
                                wordTiming.isEmphasized -> KineticLime.copy(alpha = 0.2f)
                                isSpoken -> ElectricCyan.copy(alpha = 0.2f)
                                else -> StudioSurface
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        wordTiming.isEmphasized -> KineticLime
                                        isSpoken -> ElectricCyan
                                        else -> StudioCardBorder
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onToggleWordEmphasis(activeCue, idx) }
                                .testTag("word_chip_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (wordTiming.isEmphasized) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Emphasized word",
                                        tint = KineticLime,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = wordTiming.word,
                                    fontSize = 11.sp,
                                    fontWeight = if (wordTiming.isEmphasized || isSpoken) FontWeight.Bold else FontWeight.Medium,
                                    color = if (wordTiming.isEmphasized) KineticLime else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Quick Nudge -100ms / +100ms buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = StudioSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .border(1.dp, StudioCardBorder, RoundedCornerShape(6.dp))
                            .clickable { onNudgeTiming(activeCue, -100L, -100L) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("nudge_minus_btn")
                    ) {
                        Text(
                            text = "-0.1s",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        color = StudioSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .border(1.dp, StudioCardBorder, RoundedCornerShape(6.dp))
                            .clickable { onNudgeTiming(activeCue, 100L, 100L) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("nudge_plus_btn")
                    ) {
                        Text(
                            text = "+0.1s",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 4. CapCut Quick Action Dock
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                DockActionChip(
                    icon = Icons.Default.Add,
                    label = "Thêm câu",
                    accentColor = ElectricCyan,
                    onClick = onAddCaption,
                    testTag = "dock_add_cue_btn"
                )
            }
            item {
                DockActionChip(
                    icon = Icons.Default.ContentCut,
                    label = "Tách câu",
                    accentColor = KineticLime,
                    onClick = onSplitAtPlayhead,
                    testTag = "dock_split_btn"
                )
            }
            item {
                DockActionChip(
                    icon = Icons.AutoMirrored.Filled.MergeType,
                    label = "Gộp câu kế",
                    accentColor = TextPrimary,
                    onClick = onMergeWithNext,
                    testTag = "dock_merge_btn"
                )
            }
            item {
                DockActionChip(
                    icon = Icons.Default.Mic,
                    label = "Giọng nói",
                    accentColor = HotCoral,
                    onClick = onOpenVoiceModal,
                    testTag = "dock_voice_dictate_btn"
                )
            }
            item {
                DockActionChip(
                    icon = Icons.Default.AutoAwesome,
                    label = "Chia kịch bản",
                    accentColor = ElectricCyan,
                    onClick = onOpenSmartScriptModal,
                    testTag = "dock_smart_script_btn"
                )
            }
            item {
                DockActionChip(
                    icon = Icons.Default.ContentCopy,
                    label = "Nhân bản",
                    accentColor = TextPrimary,
                    onClick = onDuplicateSegment,
                    testTag = "dock_duplicate_btn"
                )
            }
            item {
                DockActionChip(
                    icon = Icons.Default.DeleteOutline,
                    label = "Xóa",
                    accentColor = HotCoral,
                    onClick = onDeleteSegment,
                    testTag = "dock_delete_btn"
                )
            }
        }
    }
}

@Composable
private fun DockActionChip(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .border(1.dp, StudioCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}
