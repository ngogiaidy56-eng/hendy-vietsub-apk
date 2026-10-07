package com.example.data

import kotlin.math.max
import kotlin.math.roundToInt

enum class ScriptCadenceMode(val label: String, val maxWordsPerCue: Int, val wordsPerMinute: Int) {
    VIRAL_FAST("Nhịp nhanh Viral (2–3 từ/câu)", 3, 175),
    SOCIAL_BALANCED("Nhịp Mạng xã hội (5–6 từ/câu)", 6, 155),
    CINEMA_PHRASE("Nhịp Điện ảnh (8–10 từ/câu)", 9, 140)
}

object SubtitleParserExporter {

    private val timecodeRegex = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})"""
    )

    fun parseSrtOrVtt(
        rawContent: String,
        projectId: Long
    ): List<CaptionSegmentEntity> {
        val normalized = rawContent.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isBlank()) return emptyList()

        val blocks = normalized.split(Regex("\n\\s*\n"))
        val parsedSegments = mutableListOf<CaptionSegmentEntity>()

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            if (lines.first().startsWith("WEBVTT", ignoreCase = true)) continue

            val timeLineIndex = lines.indexOfFirst { timecodeRegex.containsMatchIn(it) }
            if (timeLineIndex == -1) continue

            val match = timecodeRegex.find(lines[timeLineIndex]) ?: continue
            val startMs = toMillis(
                match.groupValues[1],
                match.groupValues[2],
                match.groupValues[3],
                match.groupValues[4]
            )
            val endMs = max(
                startMs + 400L,
                toMillis(
                    match.groupValues[5],
                    match.groupValues[6],
                    match.groupValues[7],
                    match.groupValues[8]
                )
            )

            val textLines = lines.drop(timeLineIndex + 1)
                .map { it.replace(Regex("<[^>]*>"), "").trim() }
                .filter { it.isNotEmpty() }

            if (textLines.isEmpty()) continue

            val primaryText = textLines.first()
            val secondaryText = if (textLines.size > 1) textLines.drop(1).joinToString(" ") else ""
            val words = distributeWordsEvenly(primaryText, startMs, endMs)

            parsedSegments.add(
                CaptionSegmentEntity(
                    projectId = projectId,
                    startMs = startMs,
                    endMs = endMs,
                    text = primaryText,
                    secondaryText = secondaryText,
                    speakerTag = "Người nói 1",
                    wordsSerialized = words.serializeWords()
                )
            )
        }
        return parsedSegments.sortedBy { it.startMs }
    }

    fun autoSyncScript(
        rawScript: String,
        projectId: Long,
        startOffsetMs: Long = 0L,
        cadence: ScriptCadenceMode = ScriptCadenceMode.SOCIAL_BALANCED,
        autoHighlightKeywords: Boolean = true
    ): List<CaptionSegmentEntity> {
        val cleanText = rawScript.trim()
        if (cleanText.isEmpty()) return emptyList()

        val sentences = cleanText
            .split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val wordChunks = mutableListOf<List<String>>()
        for (sentence in sentences) {
            val words = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }
            var idx = 0
            while (idx < words.size) {
                val endIdx = (idx + cadence.maxWordsPerCue).coerceAtMost(words.size)
                wordChunks.add(words.subList(idx, endIdx))
                idx = endIdx
            }
        }

        val msPerWord = (60_000L / cadence.wordsPerMinute).coerceIn(240L, 650L)
        var cursorMs = max(0L, startOffsetMs)
        val result = mutableListOf<CaptionSegmentEntity>()

        for (chunk in wordChunks) {
            val text = chunk.joinToString(" ")
            val charBonusMs = (text.length * 18L)
            val duration = max(850L, (chunk.size * msPerWord + charBonusMs) / 2)
            val start = cursorMs
            val end = start + duration
            val distributed = distributeWordsEvenly(text, start, end).map { wt ->
                val isKeyword = autoHighlightKeywords && (
                    wt.word.any { it.isDigit() } ||
                        wt.word.endsWith("!") ||
                        wt.word.endsWith("?") ||
                        wt.word.length >= 6
                    )
                wt.copy(isEmphasized = isKeyword)
            }
            result.add(
                CaptionSegmentEntity(
                    projectId = projectId,
                    startMs = start,
                    endMs = end,
                    text = text,
                    secondaryText = "",
                    speakerTag = "Người nói 1",
                    wordsSerialized = distributed.serializeWords()
                )
            )
            cursorMs = end + 80L
        }

        return result
    }

    fun exportSubtitles(
        project: SubtitleProjectEntity,
        segments: List<CaptionSegmentEntity>,
        format: ExportFormat
    ): String {
        val sorted = segments.sortedBy { it.startMs }
        return when (format) {
            ExportFormat.SRT -> exportSrt(project, sorted)
            ExportFormat.VTT -> exportVtt(project, sorted)
            ExportFormat.ASS -> exportAss(project, sorted)
            ExportFormat.TXT -> exportTxt(project, sorted)
        }
    }

    private fun exportSrt(
        project: SubtitleProjectEntity,
        segments: List<CaptionSegmentEntity>
    ): String = buildString {
        segments.forEachIndexed { index, seg ->
            appendLine(index + 1)
            appendLine("${formatTimecodeSrt(seg.startMs)} --> ${formatTimecodeSrt(seg.endMs)}")
            val lineText = if (project.isUppercase) seg.text.uppercase() else seg.text
            appendLine(lineText)
            if (project.showBilingual && seg.secondaryText.isNotBlank()) {
                appendLine(seg.secondaryText)
            }
            appendLine()
        }
    }

    private fun exportVtt(
        project: SubtitleProjectEntity,
        segments: List<CaptionSegmentEntity>
    ): String = buildString {
        appendLine("WEBVTT")
        appendLine("Kind: captions")
        appendLine("Language: vi")
        appendLine("NOTE Tạo bởi Hendy Vietsub - ${project.title}")
        appendLine()

        val linePercent = (project.verticalPositionRatio * 100).roundToInt().coerceIn(10, 92)
        segments.forEachIndexed { index, seg ->
            appendLine("cue-${index + 1}")
            appendLine("${formatTimecodeVtt(seg.startMs)} --> ${formatTimecodeVtt(seg.endMs)} line:$linePercent% align:center")
            val words = seg.parsedWords()
            if (words.size > 1 && project.wordAnimation != WordAnimationMode.STATIC_CLEAN) {
                val karaokeLine = words.joinToString(" ") { w ->
                    val raw = if (project.isUppercase) w.word.uppercase() else w.word
                    val tag = "<${formatTimecodeVtt(w.startMs)}>"
                    if (w.isEmphasized) "$tag<c.emphasis>$raw</c>" else "$tag<c>$raw</c>"
                }
                appendLine(karaokeLine)
            } else {
                appendLine(if (project.isUppercase) seg.text.uppercase() else seg.text)
            }
            if (project.showBilingual && seg.secondaryText.isNotBlank()) {
                appendLine(seg.secondaryText)
            }
            appendLine()
        }
    }

    private fun exportAss(
        project: SubtitleProjectEntity,
        segments: List<CaptionSegmentEntity>
    ): String = buildString {
        val (resX, resY) = when (project.aspectRatioMode) {
            AspectRatioMode.RATIO_9_16 -> 1080 to 1920
            AspectRatioMode.RATIO_16_9 -> 1920 to 1080
            AspectRatioMode.RATIO_1_1 -> 1080 to 1080
            AspectRatioMode.RATIO_4_5 -> 1080 to 1350
        }
        val primaryAssColor = hexToAssColor(project.activeWordColorHex)
        val secondaryAssColor = hexToAssColor(project.textColorHex)
        val outlineAssColor = hexToAssColor(project.strokeColorHex)
        val backAssColor = hexToAssColor(project.bgColorHex, alphaFraction = project.bgOpacity)
        val marginV = ((1f - project.verticalPositionRatio) * resY).roundToInt().coerceIn(40, resY - 80)

        appendLine("[Script Info]")
        appendLine("; Phụ đề được tạo bởi Hendy Vietsub")
        appendLine("Title: ${project.title}")
        appendLine("ScriptType: v4.00+")
        appendLine("PlayResX: $resX")
        appendLine("PlayResY: $resY")
        appendLine("ScaledBorderAndShadow: yes")
        appendLine()
        appendLine("[V4+ Styles]")
        appendLine("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding")
        appendLine(
            "Style: HendyMain,${project.subtitleFont.exportFontName},${(project.fontSizeSp * 2.4f).roundToInt()},$primaryAssColor,$secondaryAssColor,$outlineAssColor,$backAssColor,-1,0,0,0,100,100,0,0,1,${project.strokeWidthDp.roundToInt()},${(project.shadowBlur / 2f).roundToInt()},2,40,40,$marginV,1"
        )
        appendLine()
        appendLine("[Events]")
        appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")

        for (seg in segments) {
            val startAss = formatAssTime(seg.startMs)
            val endAss = formatAssTime(seg.endMs)
            val words = seg.parsedWords()
            val karaokeText = buildString {
                for (w in words) {
                    val durCs = max(5L, (w.endMs - w.startMs) / 10L)
                    val displayWord = if (project.isUppercase) w.word.uppercase() else w.word
                    append("{\\kf$durCs}$displayWord ")
                }
                if (project.showBilingual && seg.secondaryText.isNotBlank()) {
                    append("\\N{\\fs${(project.fontSizeSp * 1.6f).roundToInt()}}${seg.secondaryText}")
                }
            }.trim()
            appendLine("Dialogue: 0,$startAss,$endAss,HendyMain,${seg.speakerTag},0,0,0,,$karaokeText")
        }
    }

    private fun exportTxt(
        project: SubtitleProjectEntity,
        segments: List<CaptionSegmentEntity>
    ): String = buildString {
        appendLine("# ${project.title}")
        appendLine("# Tổng thời lượng: ${formatTimecodeShort(project.durationMs)} | Số câu: ${segments.size}")
        appendLine("------------------------------------------------------------")
        appendLine()
        for (seg in segments) {
            appendLine("[${formatTimecodeShort(seg.startMs)} -> ${formatTimecodeShort(seg.endMs)}] (${seg.speakerTag})")
            appendLine(seg.text)
            if (seg.secondaryText.isNotBlank()) {
                appendLine("  ↳ ${seg.secondaryText}")
            }
            appendLine()
        }
    }

    private fun toMillis(h: String, m: String, s: String, ms: String): Long {
        val hours = h.toLongOrNull() ?: 0L
        val mins = m.toLongOrNull() ?: 0L
        val secs = s.toLongOrNull() ?: 0L
        val millis = ms.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return hours * 3_600_000L + mins * 60_000L + secs * 1000L + millis
    }

    private fun formatAssTime(ms: Long): String {
        val safe = max(0L, ms)
        val hours = safe / 3_600_000L
        val mins = (safe % 3_600_000L) / 60_000L
        val secs = (safe % 60_000L) / 1000L
        val cs = (safe % 1000L) / 10L
        return "%d:%02d:%02d.%02d".format(hours, mins, secs, cs)
    }

    private fun hexToAssColor(hex: String, alphaFraction: Float = 1f): String {
        val clean = hex.removePrefix("#").padStart(6, '0').takeLast(6)
        val r = clean.substring(0, 2)
        val g = clean.substring(2, 4)
        val b = clean.substring(4, 6)
        val assAlpha = ((1f - alphaFraction.coerceIn(0f, 1f)) * 255).roundToInt().coerceIn(0, 255)
        return "&H%02X%s%s%s".format(assAlpha, b.uppercase(), g.uppercase(), r.uppercase())
    }
}
