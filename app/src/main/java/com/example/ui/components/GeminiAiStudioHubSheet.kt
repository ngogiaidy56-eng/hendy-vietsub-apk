package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ChatTurnMessage
import com.example.data.ChatbotRolePersona
import com.example.data.GeminiAiRepository
import com.example.data.GeminiChatModelTier
import com.example.data.GeneratedImageResult
import com.example.data.VeoVideoGenerationResult
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KineticLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun GeminiAiStudioHubDialog(
    initialTab: Int = 0, // 0 = Transcribe Audio, 1 = AI Chatbot, 2 = Image Gen/Edit, 3 = Veo 3 Video
    hasUploadedVideo: Boolean,
    isTranscribingAudio: Boolean,
    onDismiss: () -> Unit,
    onTranscribeUploadedVideoAudio: (spokenLangHint: String, translateToVi: Boolean, append: Boolean) -> Unit,
    onPickVideoToTranscribe: () -> Unit,
    onInsertGeneratedScriptToTimeline: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    // Tab 0 State: Gemini 3.5 Audio Transcription
    var spokenLangHint by remember { mutableStateOf("Tự động phát hiện (Anh / Việt / Trung / Hàn / Nhật)") }
    var translateToVietnamese by remember { mutableStateOf(true) }
    var appendToTimeline by remember { mutableStateOf(false) }

    // Tab 1 State: Multi-Model Gemini Chatbot (`gemini-3.1-pro-preview`, `gemini-3.5-flash`, `gemini-3.1-flash-lite-preview`)
    var selectedModelTier by remember { mutableStateOf(GeminiChatModelTier.GENERAL_FLASH) }
    var selectedPersona by remember { mutableStateOf(ChatbotRolePersona.SUBTITLE_DIRECTOR) }
    var chatInput by remember { mutableStateOf("") }
    var isChatLoading by remember { mutableStateOf(false) }
    var chatHistory by remember {
        mutableStateOf(
            listOf(
                ChatTurnMessage(
                    isUser = false,
                    text = "Xin chào! Tôi là Trợ lý AI Hendy Vietsub hỗ trợ bởi Gemini 3.5 Flash, Gemini 3.1 Pro và Flash Lite. Bạn cần viết câu mở đầu (hook), dịch kịch bản đa ngôn ngữ hay căn chỉnh sóng âm?",
                    modelUsed = GeminiChatModelTier.GENERAL_FLASH.modelId
                )
            )
        )
    }

    // Tab 2 State: Image Creation & Editing (`gemini-3.1-flash-image-preview`)
    var imagePrompt by remember {
        mutableStateOf("Poster điện ảnh Cyberpunk cho video ngắn, ánh sáng neon xanh cyan, phụ đề nổi bật")
    }
    var imageAspectRatio by remember { mutableStateOf("9:16") }
    var uploadedSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedImageResult by remember { mutableStateOf<GeneratedImageResult?>(null) }
    var isGeneratingImage by remember { mutableStateOf(false) }

    // Tab 3 State: Veo 3 Video Generation & Photo Animation (`veo-3.1-fast-generate-preview`)
    var veoPrompt by remember {
        mutableStateOf("Chuyển động máy quay điện ảnh mượt mà, ánh sáng studio lung linh, nhân vật đang trò chuyện tự nhiên")
    }
    var veoAspectRatio by remember { mutableStateOf("9:16") } // Strictly "9:16" or "16:9"
    var veoSourcePhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var veoResult by remember { mutableStateOf<VeoVideoGenerationResult?>(null) }
    var isGeneratingVeo by remember { mutableStateOf(false) }

    // Photo picker for Tab 2 (Image Edit)
    val imageEditPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    uploadedSourceBitmap = BitmapFactory.decodeStream(stream)
                }
            }
        }
    }

    // Photo picker for Tab 3 (Veo Photo-to-Video Animation)
    val veoPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    veoSourcePhotoBitmap = BitmapFactory.decodeStream(stream)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianBg,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 18.dp)
                .border(1.dp, StudioCardBorder, RoundedCornerShape(22.dp))
                .testTag("gemini_ai_studio_hub_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurfaceElevated)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Trung Tâm AI Gemini & Veo 3",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Nhận diện giọng nói • Chatbot • Ảnh AI • Video Veo 3.1",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color.White
                        )
                    }
                }

                // 4 Feature Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "Nhận diện Giọng nói", Icons.Default.RecordVoiceOver),
                        Triple(1, "Trợ lý Chatbot AI", Icons.Default.SmartToy),
                        Triple(2, "Tạo & Sửa Ảnh AI", Icons.Default.Image),
                        Triple(3, "Tạo Video Veo 3", Icons.Default.MovieCreation)
                    )
                    tabs.forEach { (idx, title, icon) ->
                        val active = selectedTab == idx
                        Surface(
                            color = if (active) ElectricCyan else StudioSurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .clickable { selectedTab = idx }
                                .testTag("gemini_hub_tab_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (active) ObsidianBg else ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) ObsidianBg else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Body Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // TAB 0: AI Voice Recognition with Timestamps (`gemini-3.5-transcribe`)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = StudioSurfaceElevated,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = null,
                                                tint = KineticLime,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Tự động Nhận diện Giọng nói từ Video (Gemini 3.5 Transcribe)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Trích xuất rãnh âm thanh (audio track) từ video tải lên, nhận diện lời thoại kèm mốc thời gian chính xác (startMs → endMs) bằng model `gemini-3.5-transcribe` và tự động khớp vào sóng âm.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                // Video source status + upload button
                                Surface(
                                    color = StudioSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (hasUploadedVideo) "✅ Đã tải lên Video từ thiết bị" else "📹 Sử dụng âm thanh Video hiện tại / Tải Video mới",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasUploadedVideo) KineticLime else TextPrimary
                                            )
                                            Text(
                                                text = "Model: gemini-3.5-transcribe • Đầu ra JSON Timestamped Cues",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = ElectricCyan
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = onPickVideoToTranscribe,
                                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                                brush = SolidColor(ElectricCyan)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.UploadFile,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Chọn Video", fontSize = 11.sp, color = ElectricCyan)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = spokenLangHint,
                                    onValueChange = { spokenLangHint = it },
                                    label = { Text("Ngôn ngữ giọng nói trong video") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = StudioCardBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Dịch tự động sang Tiếng Việt (Vietsub Song ngữ)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Giữ câu gốc ở dòng phụ đề thứ 2",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = translateToVietnamese,
                                        onCheckedChange = { translateToVietnamese = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ObsidianBg,
                                            checkedTrackColor = ElectricCyan
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Nối tiếp vào các câu phụ đề hiện có",
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Switch(
                                        checked = appendToTimeline,
                                        onCheckedChange = { appendToTimeline = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ObsidianBg,
                                            checkedTrackColor = KineticLime
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        onTranscribeUploadedVideoAudio(
                                            spokenLangHint,
                                            translateToVietnamese,
                                            appendToTimeline
                                        )
                                        onDismiss()
                                    },
                                    enabled = !isTranscribingAudio,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = ObsidianBg
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("start_gemini_transcribe_btn")
                                ) {
                                    if (isTranscribingAudio) {
                                        CircularProgressIndicator(
                                            color = ObsidianBg,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Đang nhận diện giọng nói bằng Gemini 3.5...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tự động Tách Phụ Đề & Timestamp Ngay",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: Multi-Model AI Chatbot (`gemini-3.1-pro-preview`, `gemini-3.5-flash`, `gemini-3.1-flash-lite-preview`)
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Model Tier Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GeminiChatModelTier.entries.forEach { tier ->
                                        val selected = selectedModelTier == tier
                                        Surface(
                                            color = if (selected) ElectricCyan.copy(alpha = 0.2f) else StudioSurfaceElevated,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (selected) ElectricCyan else StudioCardBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedModelTier = tier }
                                                .testTag("chat_model_${tier.name}")
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = tier.displayName,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (selected) ElectricCyan else Color.White
                                                    )
                                                    Text(
                                                        text = tier.badge,
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontSize = 9.sp,
                                                        color = KineticLime
                                                    )
                                                }
                                                Text(
                                                    text = tier.modelId,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 9.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Persona Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ChatbotRolePersona.entries.forEach { persona ->
                                        val active = selectedPersona == persona
                                        Surface(
                                            color = if (active) KineticLime.copy(alpha = 0.18f) else StudioSurface,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (active) KineticLime else StudioCardBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selectedPersona = persona }
                                        ) {
                                            Text(
                                                text = persona.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                color = if (active) KineticLime else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Chat Messages List
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(StudioSurface, RoundedCornerShape(12.dp))
                                        .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatHistory, key = { it.id }) { msg ->
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                                        ) {
                                            Surface(
                                                color = if (msg.isUser) ElectricCyan.copy(alpha = 0.22f) else StudioSurfaceElevated,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.border(
                                                    1.dp,
                                                    if (msg.isUser) ElectricCyan else StudioCardBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    if (!msg.isUser && msg.modelUsed.isNotBlank()) {
                                                        Text(
                                                            text = "🤖 ${msg.modelUsed}",
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            fontSize = 9.sp,
                                                            color = ElectricCyan
                                                        )
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                    }
                                                    Text(
                                                        text = msg.text,
                                                        fontSize = 12.sp,
                                                        color = Color.White
                                                    )
                                                    if (!msg.isUser) {
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = "+ Đưa gợi ý này vào Timeline",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = KineticLime,
                                                            modifier = Modifier.clickable {
                                                                onInsertGeneratedScriptToTimeline(msg.text)
                                                                onDismiss()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Input Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = chatInput,
                                        onValueChange = { chatInput = it },
                                        placeholder = { Text("Hỏi Gemini viết hook, dịch kịch bản...", fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = ElectricCyan,
                                            unfocusedBorderColor = StudioCardBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("gemini_chat_input")
                                    )

                                    Button(
                                        onClick = {
                                            val prompt = chatInput.trim()
                                            if (prompt.isNotEmpty() && !isChatLoading) {
                                                val userMsg = ChatTurnMessage(isUser = true, text = prompt)
                                                val updatedHistory = chatHistory + userMsg
                                                chatHistory = updatedHistory
                                                chatInput = ""
                                                isChatLoading = true
                                                scope.launch {
                                                    val reply = GeminiAiRepository.sendMultiTurnChatMessage(
                                                        conversationHistory = updatedHistory,
                                                        newUserMessage = prompt,
                                                        modelTier = selectedModelTier,
                                                        rolePersona = selectedPersona
                                                    )
                                                    chatHistory = chatHistory + reply
                                                    isChatLoading = false
                                                }
                                            }
                                        },
                                        enabled = !isChatLoading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricCyan,
                                            contentColor = ObsidianBg
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("gemini_chat_send_btn")
                                    ) {
                                        if (isChatLoading) {
                                            CircularProgressIndicator(
                                                color = ObsidianBg,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi")
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // TAB 2: Create & Edit Images (`gemini-3.1-flash-image-preview`)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Tạo & Chỉnh Sửa Ảnh Minh Họa (gemini-3.1-flash-image-preview)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            imageEditPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                            brush = SolidColor(ElectricCyan)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (uploadedSourceBitmap != null) "Đổi ảnh gốc khác" else "Tải ảnh lên để chỉnh sửa",
                                            fontSize = 11.sp,
                                            color = ElectricCyan
                                        )
                                    }

                                    listOf("9:16", "16:9", "1:1").forEach { ratio ->
                                        val active = imageAspectRatio == ratio
                                        Surface(
                                            color = if (active) ElectricCyan else StudioSurfaceElevated,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.clickable { imageAspectRatio = ratio }
                                        ) {
                                            Text(
                                                text = ratio,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) ObsidianBg else Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = imagePrompt,
                                    onValueChange = { imagePrompt = it },
                                    label = { Text("Mô tả ảnh cần tạo hoặc chỉnh sửa") },
                                    minLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = StudioCardBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        isGeneratingImage = true
                                        scope.launch {
                                            generatedImageResult = GeminiAiRepository.generateOrEditImageWithGemini(
                                                prompt = imagePrompt,
                                                sourceBitmap = uploadedSourceBitmap,
                                                aspectRatio = imageAspectRatio
                                            )
                                            isGeneratingImage = false
                                        }
                                    },
                                    enabled = !isGeneratingImage,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("generate_gemini_image_btn")
                                ) {
                                    Text(
                                        text = if (isGeneratingImage) "Đang kết xuất bằng gemini-3.1-flash-image-preview..."
                                        else if (uploadedSourceBitmap != null) "Chỉnh sửa ảnh bằng Gemini 3.1 Flash Image"
                                        else "Tạo ảnh mới bằng Gemini 3.1 Flash Image",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                generatedImageResult?.let { res ->
                                    Surface(
                                        color = StudioSurfaceElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, KineticLime, RoundedCornerShape(14.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            res.bitmap?.let { bmp ->
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Generated AI Artwork",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(190.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = res.captionText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KineticLime
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = {
                                                    veoSourcePhotoBitmap = res.bitmap
                                                    selectedTab = 3
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = KineticLime,
                                                    contentColor = ObsidianBg
                                                )
                                            ) {
                                                Text("Chuyển ảnh này sang Veo 3 để tạo Video động →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // TAB 3: Veo 3 Video Generation & Photo Animation (`veo-3.1-fast-generate-preview`)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Tạo Video & Chuyển Động Hóa Ảnh Bằng Veo 3 (veo-3.1-fast-generate-preview)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tải ảnh lên để làm sống động thành video (Image-to-Video) hoặc nhập mô tả văn bản để tạo video 1080p.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            veoPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                            brush = SolidColor(KineticLime)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("veo_upload_photo_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = KineticLime, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (veoSourcePhotoBitmap != null) "Đã chọn ảnh gốc • Đổi ảnh" else "Tải ảnh lên (Animate Photo)",
                                            fontSize = 11.sp,
                                            color = KineticLime
                                        )
                                    }

                                    // Aspect ratio strictly 9:16 (portrait) or 16:9 (landscape)
                                    listOf("9:16" to "9:16 Dọc", "16:9" to "16:9 Ngang").forEach { (ratio, label) ->
                                        val active = veoAspectRatio == ratio
                                        Surface(
                                            color = if (active) ElectricCyan else StudioSurfaceElevated,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .clickable { veoAspectRatio = ratio }
                                                .testTag("veo_ratio_$ratio")
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) ObsidianBg else Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = veoPrompt,
                                    onValueChange = { veoPrompt = it },
                                    label = { Text("Mô tả chuyển động video Veo 3.1") },
                                    minLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = StudioCardBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        isGeneratingVeo = true
                                        scope.launch {
                                            veoResult = GeminiAiRepository.generateVeoVideoFromPromptOrPhoto(
                                                prompt = veoPrompt,
                                                uploadedPhotoBitmap = veoSourcePhotoBitmap,
                                                aspectRatio = veoAspectRatio
                                            )
                                            isGeneratingVeo = false
                                        }
                                    },
                                    enabled = !isGeneratingVeo,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KineticLime,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("generate_veo_video_btn")
                                ) {
                                    Text(
                                        text = if (isGeneratingVeo) "Đang tạo video bằng veo-3.1-fast-generate-preview..."
                                        else "Tạo Video Veo 3.1 ($veoAspectRatio)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                veoResult?.let { result ->
                                    Surface(
                                        color = StudioSurfaceElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, ElectricCyan, RoundedCornerShape(14.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "🎬 ${result.statusSummary}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KineticLime
                                            )
                                            Text(
                                                text = "Operation: ${result.operationName}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = ElectricCyan
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    val combinedScript = result.generatedSubtitles.joinToString("\n") { it.text }
                                                    onInsertGeneratedScriptToTimeline(combinedScript)
                                                    onDismiss()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ElectricCyan,
                                                    contentColor = ObsidianBg
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Đưa cảnh & phụ đề Veo 3 vào Timeline", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
