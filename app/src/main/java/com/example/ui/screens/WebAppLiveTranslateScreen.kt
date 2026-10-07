package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CrossAppOverlayController
import com.example.data.LiveTranslationEngine
import com.example.data.ParsedWebpage
import com.example.data.TranslatedEntry
import com.example.data.TranslationLanguage
import com.example.data.WebpageBlock
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KineticLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun WebAppLiveTranslateScreen(
    initialSharedTextOrUrl: String? = null,
    onSendToSubtitleTimeline: (String) -> Unit,
    onClose: (() -> Unit)? = null
) {
    if (onClose != null) {
        BackHandler { onClose() }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urlInput by remember(initialSharedTextOrUrl) {
        mutableStateOf(
            if (initialSharedTextOrUrl?.startsWith("http") == true) {
                initialSharedTextOrUrl
            } else {
                "https://en.m.wikipedia.org/wiki/Subtitle_(captioning)"
            }
        )
    }

    var parsedPage by remember {
        mutableStateOf(
            ParsedWebpage(
                url = urlInput,
                title = "Creator Daily • Real-Time Video & Web Subtitles Guide",
                blocks = listOf(
                    WebpageBlock(
                        id = 1,
                        tag = "H1",
                        originalText = "How Real-Time Subtitles Transform Global Video & Web Content",
                        translatedText = "Cách phụ đề thời gian thực thay đổi nội dung Video & Web toàn cầu"
                    ),
                    WebpageBlock(
                        id = 2,
                        tag = "VIDEO_SUB",
                        originalText = "Stop scrolling if you edit short videos! 90% of viewers watch on mute with captions.",
                        translatedText = "Đừng lướt qua nếu bạn đang làm video ngắn! 90% người xem tắt tiếng và chỉ đọc phụ đề."
                    ),
                    WebpageBlock(
                        id = 3,
                        tag = "P",
                        originalText = "Artificial intelligence is transforming video editing. Real-time subtitle translation for websites and apps helps creators reach audiences in every language.",
                        translatedText = "Trí tuệ nhân tạo đang thay đổi hoàn toàn ngành dựng video. Dịch phụ đề trực tiếp theo thời gian thực cho trang web và ứng dụng giúp nhà sáng tạo tiếp cận khán giả ở mọi ngôn ngữ."
                    ),
                    WebpageBlock(
                        id = 4,
                        tag = "P",
                        originalText = "Word-by-word pop keeps retention at 100%. Tap any paragraph on this website to translate instantly into Vietnamese!",
                        translatedText = "Hiệu ứng nhảy chữ từng từ giúp giữ chân người xem 100%. Chạm vào bất kỳ đoạn văn nào trên trang web này để dịch ngay lập tức!"
                    ),
                    WebpageBlock(
                        id = 5,
                        tag = "H2",
                        originalText = "How to create viral short-form videos",
                        translatedText = "Cách tạo video ngắn lên xu hướng triệu lượt xem"
                    )
                )
            )
        )
    }

    var isAutoWebTranslateOn by remember { mutableStateOf(true) }
    var isTranslatingNow by remember { mutableStateOf(false) }
    var sourceLang by remember { mutableStateOf(TranslationLanguage.AUTO) }
    var targetLang by remember { mutableStateOf(TranslationLanguage.VIETNAMESE) }
    var liveCaptionBanner by remember {
        mutableStateOf("Đừng lướt qua nếu bạn đang làm video ngắn! 90% người xem tắt tiếng và chỉ đọc phụ đề.")
    }
    var customInputText by remember(initialSharedTextOrUrl) {
        mutableStateOf(
            if (initialSharedTextOrUrl != null && !initialSharedTextOrUrl.startsWith("http")) {
                initialSharedTextOrUrl
            } else {
                "Artificial intelligence is transforming video editing. Real-time subtitle translation for websites and apps."
            }
        )
    }
    var activeModeTab by remember { mutableStateOf(0) } // 0 = Dịch Trên Website, 1 = Dịch Ứng Dụng Khác
    var isSystemOverlayRunning by remember { mutableStateOf(CrossAppOverlayController.isOverlayActive()) }

    var translationHistory by remember {
        mutableStateOf(
            listOf(
                TranslatedEntry(
                    sourceText = "Real-time subtitle translation for websites and apps",
                    translatedText = "Dịch phụ đề trực tiếp theo thời gian thực cho trang web và ứng dụng",
                    sourceLangLabel = "Tiếng Anh",
                    targetLangLabel = "Tiếng Việt",
                    sourceOrigin = "Website Trực Tiếp"
                ),
                TranslatedEntry(
                    sourceText = "Stop scrolling if you edit short videos! 90% of viewers watch on mute with captions.",
                    translatedText = "Đừng lướt qua nếu bạn đang làm video ngắn! 90% người xem tắt tiếng và chỉ đọc phụ đề.",
                    sourceLangLabel = "Tiếng Anh",
                    targetLangLabel = "Tiếng Việt",
                    sourceOrigin = "Ứng dụng Video"
                )
            )
        )
    }

    fun translateAndShow(rawText: String, origin: String) {
        val clean = rawText.trim()
        if (clean.isEmpty()) return
        isTranslatingNow = true
        scope.launch {
            val result = LiveTranslationEngine.translateText(clean, sourceLang, targetLang)
            liveCaptionBanner = result
            val entry = TranslatedEntry(
                sourceText = clean,
                translatedText = result,
                sourceLangLabel = sourceLang.displayName,
                targetLangLabel = targetLang.displayName,
                sourceOrigin = origin
            )
            translationHistory = listOf(entry) + translationHistory.take(25)
            // Update block translation if matching
            parsedPage = parsedPage.copy(
                blocks = parsedPage.blocks.map { block ->
                    if (block.originalText == clean) block.copy(translatedText = result) else block
                }
            )
            isTranslatingNow = false
        }
    }

    fun loadAndTranslateWebpage(targetUrl: String) {
        val normalizedUrl = if (targetUrl.startsWith("http")) targetUrl else "https://$targetUrl"
        urlInput = normalizedUrl
        isTranslatingNow = true
        scope.launch {
            val fetched = LiveTranslationEngine.fetchAndParseWebpage(normalizedUrl)
            val translatedBlocks = if (isAutoWebTranslateOn) {
                fetched.blocks.map { block ->
                    val vi = LiveTranslationEngine.translateText(block.originalText, sourceLang, targetLang)
                    block.copy(translatedText = vi)
                }
            } else {
                fetched.blocks
            }
            parsedPage = fetched.copy(blocks = translatedBlocks)
            translatedBlocks.firstOrNull { it.translatedText.isNotBlank() }?.let { first ->
                liveCaptionBanner = first.translatedText
            }
            isTranslatingNow = false
        }
    }

    fun triggerFullPageWebScan() {
        if (activeModeTab == 0) {
            isTranslatingNow = true
            scope.launch {
                val updatedBlocks = parsedPage.blocks.map { block ->
                    val vi = LiveTranslationEngine.translateText(block.originalText, sourceLang, targetLang)
                    block.copy(translatedText = vi)
                }
                parsedPage = parsedPage.copy(blocks = updatedBlocks)
                val firstTranslated = updatedBlocks.firstOrNull()?.translatedText.orEmpty()
                if (firstTranslated.isNotBlank()) {
                    liveCaptionBanner = firstTranslated
                    val firstOrig = updatedBlocks.first().originalText
                    translationHistory = listOf(
                        TranslatedEntry(
                            sourceText = firstOrig,
                            translatedText = firstTranslated,
                            sourceLangLabel = sourceLang.displayName,
                            targetLangLabel = targetLang.displayName,
                            sourceOrigin = "Quét toàn trang Web"
                        )
                    ) + translationHistory.take(25)
                }
                isTranslatingNow = false
            }
        } else {
            translateAndShow(customInputText, "Dịch nhanh Ứng dụng")
        }
    }

    LaunchedEffect(initialSharedTextOrUrl) {
        if (!initialSharedTextOrUrl.isNullOrBlank()) {
            if (initialSharedTextOrUrl.startsWith("http")) {
                loadAndTranslateWebpage(initialSharedTextOrUrl)
            } else {
                translateAndShow(initialSharedTextOrUrl, "Văn bản chia sẻ từ Ứng dụng ngoài")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101218))
            .statusBarsPadding()
            .testTag("web_app_live_translate_screen")
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161922))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Dịch Trực Tiếp Web & Ứng Dụng",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Phụ đề nổi thời gian thực trên mọi trang web & app",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.White
                    )
                }
            }
        }

        // Mode Switcher Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161922))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (activeModeTab == 0) ElectricCyan else Color(0xFF232734),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeModeTab = 0 }
                    .testTag("tab_live_website")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (activeModeTab == 0) ObsidianBg else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dịch Trên Website",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeModeTab == 0) ObsidianBg else Color.White
                    )
                }
            }

            Surface(
                color = if (activeModeTab == 1) ElectricCyan else Color(0xFF232734),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeModeTab = 1 }
                    .testTag("tab_cross_app_overlay")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (activeModeTab == 1) ObsidianBg else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dịch Ứng Dụng Khác",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeModeTab == 1) ObsidianBg else Color.White
                    )
                }
            }
        }

        // Source Language Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(TranslationLanguage.entries.filter { it != TranslationLanguage.VIETNAMESE }) { lang ->
                val selected = sourceLang == lang
                Surface(
                    color = if (selected) ElectricCyan.copy(alpha = 0.2f) else Color(0xFF1D222E),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) ElectricCyan else StudioCardBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            sourceLang = lang
                            triggerFullPageWebScan()
                        }
                ) {
                    Text(
                        text = "${lang.flag} ${lang.displayName} → 🇻🇳 Tiếng Việt",
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) ElectricCyan else TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Main Interactive Content Area with Image 2 Floating Black Pill Overlay
        Box(modifier = Modifier.weight(1f)) {
            if (activeModeTab == 0) {
                // TAB 0: Native Interactive Website Reader & Live Subtitle Streamer (Zero WebView / MESA errors)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            singleLine = true,
                            placeholder = { Text("Nhập địa chỉ trang web (URL)...", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = StudioCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("web_url_input")
                        )
                        Button(
                            onClick = { loadAndTranslateWebpage(urlInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("open_web_url_btn")
                        ) {
                            Text("Mở Web", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { triggerFullPageWebScan() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF232734), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Dịch lại trang",
                                tint = KineticLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Quick Bookmark Chips
                    val bookmarks = listOf(
                        "Wikipedia Subtitles" to "https://en.m.wikipedia.org/wiki/Subtitle_(captioning)",
                        "BBC Tech News" to "https://www.bbc.com/news/technology",
                        "Creator Video Guide" to "https://creatordaily.io/viral-captions"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(bookmarks) { (title, link) ->
                            Surface(
                                color = Color(0xFF1A1F2C),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
                                    .clickable { loadAndTranslateWebpage(link) }
                            ) {
                                Text(
                                    text = "🌐 $title",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Interactive Webpage Content + Bilingual Subtitles
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF151922))
                            .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp)),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = parsedPage.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = parsedPage.url,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = ElectricCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = if (isAutoWebTranslateOn) KineticLime.copy(alpha = 0.2f) else Color(0xFF232734),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (isAutoWebTranslateOn) KineticLime else StudioCardBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            isAutoWebTranslateOn = !isAutoWebTranslateOn
                                            if (isAutoWebTranslateOn) triggerFullPageWebScan()
                                        }
                                ) {
                                    Text(
                                        text = if (isAutoWebTranslateOn) "Vietsub Tự Động: BẬT" else "Vietsub: TẮT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAutoWebTranslateOn) KineticLime else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        items(parsedPage.blocks, key = { it.id }) { block ->
                            val isVideoSub = block.tag == "VIDEO_SUB"
                            val isHeading = block.tag.startsWith("H")
                            Surface(
                                color = if (isVideoSub) Color(0xFF1D2636) else Color(0xFF1B202C),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isVideoSub) ElectricCyan.copy(alpha = 0.6f) else StudioCardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        translateAndShow(block.originalText, "Chạm trực tiếp trên Web")
                                    }
                                    .testTag("webpage_block_${block.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isVideoSub) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircleFilled,
                                                    contentDescription = null,
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isVideoSub) "VIDEO WEB STREAM • CHẠM ĐỂ DỊCH" else "ĐOẠN VĂN WEB (${block.tag}) • CHẠM ĐỂ DỊCH",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isVideoSub) ElectricCyan else TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "+ Timeline",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan,
                                            modifier = Modifier.clickable {
                                                val target = block.translatedText.ifBlank { block.originalText }
                                                onSendToSubtitleTimeline(target)
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = block.originalText,
                                        fontSize = if (isHeading) 15.sp else 13.sp,
                                        fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White
                                    )

                                    if (block.translatedText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFF0E131B),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, KineticLime.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                        ) {
                                            Text(
                                                text = "🇻🇳 ${block.translatedText}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KineticLime,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(145.dp))
                        }
                    }
                }
            } else {
                // TAB 1: Cross-App Floating Overlay & Instant Clipboard/App Translator
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Enable System-Wide Floating Pill Over Other Apps
                    item {
                        Surface(
                            color = Color(0xFF1A1E29),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Thanh Nổi Dịch Trên Ứng Dụng Khác",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Hiển thị thanh công cụ đen nổi trên Chrome, YouTube, TikTok, Telegram để dịch phụ đề theo thời gian thực.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = isSystemOverlayRunning,
                                        onCheckedChange = { wantActive ->
                                            if (wantActive) {
                                                if (CrossAppOverlayController.canDrawOverlays(context)) {
                                                    val ok = CrossAppOverlayController.startFloatingOverlay(context) { entry ->
                                                        translationHistory = listOf(entry) + translationHistory
                                                        liveCaptionBanner = entry.translatedText
                                                    }
                                                    isSystemOverlayRunning = ok
                                                } else {
                                                    runCatching {
                                                        context.startActivity(
                                                            CrossAppOverlayController.requestOverlayPermissionIntent(context)
                                                        )
                                                    }
                                                }
                                            } else {
                                                CrossAppOverlayController.stopFloatingOverlay()
                                                isSystemOverlayRunning = false
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ObsidianBg,
                                            checkedTrackColor = ElectricCyan
                                        ),
                                        modifier = Modifier.testTag("toggle_system_overlay_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 Mẹo: Bạn cũng có thể bôi đen văn bản trong bất kỳ trình duyệt hoặc ứng dụng nào và chọn \"Hendy Vietsub\" trong menu hiện lên để dịch ngay!",
                                    fontSize = 11.sp,
                                    color = KineticLime
                                )
                            }
                        }
                    }

                    // Card 2: Quick Cross-App Text / Clipboard Box
                    item {
                        Surface(
                            color = Color(0xFF1A1E29),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DỊCH NHANH VĂN BẢN / CLIPBOARD",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan
                                    )
                                    Surface(
                                        color = StudioSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                            if (clip.isNotBlank()) {
                                                customInputText = clip
                                                translateAndShow(clip, "Bộ nhớ tạm (Clipboard)")
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Dán & Dịch ngay",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricCyan
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customInputText,
                                    onValueChange = { customInputText = it },
                                    label = { Text("Văn bản ngoại ngữ từ Web / Ứng dụng khác") },
                                    minLines = 2,
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = StudioCardBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cross_app_text_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { translateAndShow(customInputText, "Dịch trực tiếp") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricCyan,
                                            contentColor = ObsidianBg
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("translate_now_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Dịch sang Tiếng Việt", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onSendToSubtitleTimeline(liveCaptionBanner) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = KineticLime,
                                            contentColor = ObsidianBg
                                        ),
                                        modifier = Modifier.testTag("send_translation_to_timeline_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đưa vào Video", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // History of Real-Time Translated Cues
                    item {
                        Text(
                            text = "LỊCH SỬ DỊCH TRỰC TIẾP (${translationHistory.size})",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    items(translationHistory, key = { it.id }) { item ->
                        Surface(
                            color = Color(0xFF171B24),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.sourceOrigin} • ${item.sourceLangLabel} → ${item.targetLangLabel}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = ElectricCyan
                                    )
                                    Surface(
                                        color = ElectricCyan.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable {
                                            onSendToSubtitleTimeline(item.translatedText)
                                        }
                                    ) {
                                        Text(
                                            text = "+ Thêm vào Timeline",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.sourceText,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🇻🇳 ${item.translatedText}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KineticLime
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }
            }

            // Bottom Floating Real-Time Vietsub Overlay + Exact Black Pill Bar from Image 2 (`photo_8`)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Translated Subtitle Overlay Box
                Surface(
                    color = Color(0xEB0B0D12),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ElectricCyan.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "VIETSUB TRỰC TIẾP",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                if (isTranslatingNow) {
                                    CircularProgressIndicator(
                                        color = KineticLime,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = liveCaptionBanner,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KineticLime,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onSendToSubtitleTimeline(liveCaptionBanner) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(ElectricCyan.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Đưa câu dịch vào Timeline",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Exact Black Pill Bar from Image 2 (photo_8)
                CapCutFloatingBlackPillBar(
                    onAutoCaptureTranslate = { triggerFullPageWebScan() },
                    onRegionOrClipboardTranslate = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        if (clip.isNotBlank()) {
                            translateAndShow(clip, "Vùng chọn / Clipboard")
                        } else {
                            triggerFullPageWebScan()
                        }
                    },
                    onShareOrSendToTimeline = {
                        onSendToSubtitleTimeline(liveCaptionBanner)
                    }
                )
            }
        }
    }
}

/**
 * Reproduces the exact floating black pill bar with white double border and glowing cyan atom badge
 * shown in uploaded Image 2 (`photo_8`).
 */
@Composable
fun CapCutFloatingBlackPillBar(
    onAutoCaptureTranslate: () -> Unit,
    onRegionOrClipboardTranslate: () -> Unit,
    onShareOrSendToTimeline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Glowing Cyan Atom Circle Button above the left edge of the pill (from Image 2, 3, 4, 7)
        Box(
            modifier = Modifier
                .padding(start = 6.dp, bottom = 4.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1B3A42),
                            Color(0xFF1F242B)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(onClick = onAutoCaptureTranslate)
                .testTag("floating_atom_ai_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Dịch tự động",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
            )
        }

        // Black Pill Bar with White Double Border (Image 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF050608))
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp)
                .testTag("floating_black_pill_bar")
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left circular thumbnail preview inside pill
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF7A45), Color(0xFF9D4EDD))
                            )
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable(onClick = onAutoCaptureTranslate),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SUB",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Center-Left Icon: [︾] Scroll / Live Auto-Sub Capture
                IconButton(
                    onClick = onAutoCaptureTranslate,
                    modifier = Modifier.testTag("pill_auto_capture_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowDown,
                        contentDescription = "Quét & dịch trực tiếp trang web",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center-Right Icon: [✂✎] Crop / Region / Clipboard Translate
                IconButton(
                    onClick = onRegionOrClipboardTranslate,
                    modifier = Modifier.testTag("pill_region_translate_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Dịch vùng chọn hoặc Clipboard",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Right Icon: [< Share / Send to Timeline]
                IconButton(
                    onClick = onShareOrSendToTimeline,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("pill_share_timeline_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Chuyển bản dịch vào Timeline",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}
