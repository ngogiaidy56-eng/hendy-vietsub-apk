package com.example.data

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class TranslationLanguage(val code: String, val displayName: String, val flag: String) {
    AUTO("auto", "Tự động phát hiện", "🌐"),
    ENGLISH("en", "Tiếng Anh (English)", "🇺🇸"),
    CHINESE("zh-CN", "Tiếng Trung (中文)", "🇨🇳"),
    JAPANESE("ja", "Tiếng Nhật (日本語)", "🇯🇵"),
    KOREAN("ko", "Tiếng Hàn (한국어)", "🇰🇷"),
    FRENCH("fr", "Tiếng Pháp (Français)", "🇫🇷"),
    SPANISH("es", "Tiếng Tây Ban Nha", "🇪🇸"),
    VIETNAMESE("vi", "Tiếng Việt", "🇻🇳")
}

data class TranslatedEntry(
    val id: Long = System.currentTimeMillis(),
    val sourceText: String,
    val translatedText: String,
    val sourceLangLabel: String,
    val targetLangLabel: String,
    val sourceOrigin: String = "Website / Ứng dụng",
    val timestampMs: Long = System.currentTimeMillis()
)

data class WebpageBlock(
    val id: Int,
    val tag: String, // "H1", "H2", "P", "VIDEO_SUB"
    val originalText: String,
    val translatedText: String = ""
)

data class ParsedWebpage(
    val url: String,
    val title: String,
    val blocks: List<WebpageBlock>
)

object LiveTranslationEngine {

    private val offlinePhrasebook = mapOf(
        "stop scrolling if you edit short videos!" to "Đừng lướt qua nếu bạn đang làm video ngắn!",
        "90% of viewers watch on mute with captions" to "90% người xem tắt tiếng và chỉ đọc phụ đề",
        "word-by-word pop keeps retention at 100%" to "Hiệu ứng nhảy chữ từng từ giúp giữ chân người xem 100%",
        "breaking news" to "Tin nóng cập nhật",
        "welcome to our live stream" to "Chào mừng các bạn đến với buổi phát trực tiếp",
        "subscribe and turn on notifications" to "Đăng ký kênh và bật chuông thông báo nhé",
        "artificial intelligence is transforming video editing" to "Trí tuệ nhân tạo đang thay đổi hoàn toàn ngành dựng video",
        "real-time subtitle translation for websites and apps" to "Dịch phụ đề trực tiếp theo thời gian thực cho trang web và ứng dụng",
        "tap any paragraph on this website to translate instantly" to "Chạm vào bất kỳ đoạn văn nào trên trang web này để dịch ngay lập tức",
        "how to create viral short-form videos" to "Cách tạo video ngắn lên xu hướng triệu lượt xem",
        "the quick brown fox jumps over the lazy dog" to "Chú cáo nâu nhanh nhẹn nhảy qua chú chó lười biếng"
    )

    private val wordDictionary = mapOf(
        "hello" to "xin chào",
        "world" to "thế giới",
        "video" to "video",
        "subtitle" to "phụ đề",
        "subtitles" to "phụ đề",
        "caption" to "chú thích",
        "captions" to "phụ đề",
        "translate" to "dịch",
        "translation" to "bản dịch",
        "website" to "trang web",
        "app" to "ứng dụng",
        "application" to "ứng dụng",
        "live" to "trực tiếp",
        "real-time" to "thời gian thực",
        "audio" to "âm thanh",
        "voice" to "giọng nói",
        "music" to "âm nhạc",
        "movie" to "bộ phim",
        "creator" to "nhà sáng tạo",
        "new" to "mới",
        "free" to "miễn phí",
        "download" to "tải xuống",
        "share" to "chia sẻ",
        "edit" to "chỉnh sửa",
        "editor" to "trình chỉnh sửa",
        "automatic" to "tự động",
        "language" to "ngôn ngữ",
        "settings" to "cài đặt",
        "today" to "hôm nay",
        "news" to "tin tức",
        "technology" to "công nghệ",
        "future" to "tương lai",
        "learn" to "học",
        "guide" to "hướng dẫn",
        "best" to "tốt nhất",
        "fast" to "nhanh",
        "easy" to "dễ dàng"
    )

    suspend fun translateText(
        rawText: String,
        sourceLang: TranslationLanguage = TranslationLanguage.AUTO,
        targetLang: TranslationLanguage = TranslationLanguage.VIETNAMESE
    ): String = withContext(Dispatchers.IO) {
        val cleaned = rawText.trim()
        if (cleaned.isEmpty()) return@withContext ""

        // Check exact phrasebook first for instant zero-latency hits
        offlinePhrasebook[cleaned.lowercase()]?.let {
            if (targetLang == TranslationLanguage.VIETNAMESE) return@withContext it
        }

        // Try live Google Translate GTX endpoint
        try {
            val encoded = URLEncoder.encode(cleaned, "UTF-8")
            val sl = sourceLang.code
            val tl = targetLang.code
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$encoded"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile)")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val rootArray = JSONArray(body)
                val sentencesArray = rootArray.optJSONArray(0)
                if (sentencesArray != null) {
                    val sb = StringBuilder()
                    for (i in 0 until sentencesArray.length()) {
                        val seg = sentencesArray.optJSONArray(i)
                        val translatedPiece = seg?.optString(0).orEmpty()
                        sb.append(translatedPiece)
                    }
                    val finalResult = sb.toString().trim()
                    if (finalResult.isNotBlank()) {
                        return@withContext finalResult
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to smart offline fallback
        }

        fallbackSmartTranslate(cleaned, targetLang)
    }

    private fun fallbackSmartTranslate(text: String, targetLang: TranslationLanguage): String {
        if (targetLang != TranslationLanguage.VIETNAMESE) {
            return "[${targetLang.displayName}] $text"
        }
        val words = text.split(Regex("\\s+"))
        var matchedCount = 0
        val mapped = words.map { token ->
            val punctuation = token.takeLastWhile { !it.isLetterOrDigit() }
            val core = token.dropLast(punctuation.length).lowercase()
            val hit = wordDictionary[core]
            if (hit != null) {
                matchedCount++
                hit + punctuation
            } else {
                token
            }
        }
        return if (matchedCount > 0) {
            mapped.joinToString(" ").replaceFirstChar { it.uppercase() }
        } else {
            "[Bản dịch Vietsub] $text"
        }
    }

    suspend fun fetchAndParseWebpage(urlInput: String): ParsedWebpage = withContext(Dispatchers.IO) {
        val normalizedUrl = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
        try {
            val conn = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            }
            if (conn.responseCode in 200..299) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                val titleMatch = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(html)?.groupValues?.getOrNull(1)
                    ?.replace(Regex("<[^>]+>"), "")
                    ?.trim()
                    .orEmpty()

                // Remove script and style blocks
                val cleanedHtml = html
                    .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                    .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")

                val tagRegex = Regex("<(h1|h2|h3|p)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                val extracted = mutableListOf<WebpageBlock>()
                var index = 1
                for (match in tagRegex.findAll(cleanedHtml)) {
                    val tag = match.groupValues[1].uppercase()
                    val rawInner = match.groupValues[2]
                        .replace(Regex("<[^>]+>"), " ")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    if (rawInner.length >= 24) {
                        extracted.add(WebpageBlock(id = index++, tag = tag, originalText = rawInner.take(280)))
                    }
                    if (extracted.size >= 12) break
                }

                if (extracted.isNotEmpty()) {
                    return@withContext ParsedWebpage(
                        url = normalizedUrl,
                        title = titleMatch.ifBlank { normalizedUrl },
                        blocks = extracted
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to rich built-in interactive web content
        }

        defaultSampleWebpage(normalizedUrl)
    }

    private fun defaultSampleWebpage(url: String): ParsedWebpage {
        return ParsedWebpage(
            url = url,
            title = "Creator Daily • Real-Time Video & Web Subtitles Guide",
            blocks = listOf(
                WebpageBlock(
                    id = 1,
                    tag = "H1",
                    originalText = "How Real-Time Subtitles Transform Global Video & Web Content"
                ),
                WebpageBlock(
                    id = 2,
                    tag = "VIDEO_SUB",
                    originalText = "Stop scrolling if you edit short videos! 90% of viewers watch on mute with captions."
                ),
                WebpageBlock(
                    id = 3,
                    tag = "P",
                    originalText = "Artificial intelligence is transforming video editing. Real-time subtitle translation for websites and apps helps creators reach audiences in every language."
                ),
                WebpageBlock(
                    id = 4,
                    tag = "P",
                    originalText = "Word-by-word pop keeps retention at 100%. Tap any paragraph on this website to translate instantly into Vietnamese!"
                ),
                WebpageBlock(
                    id = 5,
                    tag = "H2",
                    originalText = "How to create viral short-form videos"
                ),
                WebpageBlock(
                    id = 6,
                    tag = "P",
                    originalText = "Welcome to our live stream! Subscribe and turn on notifications to never miss new creator tutorials."
                )
            )
        )
    }
}

/**
 * Manages the system-wide floating overlay bar (matching Image 2: black pill with white double border)
 * so users can translate text on websites or other apps in real time.
 */
object CrossAppOverlayController {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isShowing = false

    private val _isBubbleEnabled = MutableStateFlow(true)
    val isBubbleEnabledFlow: StateFlow<Boolean> = _isBubbleEnabled.asStateFlow()

    private val _isBubbleExpanded = MutableStateFlow(false)
    val isBubbleExpandedFlow: StateFlow<Boolean> = _isBubbleExpanded.asStateFlow()

    private val _autoClipboardTranslate = MutableStateFlow(true)
    val autoClipboardTranslateFlow: StateFlow<Boolean> = _autoClipboardTranslate.asStateFlow()

    private val _bubbleOpacity = MutableStateFlow(0.95f)
    val bubbleOpacityFlow: StateFlow<Float> = _bubbleOpacity.asStateFlow()

    private val _lastBubbleTranslation = MutableStateFlow(
        "Đừng lướt qua nếu bạn đang làm video ngắn! 90% người xem tắt tiếng và chỉ đọc phụ đề."
    )
    val lastBubbleTranslationFlow: StateFlow<String> = _lastBubbleTranslation.asStateFlow()

    fun isOverlayActive(): Boolean = _isBubbleEnabled.value || isShowing

    fun setBubbleExpanded(expanded: Boolean) {
        _isBubbleExpanded.value = expanded
    }

    fun setAutoClipboardTranslate(enabled: Boolean) {
        _autoClipboardTranslate.value = enabled
    }

    fun setBubbleOpacity(opacity: Float) {
        _bubbleOpacity.value = opacity.coerceIn(0.45f, 1.0f)
    }

    fun updateBubbleTranslation(text: String) {
        if (text.isNotBlank()) {
            _lastBubbleTranslation.value = text
        }
    }

    fun setBubbleEnabled(
        context: Context,
        enabled: Boolean,
        onNewTranslation: (TranslatedEntry) -> Unit = {}
    ): Boolean {
        _isBubbleEnabled.value = enabled
        if (enabled) {
            if (canDrawOverlays(context)) {
                startFloatingOverlay(context, onNewTranslation)
            }
            return true
        } else {
            stopFloatingOverlay()
            return false
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun startFloatingOverlay(
        context: Context,
        onNewTranslation: (TranslatedEntry) -> Unit
    ): Boolean {
        _isBubbleEnabled.value = true
        if (isShowing) return true
        if (!canDrawOverlays(context)) return true

        val appContext = context.applicationContext
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = appContext.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val rootContainer = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }

        // Subtitle result popup banner above the black pill
        val resultBanner = TextView(appContext).apply {
            text = "🌐 Hendy Vietsub sẵn sàng • Sao chép văn bản ở bất kỳ ứng dụng nào rồi bấm [︾] để dịch ngay!"
            setTextColor(AndroidColor.parseColor("#CCFF00"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            background = GradientDrawable().apply {
                setColor(AndroidColor.parseColor("#E610131A"))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), AndroidColor.parseColor("#00E5FF"))
            }
        }

        // Exact Black Pill Bar from Image 2
        val pillBar = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(14), dp(6))
            background = GradientDrawable().apply {
                setColor(AndroidColor.parseColor("#F208090C"))
                cornerRadius = dp(30).toFloat()
                setStroke(dp(2), AndroidColor.parseColor("#D9FFFFFF"))
            }
        }

        // Left circular avatar badge inside pill
        val avatarBadge = TextView(appContext).apply {
            text = "HV"
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            paint.isFakeBoldText = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(AndroidColor.parseColor("#00E5FF"))
            }
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                marginEnd = dp(16)
            }
        }

        // Center-left: [︾] Auto-Translate Clipboard / Webpage
        val btnAutoTranslate = TextView(appContext).apply {
            text = "〘︾〙 Dịch ngay"
            setTextColor(AndroidColor.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            paint.isFakeBoldText = true
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipText = cm.primaryClip?.getItemAt(0)?.coerceToText(appContext)?.toString()?.trim().orEmpty()
                val textToTranslate = if (clipText.isNotBlank()) {
                    clipText
                } else {
                    "Real-time subtitle translation for websites and apps"
                }
                resultBanner.text = "⏳ Đang dịch: $textToTranslate..."
                scope.launch {
                    val translated = LiveTranslationEngine.translateText(textToTranslate)
                    resultBanner.text = "🇻🇳 $translated"
                    onNewTranslation(
                        TranslatedEntry(
                            sourceText = textToTranslate,
                            translatedText = translated,
                            sourceLangLabel = "Tự động",
                            targetLangLabel = "Tiếng Việt",
                            sourceOrigin = "Thanh nổi Đa ứng dụng"
                        )
                    )
                }
            }
        }

        // Center-right: [✎] Toggle Banner visibility
        val btnCropEdit = TextView(appContext).apply {
            text = "  ✂ Ẩn/Hiện  "
            setTextColor(AndroidColor.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            paint.isFakeBoldText = true
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setOnClickListener {
                resultBanner.visibility = if (resultBanner.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }

        // Right: [✕] Close overlay
        val btnClose = TextView(appContext).apply {
            text = "  ✕"
            setTextColor(AndroidColor.parseColor("#FF4D6D"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            paint.isFakeBoldText = true
            setPadding(dp(8), dp(6), dp(4), dp(6))
            setOnClickListener {
                stopFloatingOverlay()
            }
        }

        pillBar.addView(avatarBadge)
        pillBar.addView(btnAutoTranslate)
        pillBar.addView(btnCropEdit)
        pillBar.addView(btnClose)

        rootContainer.addView(
            resultBanner,
            LinearLayout.LayoutParams(dp(320), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            }
        )
        rootContainer.addView(
            pillBar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(110)
        }

        // Allow dragging the floating pill anywhere on screen
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        avatarBadge.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    runCatching { wm.updateViewLayout(rootContainer, params) }
                    true
                }
                else -> false
            }
        }

        return try {
            Handler(Looper.getMainLooper()).post {
                runCatching {
                    wm.addView(rootContainer, params)
                    overlayView = rootContainer
                    isShowing = true
                    Toast.makeText(
                        appContext,
                        "Đã bật Thanh Nổi Hendy Vietsub trên màn hình!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stopFloatingOverlay() {
        _isBubbleEnabled.value = false
        val view = overlayView ?: return
        val wm = windowManager
        Handler(Looper.getMainLooper()).post {
            runCatching { wm?.removeView(view) }
            overlayView = null
            isShowing = false
        }
    }
}
