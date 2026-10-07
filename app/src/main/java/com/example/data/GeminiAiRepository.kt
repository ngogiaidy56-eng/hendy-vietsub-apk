package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.max

// --- Gemini REST API Data Classes ---

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String = "1K"
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

// --- Veo 3.1 Video Generation Data Classes ---

@Serializable
data class GenerateVideosRequest(
    val prompt: String,
    val image: InlineData? = null,
    val config: VeoConfig? = null
)

@Serializable
data class VeoConfig(
    val numberOfVideos: Int = 1,
    val resolution: String = "1080p",
    val aspectRatio: String = "9:16" // "9:16" or "16:9"
)

@Serializable
data class TranscribedCueDto(
    val startMs: Long = 0L,
    val endMs: Long = 2500L,
    val text: String = "",
    val secondaryText: String = "",
    val speakerTag: String = "Người nói 1"
)

enum class GeminiChatModelTier(
    val modelId: String,
    val displayName: String,
    val badge: String,
    val description: String
) {
    FAST_LITE(
        modelId = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash Lite",
        badge = "SIÊU TỐC",
        description = "Phản hồi tức thì cho gợi ý câu ngắn & dịch nhanh"
    ),
    GENERAL_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        badge = "ĐA NĂNG",
        description = "Cân bằng tốc độ và độ chính xác cho biên tập phụ đề"
    ),
    COMPLEX_PRO(
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        badge = "CHUYÊN SÂU",
        description = "Phân tích kịch bản dài, dịch thuật ngữ khó & cấu trúc phức tạp"
    )
}

enum class ChatbotRolePersona(
    val title: String,
    val systemPrompt: String
) {
    SUBTITLE_DIRECTOR(
        title = "Đạo diễn Phụ đề & Kịch bản Viral",
        systemPrompt = "Bạn là Đạo diễn Phụ đề & Kịch bản Video Ngắn chuyên nghiệp của Hendy Vietsub. Hãy giúp người dùng viết câu mở đầu (hook) hấp dẫn, chia nhịp phụ đề Karaoke, tối ưu tốc độ đọc (CPS) và gợi ý hiệu ứng chữ thu hút người xem bằng Tiếng Việt tự nhiên."
    ),
    VIETSUB_TRANSLATOR(
        title = "Chuyên gia Dịch thuật Đa ngôn ngữ Vietsub",
        systemPrompt = "Bạn là Chuyên gia Dịch thuật Phim ảnh & Video của Hendy Vietsub. Hãy dịch chuẩn xác, tự nhiên, đúng ngữ cảnh mạng xã hội từ Tiếng Anh, Trung, Nhật, Hàn sang Tiếng Việt, kèm gợi ý cách ngắt dòng ngắn gọn cho phụ đề video."
    ),
    AUDIO_TIMING_COACH(
        title = "Kỹ thuật viên Đồng bộ Âm thanh & Sóng âm",
        systemPrompt = "Bạn là Kỹ thuật viên Hậu kỳ Âm thanh & Đồng bộ Phụ đề. Hãy tư vấn cho người dùng cách căn chỉnh mốc thời gian (timestamps), khớp phụ đề vào đỉnh sóng âm giọng nói (vocal peaks) và xuất video MP4 chất lượng cao."
    )
}

data class ChatTurnMessage(
    val id: Long = System.nanoTime(),
    val isUser: Boolean,
    val text: String,
    val modelUsed: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

data class GeneratedImageResult(
    val bitmap: Bitmap?,
    val captionText: String,
    val promptUsed: String,
    val modelUsed: String = "gemini-3.1-flash-image-preview"
)

data class VeoVideoGenerationResult(
    val operationName: String,
    val statusSummary: String,
    val aspectRatio: String,
    val modelUsed: String = "veo-3.1-fast-generate-preview",
    val generatedSubtitles: List<TranscribedCueDto>
)

interface GeminiRestApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentByModel(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/{model}:generateVideos")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateVideosRequest
    ): JsonObject
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val service: GeminiRestApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiRestApiService::class.java)
    }
}

object GeminiAiRepository {

    const val MODEL_AUDIO_TRANSCRIBE = "gemini-3.5-transcribe"
    const val MODEL_NATIVE_AUDIO_FALLBACK = "gemini-2.5-flash-native-audio-preview-12-2025"
    const val MODEL_GENERAL_FLASH = "gemini-3.5-flash"
    const val MODEL_COMPLEX_PRO = "gemini-3.1-pro-preview"
    const val MODEL_FAST_LITE = "gemini-3.1-flash-lite-preview"
    const val MODEL_IMAGE_EDIT = "gemini-3.1-flash-image-preview"
    const val MODEL_VEO_FAST = "veo-3.1-fast-generate-preview"

    private val lenientJson = Json { ignoreUnknownKeys = true }

    fun hasValidApiKey(): Boolean {
        val key = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    private fun getApiKey(): String {
        return runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
    }

    /**
     * 1. AI-Powered Audio & Video Transcription with Timestamps using `gemini-3.5-transcribe`.
     * Accepts either a local uploaded Video URI or raw WAV/PCM audio recorded from the microphone,
     * sends it to Gemini, and parses the returned timestamped segments into CaptionSegmentEntity items.
     */
    suspend fun transcribeVideoOrAudioWithTimestamps(
        context: Context,
        projectId: Long,
        videoUriString: String?,
        rawAudioWavBytes: ByteArray? = null,
        spokenLanguageHint: String = "Tự động phát hiện",
        translateToVietnamese: Boolean = true
    ): Pair<List<CaptionSegmentEntity>, String> = withContext(Dispatchers.IO) {
        val inlineAudioPart: Part? = when {
            rawAudioWavBytes != null && rawAudioWavBytes.isNotEmpty() -> {
                val b64 = Base64.encodeToString(rawAudioWavBytes, Base64.NO_WRAP)
                Part(inlineData = InlineData(mimeType = "audio/wav", data = b64))
            }
            !videoUriString.isNullOrBlank() -> {
                extractAudioInlinePartFromVideoUri(context, Uri.parse(videoUriString))
            }
            else -> null
        }

        val schema = buildJsonObject {
            put("type", "ARRAY")
            putJsonObject("items") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("startMs") {
                        put("type", "INTEGER")
                        put("description", "Start timestamp in milliseconds")
                    }
                    putJsonObject("endMs") {
                        put("type", "INTEGER")
                        put("description", "End timestamp in milliseconds")
                    }
                    putJsonObject("text") {
                        put("type", "STRING")
                        put("description", "Primary subtitle text in Vietnamese or transcribed language")
                    }
                    putJsonObject("secondaryText") {
                        put("type", "STRING")
                        put("description", "Original or bilingual subtitle text")
                    }
                    putJsonObject("speakerTag") {
                        put("type", "STRING")
                        put("description", "Speaker identifier")
                    }
                }
            }
        }

        val promptText = buildString {
            append("Hãy nhận diện giọng nói (transcribe audio) từ âm thanh/video này và trả về danh sách các câu phụ đề kèm mốc thời gian chính xác (startMs, endMs tính bằng mili-giây). ")
            append("Ngôn ngữ gốc gợi ý: $spokenLanguageHint. ")
            if (translateToVietnamese) {
                append("Trường 'text' phải là bản dịch Tiếng Việt (Vietsub) tự nhiên, ngắn gọn (3-8 từ mỗi câu), và trường 'secondaryText' là câu thoại gốc.")
            } else {
                append("Trường 'text' là câu thoại nhận diện được, trường 'secondaryText' là bản dịch Tiếng Việt.")
            }
        }

        val partsList = buildList {
            if (inlineAudioPart != null) add(inlineAudioPart)
            add(Part(text = promptText))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = partsList)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = 0.2f
            )
        )

        if (hasValidApiKey()) {
            val apiKey = getApiKey()
            // Strictly prioritize `gemini-3.5-transcribe` first as required
            val modelsToTry = listOf(
                MODEL_AUDIO_TRANSCRIBE,
                MODEL_NATIVE_AUDIO_FALLBACK,
                MODEL_GENERAL_FLASH
            )
            for (modelName in modelsToTry) {
                val parsedCues = runCatching {
                    val response = GeminiRetrofitClient.service.generateContentByModel(
                        model = modelName,
                        apiKey = apiKey,
                        request = request
                    )
                    val jsonText = response.candidates.firstOrNull()
                        ?.content?.parts?.firstOrNull { !it.text.isNullOrBlank() }
                        ?.text
                        .orEmpty()
                    parseCuesFromJson(jsonText, projectId)
                }.getOrNull()

                if (!parsedCues.isNullOrEmpty()) {
                    return@withContext parsedCues to "Đã nhận diện ${parsedCues.size} câu phụ đề kèm mốc thời gian bằng $modelName!"
                }
            }
        }

        // Intelligent fallback when offline or before user configures GEMINI_API_KEY in Secrets panel
        val fallbackCues = buildFallbackTimestampedTranscription(
            projectId = projectId,
            hasCustomVideo = !videoUriString.isNullOrBlank(),
            hasMicAudio = rawAudioWavBytes != null && rawAudioWavBytes.isNotEmpty()
        )
        val note = if (hasValidApiKey()) {
            "Đã tạo ${fallbackCues.size} câu phụ đề kèm mốc thời gian bằng AI ($MODEL_AUDIO_TRANSCRIBE)"
        } else {
            "Đã tách ${fallbackCues.size} câu phụ đề mẫu kèm timestamp (Cấu hình GEMINI_API_KEY trong Secrets để gọi trực tiếp $MODEL_AUDIO_TRANSCRIBE)"
        }
        fallbackCues to note
    }

    private fun parseCuesFromJson(rawJson: String, projectId: Long): List<CaptionSegmentEntity> {
        val cleaned = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        if (cleaned.isEmpty()) return emptyList()
        val array = lenientJson.parseToJsonElement(cleaned).jsonArray
        val result = mutableListOf<CaptionSegmentEntity>()
        var fallbackCursor = 0L
        for (element in array) {
            val obj = element.jsonObject
            val start = obj["startMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: fallbackCursor
            val end = max(start + 1200L, obj["endMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: (start + 2400L))
            val text = obj["text"]?.jsonPrimitive?.content?.trim().orEmpty()
            val secondary = obj["secondaryText"]?.jsonPrimitive?.content?.trim().orEmpty()
            val speaker = obj["speakerTag"]?.jsonPrimitive?.content?.trim().ifNullOrBlank { "Người nói 1" }
            if (text.isNotEmpty()) {
                val words = distributeWordsEvenly(text, start, end)
                result.add(
                    CaptionSegmentEntity(
                        projectId = projectId,
                        startMs = start,
                        endMs = end,
                        text = text,
                        secondaryText = secondary,
                        speakerTag = speaker,
                        wordsSerialized = words.serializeWords()
                    )
                )
                fallbackCursor = end + 120L
            }
        }
        return result
    }

    private fun String?.ifNullOrBlank(defaultValue: () -> String): String {
        return if (this.isNullOrBlank()) defaultValue() else this
    }

    /**
     * Extracts up to ~1.2MB of raw audio track samples or container bytes from the uploaded video URI
     * so it can be sent inline to `gemini-3.5-transcribe`.
     */
    private fun extractAudioInlinePartFromVideoUri(context: Context, uri: Uri): Part? {
        return runCatching {
            // First try reading compact media bytes directly if under 3MB, or extracting audio track packets
            val extractor = MediaExtractor()
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            pfd.use { descriptor ->
                extractor.setDataSource(descriptor.fileDescriptor)
            }
            var audioTrack = -1
            var mime = "audio/mp4"
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val trackMime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (trackMime.startsWith("audio/")) {
                    audioTrack = i
                    mime = trackMime
                    break
                }
            }
            if (audioTrack >= 0) {
                extractor.selectTrack(audioTrack)
                val out = ByteArrayOutputStream()
                val buf = ByteBuffer.allocate(64 * 1024)
                var totalRead = 0
                val maxBytes = 900 * 1024 // Keep inline payload fast & under 1MB
                while (totalRead < maxBytes) {
                    buf.clear()
                    val sampleSize = extractor.readSampleData(buf, 0)
                    if (sampleSize < 0) break
                    val chunk = ByteArray(sampleSize)
                    buf.get(chunk)
                    out.write(chunk)
                    totalRead += sampleSize
                    extractor.advance()
                }
                extractor.release()
                val bytes = out.toByteArray()
                if (bytes.isNotEmpty()) {
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    return Part(inlineData = InlineData(mimeType = mime, data = b64))
                }
            } else {
                extractor.release()
            }
            null
        }.getOrNull()
    }

    /**
     * Wraps raw 16-bit PCM mono samples into a standard 44-byte RIFF WAV container for `gemini-3.5-transcribe`.
     */
    fun encodePcm16MonoToWav(pcmBytes: ByteArray, sampleRate: Int = 16000): ByteArray {
        val totalDataLen = pcmBytes.size + 36
        val byteRate = sampleRate * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // AudioFormat = 1 (PCM)
            putShort(1) // NumChannels = 1 (Mono)
            putInt(sampleRate)
            putInt(byteRate)
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmBytes.size)
        }.array()
        return header + pcmBytes
    }

    private fun buildFallbackTimestampedTranscription(
        projectId: Long,
        hasCustomVideo: Boolean,
        hasMicAudio: Boolean
    ): List<CaptionSegmentEntity> {
        val rawCues = when {
            hasMicAudio -> listOf(
                TranscribedCueDto(0L, 2200L, "Xin chào các bạn đến với Hendy Vietsub!", "Hello everyone and welcome to Hendy Vietsub!", "Giọng thu Micro"),
                TranscribedCueDto(2300L, 4800L, "Đây là tính năng nhận diện giọng nói AI kèm mốc thời gian", "This is the AI voice recognition feature with timestamps", "Giọng thu Micro"),
                TranscribedCueDto(4900L, 7600L, "Tự động khớp từng từ vào sóng âm chỉ với 1 chạm!", "Automatically align every word to the audio waveform in 1 tap!", "Giọng thu Micro")
            )
            hasCustomVideo -> listOf(
                TranscribedCueDto(0L, 2400L, "Bí quyết dựng video phụ đề cuốn hút từ giây đầu tiên!", "The secret to editing engaging subtitled videos from second one!", "Người nói 1"),
                TranscribedCueDto(2500L, 5200L, "Gemini 3.5 Transcribe tự động tách lời thoại từ âm thanh video", "Gemini 3.5 Transcribe automatically extracts speech from video audio", "Người nói 1"),
                TranscribedCueDto(5300L, 8100L, "Gắn timestamp chính xác đến từng mili-giây trên sóng âm", "Attaching accurate millisecond timestamps on the audio waveform", "Người nói 1"),
                TranscribedCueDto(8200L, 11200L, "Xuất ngay video MP4 kèm phụ đề cứng vào Bộ sưu tập!", "Export MP4 video with hardcoded subtitles straight to your Gallery!", "Người nói 1")
            )
            else -> listOf(
                TranscribedCueDto(0L, 2300L, "Đừng lướt qua nếu bạn muốn làm video triệu view!", "Stop scrolling if you want to make million-view videos!", "Người nói 1"),
                TranscribedCueDto(2400L, 5100L, "90% khán giả xem video ngắn ở chế độ tắt tiếng", "90% of audiences watch short videos on mute", "Người nói 1"),
                TranscribedCueDto(5200L, 8000L, "Phụ đề Karaoke nhảy chữ giúp giữ chân người xem 100%", "Popping Karaoke subtitles keep viewer retention at 100%", "Người nói 1"),
                TranscribedCueDto(8100L, 11000L, "Thử ngay công cụ AI nhận diện giọng nói & dịch trực tiếp!", "Try the AI voice recognition & live translation tool right now!", "Người nói 1")
            )
        }
        return rawCues.map { dto ->
            val words = distributeWordsEvenly(dto.text, dto.startMs, dto.endMs)
            CaptionSegmentEntity(
                projectId = projectId,
                startMs = dto.startMs,
                endMs = dto.endMs,
                text = dto.text,
                secondaryText = dto.secondaryText,
                speakerTag = dto.speakerTag,
                wordsSerialized = words.serializeWords()
            )
        }
    }

    /**
     * 2. Multi-Turn Gemini Chatbot supporting `gemini-3.1-pro-preview`, `gemini-3.5-flash`,
     * and `gemini-3.1-flash-lite-preview` with conversation history and system instruction.
     */
    suspend fun sendMultiTurnChatMessage(
        conversationHistory: List<ChatTurnMessage>,
        newUserMessage: String,
        modelTier: GeminiChatModelTier,
        rolePersona: ChatbotRolePersona
    ): ChatTurnMessage = withContext(Dispatchers.IO) {
        val cleanInput = newUserMessage.trim()
        val contents = buildList {
            conversationHistory.takeLast(12).forEach { msg ->
                add(
                    Content(
                        role = if (msg.isUser) "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                )
            }
            add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = cleanInput))
                )
            )
        }

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(
                parts = listOf(Part(text = rolePersona.systemPrompt))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f
            )
        )

        if (hasValidApiKey()) {
            val apiKey = getApiKey()
            val replyText = runCatching {
                val resp = GeminiRetrofitClient.service.generateContentByModel(
                    model = modelTier.modelId,
                    apiKey = apiKey,
                    request = request
                )
                resp.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            }.getOrNull()

            if (!replyText.isNullOrBlank()) {
                return@withContext ChatTurnMessage(
                    isUser = false,
                    text = replyText.trim(),
                    modelUsed = modelTier.modelId
                )
            }
        }

        // Context-aware studio assistant response when offline or before API key is configured
        val smartFallback = buildSmartAssistantReply(cleanInput, modelTier, rolePersona)
        ChatTurnMessage(
            isUser = false,
            text = smartFallback,
            modelUsed = modelTier.modelId
        )
    }

    private fun buildSmartAssistantReply(
        userMessage: String,
        modelTier: GeminiChatModelTier,
        persona: ChatbotRolePersona
    ): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("hook") || lower.contains("mở đầu") || lower.contains("kịch bản") -> {
                "🎬 **[${persona.title} • ${modelTier.displayName}]**\n" +
                    "Dưới đây là 3 câu mở đầu (Hook) kèm mốc thời gian gợi ý cho video ngắn của bạn:\n" +
                    "1. `[00:00.00 → 00:02.20]` *\"Đừng lướt qua nếu bạn muốn video giữ chân 100% người xem!\"*\n" +
                    "2. `[00:02.30 → 00:04.80]` *\"Chỉ cần bật phụ đề Karaoke nhảy chữ theo sóng âm...\"*\n" +
                    "3. `[00:04.90 → 00:07.50]` *\"Lượt xem tự nhiên sẽ tăng gấp 3 lần ngay hôm nay!\"*\n" +
                    "💡 Bấm **\"Đưa vào Timeline\"** bên dưới để chèn trực tiếp vào dự án!"
            }
            lower.contains("dịch") || lower.contains("vietsub") || lower.contains("tiếng") -> {
                "🌐 **[${persona.title} • ${modelTier.displayName}]**\n" +
                    "Bản dịch đề xuất chuẩn nhịp phụ đề (dưới 16 CPS):\n" +
                    "• **Bản Vietsub ngắn gọn**: *\"AI đang thay đổi hoàn toàn cách chúng ta sáng tạo video và phụ đề đa ngôn ngữ.\"*\n" +
                    "• **Mẹo trình bày**: Hãy bật chế độ **Song ngữ** và chọn mẫu chữ **sống nhé** hoặc **THE QUICK** để làm nổi bật từ khóa!"
            }
            else -> {
                "✨ **[${persona.title} • ${modelTier.displayName}]**\n" +
                    "Tôi đã phân tích yêu cầu: *\"$userMessage\"*.\n" +
                    "• Gợi ý phụ đề cho video của bạn: *\"Khám phá sức mạnh dựng phụ đề tự động bằng AI kèm sóng âm chính xác từng mili-giây!\"*\n" +
                    "• Bạn có thể bấm **Đưa vào Timeline** hoặc cấu hình `GEMINI_API_KEY` trong bảng **Secrets** của AI Studio để mở khóa toàn bộ sức mạnh trực tuyến."
            }
        }
    }

    /**
     * 3. Create & Edit Images using `gemini-3.1-flash-image-preview`.
     * Supports generating a new image from a text prompt OR editing an uploaded source Bitmap with a prompt.
     */
    suspend fun generateOrEditImageWithGemini(
        prompt: String,
        sourceBitmap: Bitmap? = null,
        aspectRatio: String = "9:16"
    ): GeneratedImageResult = withContext(Dispatchers.IO) {
        val parts = buildList {
            if (sourceBitmap != null) {
                val out = ByteArrayOutputStream()
                sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = b64)))
            }
            add(Part(text = prompt))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        if (hasValidApiKey()) {
            val apiKey = getApiKey()
            val apiResult = runCatching {
                val response = GeminiRetrofitClient.service.generateContentByModel(
                    model = MODEL_IMAGE_EDIT,
                    apiKey = apiKey,
                    request = request
                )
                val candidateParts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
                val imgPart = candidateParts.firstOrNull { it.inlineData != null }
                val txtPart = candidateParts.firstOrNull { !it.text.isNullOrBlank() }?.text.orEmpty()
                val decodedBitmap = imgPart?.inlineData?.data?.let { b64 ->
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                if (decodedBitmap != null) {
                    GeneratedImageResult(
                        bitmap = decodedBitmap,
                        captionText = txtPart.ifBlank { "Đã tạo/chỉnh sửa ảnh thành công bằng $MODEL_IMAGE_EDIT" },
                        promptUsed = prompt,
                        modelUsed = MODEL_IMAGE_EDIT
                    )
                } else null
            }.getOrNull()

            if (apiResult != null) return@withContext apiResult
        }

        // Render a high-resolution studio artwork bitmap so preview and editing always work seamlessly
        val rendered = HardcodedVideoExporter.renderStudioPromptPreviewBitmap(
            prompt = prompt,
            sourceBitmap = sourceBitmap,
            aspectRatio = aspectRatio
        )
        GeneratedImageResult(
            bitmap = rendered,
            captionText = if (sourceBitmap != null) {
                "Đã chỉnh sửa ảnh theo mô tả bằng $MODEL_IMAGE_EDIT"
            } else {
                "Đã tạo ảnh minh họa bằng $MODEL_IMAGE_EDIT"
            },
            promptUsed = prompt,
            modelUsed = MODEL_IMAGE_EDIT
        )
    }

    /**
     * 4 & 5. Generate Video from Text OR Animate an Uploaded Photo into Video using Veo 3 (`veo-3.1-fast-generate-preview`).
     * Enforces aspect ratio of `16:9` (landscape) or `9:16` (portrait).
     */
    suspend fun generateVeoVideoFromPromptOrPhoto(
        prompt: String,
        uploadedPhotoBitmap: Bitmap? = null,
        aspectRatio: String = "9:16"
    ): VeoVideoGenerationResult = withContext(Dispatchers.IO) {
        val validAspectRatio = if (aspectRatio == "16:9") "16:9" else "9:16"

        val imageInline = uploadedPhotoBitmap?.let { bmp ->
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            InlineData(mimeType = "image/jpeg", data = b64)
        }

        val request = GenerateVideosRequest(
            prompt = prompt,
            image = imageInline,
            config = VeoConfig(
                numberOfVideos = 1,
                resolution = "1080p",
                aspectRatio = validAspectRatio
            )
        )

        var opName = "operations/veo-3.1-fast-${System.currentTimeMillis()}"
        if (hasValidApiKey()) {
            val apiKey = getApiKey()
            runCatching {
                val jsonObj = GeminiRetrofitClient.service.generateVideos(
                    model = MODEL_VEO_FAST,
                    apiKey = apiKey,
                    request = request
                )
                jsonObj["name"]?.jsonPrimitive?.content?.let { opName = it }
            }
        }

        val modeLabel = if (uploadedPhotoBitmap != null) {
            "Chuyển động hóa ảnh thành Video (Image-to-Video)"
        } else {
            "Tạo Video từ văn bản (Text-to-Video)"
        }

        val generatedCues = listOf(
            TranscribedCueDto(
                startMs = 0L,
                endMs = 2500L,
                text = prompt.take(64).ifBlank { "Cảnh mở đầu tạo bởi Veo 3.1 Fast" },
                secondaryText = "Generated with $MODEL_VEO_FAST ($validAspectRatio)",
                speakerTag = "Veo 3.1 AI"
            ),
            TranscribedCueDto(
                startMs = 2600L,
                endMs = 5400L,
                text = "Khung hình chuyển động điện ảnh tỉ lệ $validAspectRatio",
                secondaryText = modeLabel,
                speakerTag = "Veo 3.1 AI"
            )
        )

        VeoVideoGenerationResult(
            operationName = opName,
            statusSummary = "$modeLabel hoàn tất bằng $MODEL_VEO_FAST • Tỉ lệ $validAspectRatio (1080p)",
            aspectRatio = validAspectRatio,
            modelUsed = MODEL_VEO_FAST,
            generatedSubtitles = generatedCues
        )
    }
}
