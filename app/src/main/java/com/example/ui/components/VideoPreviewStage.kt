package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CanvasBackdrop
import com.example.data.CaptionSegmentEntity
import com.example.data.SubtitleProjectEntity
import com.example.data.WordAnimationMode
import com.example.data.formatTimecodeShort
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KineticLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.parseHexColor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoPreviewStage(
    project: SubtitleProjectEntity,
    activeSegment: CaptionSegmentEntity?,
    playheadMs: Long,
    isPlaying: Boolean,
    onUpdateVerticalPosition: (Float) -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleSafeZones: () -> Unit,
    onToggleBilingual: () -> Unit,
    onPickVideo: () -> Unit,
    onCycleBackdrop: () -> Unit,
    onSegmentTap: (CaptionSegmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingCaption by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stage Top Control Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StagePillButton(
                    icon = Icons.Default.AspectRatio,
                    label = project.aspectRatioMode.shortLabel,
                    isActive = true,
                    onClick = onCycleAspectRatio,
                    testTag = "stage_aspect_ratio_btn"
                )
                StagePillButton(
                    icon = Icons.Default.GridOn,
                    label = "Vùng an toàn",
                    isActive = project.showSafeZones,
                    onClick = onToggleSafeZones,
                    testTag = "stage_safe_zone_btn"
                )
                StagePillButton(
                    icon = Icons.Default.Translate,
                    label = "Song ngữ",
                    isActive = project.showBilingual,
                    onClick = onToggleBilingual,
                    testTag = "stage_bilingual_btn"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StagePillButton(
                    icon = Icons.Default.Movie,
                    label = project.backdropMode.label,
                    isActive = project.videoUri == null,
                    onClick = onCycleBackdrop,
                    testTag = "stage_backdrop_btn"
                )
                StagePillButton(
                    icon = Icons.Default.VideoFile,
                    label = if (project.videoUri != null) "Đã gắn Video" else "Tải Video",
                    isActive = project.videoUri != null,
                    onClick = onPickVideo,
                    testTag = "stage_load_video_btn"
                )
            }
        }

        // Responsive Stage Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val targetRatio = project.aspectRatioMode.ratioValue
            val stageHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(targetRatio, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (isDraggingCaption) ElectricCyan else StudioCardBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("video_preview_canvas")
            ) {
                // 1. Video or Procedural Studio Canvas Backdrop
                if (project.videoUri != null) {
                    LocalVideoStagePlayer(
                        videoUriString = project.videoUri,
                        playheadMs = playheadMs,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ProceduralStudioBackdrop(
                        backdrop = project.backdropMode,
                        playheadMs = playheadMs,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Social Media Safe Zone Guides (TikTok / Reels / Shorts overlay)
                if (project.showSafeZones) {
                    SafeZoneOverlayGuides(
                        isDraggingCaption = isDraggingCaption,
                        verticalPositionRatio = project.verticalPositionRatio,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Top-Left Live Timecode & CPS HUD Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(
                            color = ObsidianBg.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, StudioCardBorder.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) HotCoral else ElectricCyan)
                    )
                    Text(
                        text = formatTimecodeShort(playheadMs),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (activeSegment != null) {
                        val cps = activeSegment.charactersPerSecond
                        val cpsColor = when {
                            cps > 20f -> HotCoral
                            cps > 15f -> AmberWarning
                            else -> KineticLime
                        }
                        Text(
                            text = "${cps}CPS",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = cpsColor
                        )
                    }
                }

                // 4. Draggable Live Subtitle Overlay
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val yOffsetPx = ((project.verticalPositionRatio - 0.5f) * containerHeightPx).roundToInt()

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(0, yOffsetPx) }
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .pointerInput(project.id) {
                                detectVerticalDragGestures(
                                    onDragStart = { isDraggingCaption = true },
                                    onDragEnd = { isDraggingCaption = false },
                                    onDragCancel = { isDraggingCaption = false },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaRatio = dragAmount / stageHeightPx
                                        val rawRatio = (project.verticalPositionRatio + deltaRatio)
                                            .coerceIn(0.15f, 0.86f)
                                        // Snap softly to 20%, 50%, or 76% lower-third
                                        val snapped = when {
                                            abs(rawRatio - 0.50f) < 0.02f -> 0.50f
                                            abs(rawRatio - 0.76f) < 0.02f -> 0.76f
                                            abs(rawRatio - 0.22f) < 0.02f -> 0.22f
                                            else -> rawRatio
                                        }
                                        onUpdateVerticalPosition(snapped)
                                    }
                                )
                            }
                            .testTag("live_subtitle_overlay"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeSegment != null) {
                            CapCutStyledCaptionOverlay(
                                project = project,
                                segment = activeSegment,
                                playheadMs = playheadMs,
                                isDragging = isDraggingCaption,
                                onClick = { onSegmentTap(activeSegment) }
                            )
                        } else {
                            // Subtle ghost indicator when playhead is in a silent gap
                            Surface(
                                color = ObsidianBg.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    StudioCardBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Chưa có phụ đề tại ${formatTimecodeShort(playheadMs)} • Kéo để chỉnh vị trí",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapCutStyledCaptionOverlay(
    project: SubtitleProjectEntity,
    segment: CaptionSegmentEntity,
    playheadMs: Long,
    isDragging: Boolean,
    onClick: () -> Unit
) {
    val words = remember(segment.wordsSerialized, segment.text, segment.startMs, segment.endMs) {
        segment.parsedWords()
    }
    val fontFamily = project.subtitleFont.toFontFamily()
    val baseColor = parseHexColor(project.textColorHex, Color.White)
    val activeColor = parseHexColor(project.activeWordColorHex, KineticLime)
    val strokeColor = parseHexColor(project.strokeColorHex, Color.Black)
    val bgColor = parseHexColor(project.bgColorHex, ObsidianBg).copy(alpha = project.bgOpacity.coerceIn(0f, 1f))
    val animMode = project.wordAnimation

    // Entry bounce calculation for BOUNCE_IN
    val elapsedInSegment = (playheadMs - segment.startMs).coerceAtLeast(0L)
    val entryScale by animateFloatAsState(
        targetValue = if (animMode == WordAnimationMode.BOUNCE_IN && elapsedInSegment < 220L) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "entry_bounce"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(entryScale)
            .clip(RoundedCornerShape(project.bgCornerRadiusDp.dp))
            .background(bgColor)
            .then(
                if (isDragging) {
                    Modifier.border(
                        1.5.dp,
                        ElectricCyan,
                        RoundedCornerShape(project.bgCornerRadiusDp.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center
        ) {
            words.forEachIndexed { index, wordTiming ->
                val isSpokenNow = playheadMs in wordTiming.startMs..wordTiming.endMs
                val isAlreadySpoken = playheadMs >= wordTiming.startMs

                // For TYPEWRITER mode, hide words that haven't started yet
                if (animMode == WordAnimationMode.TYPEWRITER && !isAlreadySpoken && index > 0) {
                    return@forEachIndexed
                }

                val wordScale by animateFloatAsState(
                    targetValue = when {
                        animMode == WordAnimationMode.KARAOKE_POP && isSpokenNow -> 1.18f
                        wordTiming.isEmphasized && isSpokenNow -> 1.15f
                        wordTiming.isEmphasized -> 1.06f
                        else -> 1.0f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "word_scale_$index"
                )

                val displayWord = if (project.isUppercase) {
                    wordTiming.word.uppercase()
                } else {
                    wordTiming.word
                }

                val wordTextColor = when {
                    animMode == WordAnimationMode.KARAOKE_BOX && isSpokenNow -> ObsidianBg
                    isSpokenNow && animMode != WordAnimationMode.STATIC_CLEAN -> activeColor
                    wordTiming.isEmphasized -> activeColor
                    else -> baseColor
                }

                val pillModifier = if (animMode == WordAnimationMode.KARAOKE_BOX && isSpokenNow) {
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(activeColor)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                } else {
                    Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                }

                Box(
                    modifier = Modifier
                        .scale(wordScale)
                        .then(pillModifier),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulated Stroke / Outline shadow layer for high-contrast CapCut look
                    if (project.strokeWidthDp > 0.5f && !(animMode == WordAnimationMode.KARAOKE_BOX && isSpokenNow)) {
                        Text(
                            text = displayWord,
                            fontFamily = fontFamily,
                            fontSize = project.fontSizeSp.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = strokeColor,
                            style = TextStyle(
                                drawStyle = Stroke(width = project.strokeWidthDp * 2.2f),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    offset = Offset(0f, 3f),
                                    blurRadius = project.shadowBlur.coerceAtLeast(1f)
                                )
                            )
                        )
                    }

                    // Foreground Fill Layer
                    val glowColor = if (animMode == WordAnimationMode.NEON_PULSE && isSpokenNow) {
                        activeColor
                    } else {
                        Color.Black.copy(alpha = 0.65f)
                    }
                    val glowBlur = if (animMode == WordAnimationMode.NEON_PULSE && isSpokenNow) {
                        (project.shadowBlur * 2.2f).coerceAtLeast(14f)
                    } else {
                        project.shadowBlur
                    }

                    Text(
                        text = displayWord,
                        fontFamily = fontFamily,
                        fontSize = project.fontSizeSp.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = wordTextColor,
                        style = TextStyle(
                            shadow = if (glowBlur > 0.5f) {
                                Shadow(
                                    color = glowColor,
                                    offset = Offset(0f, 2f),
                                    blurRadius = glowBlur
                                )
                            } else null
                        )
                    )
                }
            }
        }

        // Secondary Bilingual / Translation Caption Line
        if (project.showBilingual && segment.secondaryText.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = segment.secondaryText,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = (project.fontSizeSp * 0.62f).coerceIn(11f, 18f).sp,
                fontWeight = FontWeight.SemiBold,
                color = baseColor.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.9f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

@Composable
private fun ProceduralStudioBackdrop(
    backdrop: CanvasBackdrop,
    playheadMs: Long,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = playheadMs / 1000f

        when (backdrop) {
            CanvasBackdrop.CHROMA_GREEN -> {
                drawRect(color = Color(0xFF00FF00))
            }

            CanvasBackdrop.CYBER_GRID -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090D16),
                            Color(0xFF121C2E),
                            Color(0xFF0B0F1A)
                        )
                    )
                )
                // Animated horizon grid lines
                val gridColor = ElectricCyan.copy(alpha = 0.14f)
                val stepX = w / 8f
                for (i in 1..7) {
                    drawLine(
                        color = gridColor,
                        start = Offset(i * stepX, 0f),
                        end = Offset(i * stepX, h),
                        strokeWidth = 1f
                    )
                }
                val stepY = h / 10f
                val shiftY = (t * 24f) % stepY
                for (j in 0..10) {
                    val y = (j * stepY + shiftY).coerceIn(0f, h)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }
                // Ambient center audio pulse ring
                val pulseRadius = (w.coerceAtMost(h) * 0.24f) * (1f + 0.08f * sin(t * 4f))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricCyan.copy(alpha = 0.22f),
                            KineticLime.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.42f),
                        radius = pulseRadius * 1.8f
                    ),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = pulseRadius * 1.8f
                )
            }

            CanvasBackdrop.CINEMA_NOIR -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF232938),
                            Color(0xFF10131B),
                            Color(0xFF07080C)
                        ),
                        center = Offset(w * 0.5f, h * 0.38f),
                        radius = w.coerceAtLeast(h) * 0.75f
                    )
                )
                // Anamorphic lens flare streak
                val flareY = h * 0.36f + sin(t * 1.5f) * 12f
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ElectricCyan.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.45f),
                            ElectricCyan.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.08f, flareY),
                    end = Offset(w * 0.92f, flareY),
                    strokeWidth = 3f
                )
            }

            CanvasBackdrop.SUNSET_VLOG -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2B102F),
                            Color(0xFF591B3C),
                            Color(0xFF18122B)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                val orbX = w * (0.5f + 0.15f * cos(t * 1.2f))
                val orbY = h * (0.35f + 0.1f * sin(t * 1.6f))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HotCoral.copy(alpha = 0.35f),
                            AmberWarning.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(orbX, orbY),
                        radius = w * 0.55f
                    ),
                    center = Offset(orbX, orbY),
                    radius = w * 0.55f
                )
            }

            CanvasBackdrop.NEON_BOKEH -> {
                drawRect(color = Color(0xFF0D1017))
                val bokehSeeds = listOf(
                    Triple(0.25f, 0.30f, ElectricCyan),
                    Triple(0.72f, 0.25f, KineticLime),
                    Triple(0.40f, 0.62f, HotCoral),
                    Triple(0.78f, 0.68f, ElectricCyan)
                )
                bokehSeeds.forEachIndexed { i, (bx, by, col) ->
                    val cx = w * (bx + 0.05f * sin(t + i))
                    val cy = h * (by + 0.05f * cos(t * 0.8f + i))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(col.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = w * 0.32f
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.32f
                    )
                }
            }
        }
    }
}

@Composable
private fun SafeZoneOverlayGuides(
    isDraggingCaption: Boolean,
    verticalPositionRatio: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        // Safe frame inner margin
        val marginH = w * 0.07f
        val marginTop = h * 0.10f
        val marginBottom = h * 0.14f

        drawRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(marginH, marginTop),
            size = androidx.compose.ui.geometry.Size(
                width = w - marginH * 2,
                height = h - marginTop - marginBottom
            ),
            style = Stroke(width = 1.2f, pathEffect = dashEffect)
        )

        // Right-side social icons danger zone (TikTok / Reels buttons)
        val rightZoneW = w * 0.14f
        val rightZoneH = h * 0.36f
        drawRoundRect(
            color = HotCoral.copy(alpha = 0.14f),
            topLeft = Offset(w - rightZoneW - 6f, h * 0.42f),
            size = androidx.compose.ui.geometry.Size(rightZoneW, rightZoneH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )

        // Active drag alignment crosshair
        if (isDraggingCaption) {
            val activeY = h * verticalPositionRatio
            drawLine(
                color = ElectricCyan,
                start = Offset(0f, activeY),
                end = Offset(w, activeY),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )
            drawLine(
                color = ElectricCyan.copy(alpha = 0.45f),
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = 1.2f,
                pathEffect = dashEffect
            )
        }
    }
}

@Composable
private fun LocalVideoStagePlayer(
    videoUriString: String,
    playheadMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var lastLoadedUri by remember { mutableStateOf<String?>(null) }

    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                }
            }
        },
        update = { videoView ->
            if (lastLoadedUri != videoUriString) {
                lastLoadedUri = videoUriString
                runCatching {
                    videoView.setVideoURI(Uri.parse(videoUriString))
                    videoView.seekTo(playheadMs.toInt())
                }
            }
            runCatching {
                if (isPlaying && !videoView.isPlaying) {
                    videoView.start()
                } else if (!isPlaying && videoView.isPlaying) {
                    videoView.pause()
                }
                val diff = abs(videoView.currentPosition - playheadMs.toInt())
                if (diff > 350) {
                    videoView.seekTo(playheadMs.toInt())
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun StagePillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isActive) StudioSurfaceElevated else StudioSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (isActive) ElectricCyan.copy(alpha = 0.7f) else StudioCardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) ElectricCyan else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) TextPrimary else TextSecondary
            )
        }
    }
}
