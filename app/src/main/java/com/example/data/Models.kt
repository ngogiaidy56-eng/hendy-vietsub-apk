package com.example.data

import androidx.compose.ui.text.font.FontFamily
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.BangersFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MontserratFontFamily
import com.example.ui.theme.PermanentMarkerFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.max
import kotlin.math.roundToInt

enum class AspectRatioMode(val label: String, val ratioValue: Float, val shortLabel: String) {
    RATIO_9_16("9:16 Dọc (TikTok/Reels)", 9f / 16f, "9:16"),
    RATIO_16_9("16:9 Ngang (YouTube)", 16f / 9f, "16:9"),
    RATIO_1_1("1:1 Vuông (Bài đăng)", 1f, "1:1"),
    RATIO_4_5("4:5 Chân dung (Feed)", 4f / 5f, "4:5");

    fun next(): AspectRatioMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

enum class CanvasBackdrop(val label: String) {
    CYBER_GRID("Lưới Cyber"),
    CINEMA_NOIR("Điện ảnh Noir"),
    SUNSET_VLOG("Hoàng hôn Vlog"),
    NEON_BOKEH("Bokeh Studio"),
    CHROMA_GREEN("Phông xanh")
}

enum class WordAnimationMode(val label: String, val description: String) {
    KARAOKE_POP("Nảy chữ", "Phóng to & đổi màu từ đang phát âm"),
    KARAOKE_BOX("Khung Karaoke", "Khung nền màu trượt theo từng từ"),
    TYPEWRITER("Gõ chữ", "Hiện dần từng từ theo giọng đọc"),
    NEON_PULSE("Phát sáng Neon", "Hiệu ứng hào quang điện tử quanh từ"),
    BOUNCE_IN("Nảy lò xo", "Chuyển động bật lên khi vào câu mới"),
    STATIC_CLEAN("Truyền hình", "Phụ đề tĩnh chuẩn phim & tin tức")
}

enum class SubtitleFontOption(
    val displayName: String,
    val exportFontName: String,
    val sampleTag: String
) {
    MONTSERRAT("Montserrat Đậm", "Montserrat", "XU HƯỚNG"),
    SPACE_GROTESK("Space Grotesk", "Space Grotesk", "HIỆN ĐẠI"),
    BANGERS("Bangers Nổi Bật", "Bangers", "ẤN TƯỢNG"),
    PLUS_JAKARTA("Jakarta Thanh Lịch", "Plus Jakarta Sans", "RÕ NÉT"),
    PERMANENT_MARKER("Bút Dạ Đường Phố", "Permanent Marker", "VLOG"),
    JETBRAINS_MONO("JetBrains Mono", "JetBrains Mono", "KỸ THUẬT");

    fun toFontFamily(): FontFamily = when (this) {
        MONTSERRAT -> MontserratFontFamily
        SPACE_GROTESK -> SpaceGroteskFontFamily
        BANGERS -> BangersFontFamily
        PLUS_JAKARTA -> PlusJakartaSansFontFamily
        PERMANENT_MARKER -> PermanentMarkerFontFamily
        JETBRAINS_MONO -> JetBrainsMonoFontFamily
    }
}

enum class ExportFormat(val extension: String, val mimeType: String, val title: String, val subtitle: String) {
    SRT("srt", "application/x-subrip", "SubRip (.SRT)", "Chuẩn phổ biến cho YouTube, Premiere & CapCut"),
    VTT("vtt", "text/vtt", "WebVTT (.VTT)", "Phụ đề web HTML5 kèm vị trí khung hình & thẻ từ"),
    ASS("ass", "text/plain", "SubStation Alpha (.ASS)", "Giữ nguyên phông chữ, màu sắc, viền & thẻ Karaoke \\k"),
    TXT("txt", "text/plain", "Bản thảo kèm giờ (.TXT)", "Văn bản dễ đọc gồm mốc thời gian & tên người nói")
}

data class WordTiming(
    val word: String,
    val startMs: Long,
    val endMs: Long,
    val isEmphasized: Boolean = false
)

@Entity(tableName = "subtitle_projects")
data class SubtitleProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val durationMs: Long = 16000L,
    val aspectRatio: String = AspectRatioMode.RATIO_9_16.name,
    val backdrop: String = CanvasBackdrop.CYBER_GRID.name,
    val videoUri: String? = null,
    val verticalPositionRatio: Float = 0.76f,
    val showSafeZones: Boolean = true,
    val showBilingual: Boolean = true,
    val fontOption: String = SubtitleFontOption.MONTSERRAT.name,
    val fontSizeSp: Float = 24f,
    val textColorHex: String = "#FFFFFF",
    val activeWordColorHex: String = "#CCFF00",
    val strokeColorHex: String = "#000000",
    val strokeWidthDp: Float = 3.5f,
    val shadowBlur: Float = 6f,
    val bgColorHex: String = "#0B0D12",
    val bgOpacity: Float = 0.45f,
    val bgCornerRadiusDp: Float = 10f,
    val isUppercase: Boolean = true,
    val animationMode: String = WordAnimationMode.KARAOKE_POP.name,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val aspectRatioMode: AspectRatioMode
        get() = runCatching { AspectRatioMode.valueOf(aspectRatio) }.getOrDefault(AspectRatioMode.RATIO_9_16)

    val backdropMode: CanvasBackdrop
        get() = runCatching { CanvasBackdrop.valueOf(backdrop) }.getOrDefault(CanvasBackdrop.CYBER_GRID)

    val subtitleFont: SubtitleFontOption
        get() = runCatching { SubtitleFontOption.valueOf(fontOption) }.getOrDefault(SubtitleFontOption.MONTSERRAT)

    val wordAnimation: WordAnimationMode
        get() = runCatching { WordAnimationMode.valueOf(animationMode) }.getOrDefault(WordAnimationMode.KARAOKE_POP)
}

@Entity(tableName = "caption_segments")
data class CaptionSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val secondaryText: String = "",
    val speakerTag: String = "Người nói 1",
    val wordsSerialized: String = ""
) {
    val durationMs: Long
        get() = max(100L, endMs - startMs)

    val charactersPerSecond: Float
        get() {
            val sec = durationMs / 1000f
            return if (sec > 0f) (text.length / sec * 10f).roundToInt() / 10f else 0f
        }

    fun parsedWords(): List<WordTiming> {
        if (wordsSerialized.isNotBlank()) {
            val parsed = wordsSerialized.split("|").mapNotNull { token ->
                val parts = token.split("~")
                if (parts.size >= 3) {
                    val w = parts[0]
                    val s = parts[1].toLongOrNull() ?: startMs
                    val e = parts[2].toLongOrNull() ?: endMs
                    val emp = parts.getOrNull(3) == "1"
                    if (w.isNotBlank()) WordTiming(w, s, e, emp) else null
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return distributeWordsEvenly(text, startMs, endMs)
    }
}

@Entity(tableName = "style_presets")
data class StylePresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val badge: String,
    val fontOption: String,
    val fontSizeSp: Float,
    val textColorHex: String,
    val activeWordColorHex: String,
    val strokeColorHex: String,
    val strokeWidthDp: Float,
    val shadowBlur: Float,
    val bgColorHex: String,
    val bgOpacity: Float,
    val bgCornerRadiusDp: Float,
    val isUppercase: Boolean,
    val animationMode: String,
    val isBuiltIn: Boolean = false
)

fun distributeWordsEvenly(
    text: String,
    startMs: Long,
    endMs: Long,
    existingWords: List<WordTiming> = emptyList()
): List<WordTiming> {
    val rawWords = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (rawWords.isEmpty()) return emptyList()
    val totalDuration = max(100L, endMs - startMs)
    val totalChars = max(1, rawWords.sumOf { max(2, it.length) })
    var cursor = startMs
    return rawWords.mapIndexed { index, word ->
        val weight = max(2, word.length).toFloat() / totalChars.toFloat()
        val slice = (totalDuration * weight).toLong().coerceAtLeast(60L)
        val wordStart = cursor
        val wordEnd = if (index == rawWords.lastIndex) endMs else (cursor + slice).coerceAtMost(endMs)
        cursor = wordEnd
        val wasEmphasized = existingWords.getOrNull(index)?.isEmphasized
            ?: existingWords.any { it.word.equals(word, ignoreCase = true) && it.isEmphasized }
        WordTiming(
            word = word,
            startMs = wordStart,
            endMs = max(wordStart + 40L, wordEnd),
            isEmphasized = wasEmphasized
        )
    }
}

fun List<WordTiming>.serializeWords(): String {
    return joinToString("|") { w ->
        val safeWord = w.word.replace("|", "").replace("~", "")
        "$safeWord~${w.startMs}~${w.endMs}~${if (w.isEmphasized) "1" else "0"}"
    }
}

fun formatTimecodeShort(ms: Long): String {
    val safe = max(0L, ms)
    val totalSeconds = safe / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (safe % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
}

fun formatTimecodeSrt(ms: Long): String {
    val safe = max(0L, ms)
    val hours = safe / 3_600_000
    val minutes = (safe % 3_600_000) / 60_000
    val seconds = (safe % 60_000) / 1000
    val millis = safe % 1000
    return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
}

fun formatTimecodeVtt(ms: Long): String {
    return formatTimecodeSrt(ms).replace(',', '.')
}
