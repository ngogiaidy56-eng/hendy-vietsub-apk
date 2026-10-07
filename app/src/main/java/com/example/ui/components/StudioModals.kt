package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AspectRatioMode
import com.example.data.CanvasBackdrop
import com.example.data.CaptionSegmentEntity
import com.example.data.ScriptCadenceMode
import com.example.data.formatTimecodeShort
import com.example.ui.theme.ElectricCyan
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
import kotlin.math.max

@Composable
fun EditCaptionDialog(
    segment: CaptionSegmentEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, Long) -> Unit
) {
    var primaryText by remember(segment.id) { mutableStateOf(segment.text) }
    var secondaryText by remember(segment.id) { mutableStateOf(segment.secondaryText) }
    var speakerTag by remember(segment.id) { mutableStateOf(segment.speakerTag) }
    var startMs by remember(segment.id) { mutableLongStateOf(segment.startMs) }
    var endMs by remember(segment.id) { mutableLongStateOf(segment.endMs) }

    val durationSec = max(0.3f, (endMs - startMs) / 1000f)
    val liveCps = ((primaryText.length / durationSec) * 10).toInt() / 10f

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chỉnh Sửa Câu Phụ Đề",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (liveCps > 20f) HotCoral.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$liveCps CPS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (liveCps > 20f) HotCoral else ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = primaryText,
                    onValueChange = { primaryText = it },
                    label = { Text("Nội dung phụ đề chính") },
                    colors = studioTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_caption_primary_input")
                )

                OutlinedTextField(
                    value = secondaryText,
                    onValueChange = { secondaryText = it },
                    label = { Text("Phụ đề phụ / Bản dịch song ngữ (Tùy chọn)") },
                    colors = studioTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_caption_secondary_input")
                )

                OutlinedTextField(
                    value = speakerTag,
                    onValueChange = { speakerTag = it },
                    label = { Text("Tên người nói") },
                    singleLine = true,
                    colors = studioTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeStepperBox(
                        label = "BẮT ĐẦU",
                        timeMs = startMs,
                        onDelta = { d -> startMs = max(0L, startMs + d) }
                    )
                    TimeStepperBox(
                        label = "KẾT THÚC",
                        timeMs = endMs,
                        onDelta = { d -> endMs = max(startMs + 300L, endMs + d) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(primaryText, secondaryText, speakerTag, startMs, endMs)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("save_caption_edit_btn")
            ) {
                Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TimeStepperBox(
    label: String,
    timeMs: Long,
    onDelta: (Long) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(StudioSurfaceElevated, RoundedCornerShape(8.dp))
            .border(1.dp, StudioCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = TextMuted
        )
        Text(
            text = formatTimecodeShort(timeMs),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { onDelta(-100L) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("-0.1s", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextSecondary)
            }
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { onDelta(100L) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("+0.1s", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SmartScriptSyncDialog(
    onDismiss: () -> Unit,
    onConfirmSync: (String, ScriptCadenceMode, Boolean, Boolean) -> Unit
) {
    var scriptText by remember {
        mutableStateOf(
            "Bạn muốn video ngắn thu hút gấp 3 lần? Hãy thêm hiệu ứng phụ đề Karaoke nhảy chữ theo từng nhịp giọng đọc! Khán giả sẽ theo dõi trọn vẹn từng giây."
        )
    }
    var selectedCadence by remember { mutableStateOf(ScriptCadenceMode.VIRAL_FAST) }
    var appendToTimeline by remember { mutableStateOf(false) }
    var autoHighlightKeywords by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricCyan
                )
                Text(
                    text = "Tự Động Chia & Căn Giờ Kịch Bản",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Dán lời thoại hoặc kịch bản thu âm vào bên dưới. Hendy Vietsub sẽ tự động tách câu, tính toán tốc độ đọc (CPS) và gắn mốc Karaoke cho từng từ.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = scriptText,
                    onValueChange = { scriptText = it },
                    label = { Text("Nội dung kịch bản / lời thoại") },
                    minLines = 3,
                    maxLines = 5,
                    colors = studioTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_script_input")
                )

                Text(
                    text = "NHỊP ĐỘ NGẮT CÂU",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScriptCadenceMode.entries.forEach { mode ->
                        val isSelected = selectedCadence == mode
                        Surface(
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ElectricCyan else StudioCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCadence = mode }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ElectricCyan else TextPrimary
                                )
                                Text(
                                    text = "${mode.wordsPerMinute} từ/phút",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = KineticLime
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { autoHighlightKeywords = !autoHighlightKeywords }
                ) {
                    Checkbox(
                        checked = autoHighlightKeywords,
                        onCheckedChange = { autoHighlightKeywords = it },
                        colors = CheckboxDefaults.colors(checkedColor = KineticLime, checkmarkColor = ObsidianBg)
                    )
                    Text(
                        text = "Tự động nhấn mạnh con số & từ khóa quan trọng",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { appendToTimeline = !appendToTimeline }
                ) {
                    Checkbox(
                        checked = appendToTimeline,
                        onCheckedChange = { appendToTimeline = it },
                        colors = CheckboxDefaults.colors(checkedColor = ElectricCyan, checkmarkColor = ObsidianBg)
                    )
                    Text(
                        text = "Nối tiếp vào sau các câu phụ đề hiện có",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSync(scriptText, selectedCadence, appendToTimeline, autoHighlightKeywords)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("confirm_smart_script_btn")
            ) {
                Text("Tạo Phụ Đề Tự Động", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        }
    )
}

@Composable
fun LiveVoiceDictationDialog(
    playheadMs: Long,
    onDismiss: () -> Unit,
    onInsertDictatedText: (String) -> Unit
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Chạm vào biểu tượng Micro để chuyển giọng nói thành phụ đề") }
    var rmsDbLevel by remember { mutableFloatStateOf(0f) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusText = "Đang lắng nghe... Hãy nói rõ vào micro"
            }

            override fun onBeginningOfSpeech() {
                statusText = "Đang thu nhận giọng nói..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsDbLevel = rmsdB.coerceIn(0f, 12f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                statusText = "Đang xử lý văn bản..."
            }

            override fun onError(error: Int) {
                isListening = false
                statusText = "Micro đang nghỉ hoặc không khả dụng trên máy ảo — bạn có thể nhập bên dưới"
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val top = matches?.firstOrNull().orEmpty()
                if (top.isNotBlank()) {
                    recognizedText = if (recognizedText.isBlank()) top else "$recognizedText $top"
                    statusText = "Đã nhận diện giọng nói! Sẵn sàng chèn tại ${formatTimecodeShort(playheadMs)}"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    recognizedText = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            runCatching { speechRecognizer?.destroy() }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            statusText = "Chưa cấp quyền Micro — bạn vẫn có thể nhập hoặc dán lời thoại bên dưới"
        }
    }

    fun toggleListening() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (speechRecognizer == null) {
            statusText = "Thiết bị này chưa cài đặt dịch vụ nhận diện giọng nói"
            return
        }
        if (isListening) {
            runCatching { speechRecognizer.stopListening() }
            isListening = false
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            runCatching {
                speechRecognizer.startListening(intent)
                isListening = true
            }.onFailure {
                statusText = "Không thể khởi động micro: ${it.localizedMessage}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = HotCoral
                )
                Text(
                    text = "Tạo Phụ Đề Bằng Giọng Nói",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = if (isListening) KineticLime else TextSecondary
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isListening) HotCoral else StudioSurfaceElevated)
                        .border(
                            width = 2.dp,
                            color = if (isListening) KineticLime else HotCoral,
                            shape = CircleShape
                        )
                        .clickable { toggleListening() }
                        .testTag("voice_mic_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Bật hoặc tắt thu âm giọng nói",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (isListening) {
                    Text(
                        text = "Âm lượng thu: ${(rmsDbLevel * 10).toInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                }

                OutlinedTextField(
                    value = recognizedText,
                    onValueChange = { recognizedText = it },
                    label = { Text("Văn bản giọng nói (có thể chỉnh sửa)") },
                    placeholder = { Text("Nói vào micro hoặc nhập câu thoại để chèn tại kim phát...") },
                    minLines = 2,
                    maxLines = 4,
                    colors = studioTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_transcript_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recognizedText.isNotBlank()) {
                        onInsertDictatedText(recognizedText)
                        onDismiss()
                    }
                },
                enabled = recognizedText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("insert_voice_caption_btn")
            ) {
                Text("Chèn tại ${formatTimecodeShort(playheadMs)}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SaveCustomPresetDialog(
    onDismiss: () -> Unit,
    onSavePreset: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("Phong Cách Vietsub Mới") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Text("Lưu Mẫu Kiểu Chữ Cá Nhân", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Lưu lại phông chữ, màu viền, nền hộp chữ và hiệu ứng Karaoke hiện tại để áp dụng nhanh cho mọi dự án sau này.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Tên mẫu kiểu chữ") },
                    singleLine = true,
                    colors = studioTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePreset(presetName)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_save_preset_btn")
            ) {
                Text("Lưu mẫu", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        }
    )
}

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (String, AspectRatioMode, CanvasBackdrop) -> Unit
) {
    var title by remember { mutableStateOf("Video Ngắn Vietsub #1") }
    var selectedRatio by remember { mutableStateOf(AspectRatioMode.RATIO_9_16) }
    var selectedBackdrop by remember { mutableStateOf(CanvasBackdrop.CYBER_GRID) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Text("Tạo Dự Án Phụ Đề Mới", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên dự án") },
                    singleLine = true,
                    colors = studioTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input")
                )

                Text(
                    text = "TỶ LỆ KHUNG HÌNH",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AspectRatioMode.entries.forEach { ratio ->
                        val isSelected = ratio == selectedRatio
                        Surface(
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else StudioCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedRatio = ratio }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = ratio.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ElectricCyan else TextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateProject(title, selectedRatio, selectedBackdrop)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_create_project_btn")
            ) {
                Text("Tạo dự án", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun studioTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = ElectricCyan,
    unfocusedBorderColor = StudioCardBorder,
    focusedLabelColor = ElectricCyan,
    unfocusedLabelColor = TextSecondary,
    cursorColor = ElectricCyan
)

/**
 * Reproduces the exact "Phụ đề tự động" bottom sheet from uploaded Image 7 (`photo_1`).
 */
@Composable
fun CapCutAutoCaptionsSheet(
    presets: List<com.example.data.StylePresetEntity>,
    onDismiss: () -> Unit,
    onGenerateCaptions: (
        sourceType: String,
        spokenLang: com.example.data.TranslationLanguage,
        selectedPreset: com.example.data.StylePresetEntity?,
        customScriptOrTranslate: String,
        translateToVietnamese: Boolean
    ) -> Unit,
    onOpenVoiceRecognizer: () -> Unit,
    onOpenLiveWebTranslate: () -> Unit
) {
    val sources = listOf("Video", "Âm thanh Micro", "Website trực tiếp", "Ứng dụng khác")
    var sourceIndex by remember { mutableStateOf(0) }
    val languages = com.example.data.TranslationLanguage.entries
    var langIndex by remember { mutableStateOf(0) }
    var selectedPresetIndex by remember { mutableStateOf(0) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var translateToVietnamese by remember { mutableStateOf(true) }
    var scriptInput by remember {
        mutableStateOf(
            "Stop scrolling if you edit short videos! 90% of viewers watch on mute with captions. Word-by-word pop keeps retention at 100%."
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18191E),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Phụ đề tự động",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = "✕",
                    fontSize = 18.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = onDismiss)
                        .padding(4.dp)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Tạo từ -> Video >
                Surface(
                    color = Color(0xFF24252B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sourceIndex = (sourceIndex + 1) % sources.size }
                        .testTag("auto_cap_source_row")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Tạo từ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${sources[sourceIndex]} >",
                            fontSize = 13.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Row 2: Ngôn ngữ nói -> Tự động phát hiện >
                Surface(
                    color = Color(0xFF24252B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { langIndex = (langIndex + 1) % languages.size }
                        .testTag("auto_cap_lang_row")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "文A",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ngôn ngữ nói",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${languages[langIndex].displayName} >",
                            fontSize = 13.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Card 3: [CC] Mẫu (Mặc định, THE QUICK, brown fox, sống nhé, The quick brown fox)
                Surface(
                    color = Color(0xFF24252B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.border(1.2.dp, Color.White, RoundedCornerShape(4.dp))
                                ) {
                                    Text(
                                        text = "CC",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = "Mẫu",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Text(text = ">", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val templateLabels = listOf(
                            Triple("Mặc định", "#FFFFFF", "#FFFFFF"),
                            Triple("THE QUICK", "#FFFFFF", "#76FF03"),
                            Triple("brown fox", "#FFFFFF", "#00E676"),
                            Triple("sống nhé", "#CCFF00", "#00E5FF"),
                            Triple("The quick", "#E5E7EB", "#FFFFFF")
                        )

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(templateLabels.size) { idx ->
                                val (label, c1, c2) = templateLabels[idx]
                                val isSelected = selectedPresetIndex == idx
                                Box(
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF2F3138))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color(0xFF3E4049),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedPresetIndex = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (idx == 3) {
                                        // Cyan diamond badge on top-right like "sống nhé" in Image 7
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF))
                                        )
                                    }
                                    Text(
                                        text = label,
                                        fontSize = if (idx == 4) 10.sp else 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (idx == 1 || idx == 3) Color(0xFF9EFF00) else Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 4: 💎 Tùy chọn nâng cao v
                Surface(
                    color = Color(0xFF24252B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedOptions = !showAdvancedOptions }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "💎", fontSize = 13.sp)
                                Text(
                                    text = "Tùy chọn nâng cao",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB388FF)
                                )
                            }
                            Text(
                                text = if (showAdvancedOptions) "⌃" else "⌄",
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }

                        if (showAdvancedOptions) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { translateToVietnamese = !translateToVietnamese }
                            ) {
                                Checkbox(
                                    checked = translateToVietnamese,
                                    onCheckedChange = { translateToVietnamese = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ElectricCyan,
                                        checkmarkColor = ObsidianBg
                                    )
                                )
                                Text(
                                    text = "Tự động dịch song ngữ sang Tiếng Việt (Vietsub)",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            OutlinedTextField(
                                value = scriptInput,
                                onValueChange = { scriptInput = it },
                                label = { Text("Lời thoại / Văn bản cần tạo phụ đề & dịch") },
                                minLines = 2,
                                maxLines = 3,
                                colors = studioTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Full-width bright cyan "Tạo" button with top-right badge (Image 7)
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        when (sourceIndex) {
                            1 -> {
                                onDismiss()
                                onOpenVoiceRecognizer()
                            }
                            2, 3 -> {
                                onDismiss()
                                onOpenLiveWebTranslate()
                            }
                            else -> {
                                val chosenPreset = presets.getOrNull(selectedPresetIndex)
                                onGenerateCaptions(
                                    sources[sourceIndex],
                                    languages[langIndex],
                                    chosenPreset,
                                    scriptInput,
                                    translateToVietnamese
                                )
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D8F6),
                        contentColor = Color(0xFF101216)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auto_captions_create_btn")
                ) {
                    Text(
                        text = "Tạo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = Color(0xFF2B2D35),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        text = "Không giới hạn",
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    )
}
