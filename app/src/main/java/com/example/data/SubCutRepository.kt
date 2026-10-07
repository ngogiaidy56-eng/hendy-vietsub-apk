package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class SubCutRepository(private val dao: SubCutDao) {

    val allProjects: Flow<List<SubtitleProjectEntity>> = dao.observeAllProjects()
    val allPresets: Flow<List<StylePresetEntity>> = dao.observeAllPresets()

    fun observeProject(projectId: Long): Flow<SubtitleProjectEntity?> =
        dao.observeProjectById(projectId)

    fun observeSegments(projectId: Long): Flow<List<CaptionSegmentEntity>> =
        dao.observeSegmentsForProject(projectId)

    suspend fun getSegmentsOnce(projectId: Long): List<CaptionSegmentEntity> =
        dao.getSegmentsForProjectOnce(projectId)

    suspend fun ensureSeedData(): Long {
        if (dao.getPresetCount() == 0) {
            dao.insertPresets(defaultPresets())
        }
        val existingProjects = dao.getAllProjectsOnce()
        if (existingProjects.isNotEmpty()) {
            return existingProjects.first().id
        }

        val now = System.currentTimeMillis()

        // Dự án 1: Mở đầu Video Xu hướng (9:16)
        val proj1Id = dao.insertProject(
            SubtitleProjectEntity(
                title = "Mở Đầu Video Xu Hướng • 9:16",
                description = "Phụ đề Karaoke nhảy từng từ giữ chân người xem kèm bản dịch song ngữ",
                durationMs = 15000L,
                aspectRatio = AspectRatioMode.RATIO_9_16.name,
                backdrop = CanvasBackdrop.CYBER_GRID.name,
                verticalPositionRatio = 0.74f,
                showSafeZones = true,
                showBilingual = true,
                fontOption = SubtitleFontOption.MONTSERRAT.name,
                fontSizeSp = 24f,
                textColorHex = "#FFFFFF",
                activeWordColorHex = "#CCFF00",
                strokeColorHex = "#000000",
                strokeWidthDp = 4f,
                shadowBlur = 8f,
                bgColorHex = "#0B0D12",
                bgOpacity = 0.35f,
                bgCornerRadiusDp = 12f,
                isUppercase = true,
                animationMode = WordAnimationMode.KARAOKE_POP.name,
                updatedAt = now
            )
        )
        dao.insertSegments(seedSegmentsForViralHook(proj1Id))

        // Dự án 2: Đánh giá Máy quay Điện ảnh (16:9)
        val proj2Id = dao.insertProject(
            SubtitleProjectEntity(
                title = "Đánh Giá Máy Quay Điện Ảnh • 16:9",
                description = "Khung hình ngang với hiệu ứng hộp chữ Karaoke trượt mượt mà",
                durationMs = 14000L,
                aspectRatio = AspectRatioMode.RATIO_16_9.name,
                backdrop = CanvasBackdrop.CINEMA_NOIR.name,
                verticalPositionRatio = 0.82f,
                showSafeZones = false,
                showBilingual = false,
                fontOption = SubtitleFontOption.SPACE_GROTESK.name,
                fontSizeSp = 22f,
                textColorHex = "#FFFFFF",
                activeWordColorHex = "#00F0FF",
                strokeColorHex = "#0B0D12",
                strokeWidthDp = 2.5f,
                shadowBlur = 6f,
                bgColorHex = "#131720",
                bgOpacity = 0.72f,
                bgCornerRadiusDp = 8f,
                isUppercase = false,
                animationMode = WordAnimationMode.KARAOKE_BOX.name,
                updatedAt = now - 60_000L
            )
        )
        dao.insertSegments(seedSegmentsForTechReview(proj2Id))

        // Dự án 3: Vlog Ẩm Thực Đêm (9:16)
        val proj3Id = dao.insertProject(
            SubtitleProjectEntity(
                title = "Vlog Ẩm Thực Đêm • 9:16",
                description = "Phông chữ Bangers nổi bật kèm phụ đề song ngữ Việt - Anh",
                durationMs = 12000L,
                aspectRatio = AspectRatioMode.RATIO_9_16.name,
                backdrop = CanvasBackdrop.SUNSET_VLOG.name,
                verticalPositionRatio = 0.76f,
                showSafeZones = true,
                showBilingual = true,
                fontOption = SubtitleFontOption.BANGERS.name,
                fontSizeSp = 27f,
                textColorHex = "#FFE600",
                activeWordColorHex = "#FF3366",
                strokeColorHex = "#000000",
                strokeWidthDp = 4.5f,
                shadowBlur = 10f,
                bgColorHex = "#000000",
                bgOpacity = 0.0f,
                bgCornerRadiusDp = 10f,
                isUppercase = true,
                animationMode = WordAnimationMode.BOUNCE_IN.name,
                updatedAt = now - 120_000L
            )
        )
        dao.insertSegments(seedSegmentsForTokyoVlog(proj3Id))

        return proj1Id
    }

    suspend fun createNewProject(
        title: String,
        aspectRatio: AspectRatioMode,
        backdrop: CanvasBackdrop,
        videoUri: String? = null,
        initialSegments: List<CaptionSegmentEntity>? = null
    ): Long {
        val maxEnd = initialSegments?.maxOfOrNull { it.endMs } ?: 12000L
        val projectId = dao.insertProject(
            SubtitleProjectEntity(
                title = title.ifBlank { "Dự Án Phụ Đề Mới" },
                description = if (videoUri != null) "Đã liên kết với video trên thiết bị" else "Timeline phụ đề tùy chỉnh",
                durationMs = max(10000L, maxEnd + 1000L),
                aspectRatio = aspectRatio.name,
                backdrop = backdrop.name,
                videoUri = videoUri,
                updatedAt = System.currentTimeMillis()
            )
        )
        val segmentsToInsert = if (!initialSegments.isNullOrEmpty()) {
            initialSegments.map { it.copy(id = 0, projectId = projectId) }
        } else {
            val defaultWords1 = distributeWordsEvenly("Chạm vào từng từ để tạo điểm nhấn Karaoke!", 0L, 3200L)
                .mapIndexed { idx, w -> if (idx == 0 || idx == 7) w.copy(isEmphasized = true) else w }
            val defaultWords2 = distributeWordsEvenly("Kéo phụ đề trên màn hình hoặc tách câu tại kim phát", 3400L, 7200L)
                .mapIndexed { idx, w -> if (idx == 0 || idx == 6) w.copy(isEmphasized = true) else w }
            listOf(
                CaptionSegmentEntity(
                    projectId = projectId,
                    startMs = 0L,
                    endMs = 3200L,
                    text = "Chạm vào từng từ để tạo điểm nhấn Karaoke!",
                    secondaryText = "Tap any word to customize karaoke emphasis!",
                    speakerTag = "Người dẫn",
                    wordsSerialized = defaultWords1.serializeWords()
                ),
                CaptionSegmentEntity(
                    projectId = projectId,
                    startMs = 3400L,
                    endMs = 7200L,
                    text = "Kéo phụ đề trên màn hình hoặc tách câu tại kim phát",
                    secondaryText = "Drag captions on stage or split at playhead",
                    speakerTag = "Người dẫn",
                    wordsSerialized = defaultWords2.serializeWords()
                )
            )
        }
        dao.insertSegments(segmentsToInsert)
        return projectId
    }

    suspend fun updateProject(project: SubtitleProjectEntity) {
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(projectId: Long) {
        dao.deleteSegmentsForProject(projectId)
        dao.deleteProjectById(projectId)
    }

    suspend fun saveSegment(segment: CaptionSegmentEntity) {
        if (segment.id == 0L) {
            dao.insertSegment(segment)
        } else {
            dao.updateSegment(segment)
        }
    }

    suspend fun deleteSegment(segmentId: Long) {
        dao.deleteSegmentById(segmentId)
    }

    suspend fun replaceAllSegments(projectId: Long, segments: List<CaptionSegmentEntity>) {
        dao.replaceProjectSegments(projectId, segments.map { it.copy(projectId = projectId) })
    }

    suspend fun saveCustomPreset(name: String, project: SubtitleProjectEntity) {
        dao.insertPreset(
            StylePresetEntity(
                name = name.ifBlank { "Phong Cách Riêng" },
                badge = "TÙY CHỈNH",
                fontOption = project.fontOption,
                fontSizeSp = project.fontSizeSp,
                textColorHex = project.textColorHex,
                activeWordColorHex = project.activeWordColorHex,
                strokeColorHex = project.strokeColorHex,
                strokeWidthDp = project.strokeWidthDp,
                shadowBlur = project.shadowBlur,
                bgColorHex = project.bgColorHex,
                bgOpacity = project.bgOpacity,
                bgCornerRadiusDp = project.bgCornerRadiusDp,
                isUppercase = project.isUppercase,
                animationMode = project.animationMode,
                isBuiltIn = false
            )
        )
    }

    suspend fun deleteCustomPreset(presetId: Long) {
        dao.deleteCustomPreset(presetId)
    }

    private fun seedSegmentsForViralHook(projectId: Long): List<CaptionSegmentEntity> {
        data class SeedCue(val s: Long, val e: Long, val vi: String, val en: String, val empIndices: Set<Int>)
        val cues = listOf(
            SeedCue(0L, 2600L, "Đừng lướt qua nếu bạn làm video ngắn!", "Stop scrolling if you edit short videos!", setOf(0, 1, 6)),
            SeedCue(2700L, 5800L, "90% người xem tắt tiếng và chỉ đọc phụ đề", "90% of viewers watch on mute with captions", setOf(0, 4, 8)),
            SeedCue(5900L, 9200L, "Hiệu ứng nhảy chữ từng từ giữ chân 100%", "Word-by-word pop keeps retention at 100%", setOf(2, 3, 7)),
            SeedCue(9300L, 12200L, "Tách câu, đổi phông và xuất file SRT hoặc ASS", "Split, style, and export to SRT or ASS", setOf(0, 7, 9)),
            SeedCue(12300L, 15000L, "Hãy thử kéo thanh timeline ngay bên dưới!", "Try scrubbing the timeline right below!", setOf(2, 4, 6))
        )
        return cues.map { cue ->
            val words = distributeWordsEvenly(cue.vi, cue.s, cue.e).mapIndexed { i, w ->
                w.copy(isEmphasized = i in cue.empIndices)
            }
            CaptionSegmentEntity(
                projectId = projectId,
                startMs = cue.s,
                endMs = cue.e,
                text = cue.vi,
                secondaryText = cue.en,
                speakerTag = "Creator",
                wordsSerialized = words.serializeWords()
            )
        }
    }

    private fun seedSegmentsForTechReview(projectId: Long): List<CaptionSegmentEntity> {
        val raw = listOf(
            Triple(0L, 3400L, "Trải nghiệm ống kính điện ảnh 35mm f/1.4 thế hệ mới"),
            Triple(3500L, 7100L, "Chi tiết vùng sáng cực kỳ mượt mà ở mức ISO 3200"),
            Triple(7200L, 10600L, "Lấy nét tự động siêu êm khi quay cận cảnh tới vô cực"),
            Triple(10700L, 14000L, "Xuất phụ đề WebVTT và SubRip chuẩn từng khung hình")
        )
        return raw.map { (s, e, text) ->
            val words = distributeWordsEvenly(text, s, e).mapIndexed { idx, w ->
                w.copy(isEmphasized = idx == 4 || idx == 5)
            }
            CaptionSegmentEntity(
                projectId = projectId,
                startMs = s,
                endMs = e,
                text = text,
                secondaryText = "",
                speakerTag = "Reviewer",
                wordsSerialized = words.serializeWords()
            )
        }
    }

    private fun seedSegmentsForTokyoVlog(projectId: Long): List<CaptionSegmentEntity> {
        val raw = listOf(
            Triple(0L, 3000L, "Khám phá quán mì Ramen lúc 2 giờ sáng!" to "2:00 AM ramen run in tiny alleyways!"),
            Triple(3100L, 6200L, "Nước dùng đậm đà ninh suốt 18 tiếng liên tục" to "Rich smoky broth simmered for 18 hours straight"),
            Triple(6300L, 9100L, "Nghe tiếng áp chảo giòn rụm của đĩa há cảo" to "Listen to that sizzle on the crispy gyoza"),
            Triple(9200L, 12000L, "Bữa ăn đêm tuyệt vời nhất cả chuyến đi!" to "Best midnight meal of the entire trip!")
        )
        return raw.map { (s, e, pair) ->
            val words = distributeWordsEvenly(pair.first, s, e).mapIndexed { idx, w ->
                w.copy(isEmphasized = idx == 3 || idx == 5)
            }
            CaptionSegmentEntity(
                projectId = projectId,
                startMs = s,
                endMs = e,
                text = pair.first,
                secondaryText = pair.second,
                speakerTag = "Vlogger",
                wordsSerialized = words.serializeWords()
            )
        }
    }

    private fun defaultPresets(): List<StylePresetEntity> = listOf(
        StylePresetEntity(
            name = "Viral Cực Mạnh",
            badge = "HOT",
            fontOption = SubtitleFontOption.MONTSERRAT.name,
            fontSizeSp = 25f,
            textColorHex = "#FFFFFF",
            activeWordColorHex = "#CCFF00",
            strokeColorHex = "#000000",
            strokeWidthDp = 4.5f,
            shadowBlur = 8f,
            bgColorHex = "#000000",
            bgOpacity = 0.0f,
            bgCornerRadiusDp = 10f,
            isUppercase = true,
            animationMode = WordAnimationMode.KARAOKE_POP.name,
            isBuiltIn = true
        ),
        StylePresetEntity(
            name = "Khung Karaoke",
            badge = "VIETSUB",
            fontOption = SubtitleFontOption.SPACE_GROTESK.name,
            fontSizeSp = 23f,
            textColorHex = "#FFFFFF",
            activeWordColorHex = "#00F0FF",
            strokeColorHex = "#0B0D12",
            strokeWidthDp = 2.5f,
            shadowBlur = 4f,
            bgColorHex = "#131720",
            bgOpacity = 0.78f,
            bgCornerRadiusDp = 12f,
            isUppercase = false,
            animationMode = WordAnimationMode.KARAOKE_BOX.name,
            isBuiltIn = true
        ),
        StylePresetEntity(
            name = "Hoạt Hình Nổi Bật",
            badge = "ẤN TƯỢNG",
            fontOption = SubtitleFontOption.BANGERS.name,
            fontSizeSp = 29f,
            textColorHex = "#FFE600",
            activeWordColorHex = "#FF3366",
            strokeColorHex = "#000000",
            strokeWidthDp = 5f,
            shadowBlur = 10f,
            bgColorHex = "#000000",
            bgOpacity = 0.0f,
            bgCornerRadiusDp = 8f,
            isUppercase = true,
            animationMode = WordAnimationMode.BOUNCE_IN.name,
            isBuiltIn = true
        ),
        StylePresetEntity(
            name = "Ánh Đèn Neon",
            badge = "GLOW",
            fontOption = SubtitleFontOption.SPACE_GROTESK.name,
            fontSizeSp = 24f,
            textColorHex = "#F5F7FA",
            activeWordColorHex = "#00F0FF",
            strokeColorHex = "#007A85",
            strokeWidthDp = 2f,
            shadowBlur = 14f,
            bgColorHex = "#0B0D12",
            bgOpacity = 0.55f,
            bgCornerRadiusDp = 14f,
            isUppercase = true,
            animationMode = WordAnimationMode.NEON_PULSE.name,
            isBuiltIn = true
        ),
        StylePresetEntity(
            name = "Bút Dạ Vlog",
            badge = "VLOG",
            fontOption = SubtitleFontOption.PERMANENT_MARKER.name,
            fontSizeSp = 24f,
            textColorHex = "#FFFFFF",
            activeWordColorHex = "#FF7A00",
            strokeColorHex = "#000000",
            strokeWidthDp = 3.5f,
            shadowBlur = 6f,
            bgColorHex = "#000000",
            bgOpacity = 0.25f,
            bgCornerRadiusDp = 10f,
            isUppercase = false,
            animationMode = WordAnimationMode.KARAOKE_POP.name,
            isBuiltIn = true
        ),
        StylePresetEntity(
            name = "Phim Điện Ảnh",
            badge = "CHUẨN",
            fontOption = SubtitleFontOption.PLUS_JAKARTA.name,
            fontSizeSp = 20f,
            textColorHex = "#FFE600",
            activeWordColorHex = "#FFFFFF",
            strokeColorHex = "#000000",
            strokeWidthDp = 2f,
            shadowBlur = 4f,
            bgColorHex = "#000000",
            bgOpacity = 0.65f,
            bgCornerRadiusDp = 6f,
            isUppercase = false,
            animationMode = WordAnimationMode.STATIC_CLEAN.name,
            isBuiltIn = true
        )
    )
}
