package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AspectRatioMode
import com.example.data.AudioWaveformData
import com.example.data.AudioWaveformExtractor
import com.example.data.CanvasBackdrop
import com.example.data.CaptionSegmentEntity
import com.example.data.ExportFormat
import com.example.data.GeminiAiRepository
import com.example.data.HardcodedVideoExporter
import com.example.data.ScriptCadenceMode
import com.example.data.StylePresetEntity
import com.example.data.SubCutRepository
import com.example.data.SubtitleFontOption
import com.example.data.SubtitleParserExporter
import com.example.data.SubtitleProjectEntity
import com.example.data.VideoExportProgress
import com.example.data.VideoExportQuality
import com.example.data.WordAnimationMode
import com.example.data.distributeWordsEvenly
import com.example.data.serializeWords
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToLong

enum class StudioBottomTab(val label: String) {
    TIMELINE("Timeline"),
    BATCH_SCRIPT("Bản thảo"),
    STYLE_STUDIO("Kiểu chữ"),
    WORD_FX("Hiệu ứng"),
    EXPORT("Xuất file")
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
    private val repository: SubCutRepository
) : ViewModel() {

    val projects: StateFlow<List<SubtitleProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presets: StateFlow<List<StylePresetEntity>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    val activeProject: StateFlow<SubtitleProjectEntity?> = _activeProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeProject(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val segments: StateFlow<List<CaptionSegmentEntity>> = _activeProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeSegments(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProjectHubOpen = MutableStateFlow(true)
    val isProjectHubOpen: StateFlow<Boolean> = _isProjectHubOpen.asStateFlow()

    private val _activeBottomTab = MutableStateFlow(StudioBottomTab.TIMELINE)
    val activeBottomTab: StateFlow<StudioBottomTab> = _activeBottomTab.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _selectedSegmentId = MutableStateFlow<Long?>(null)
    val selectedSegmentId: StateFlow<Long?> = _selectedSegmentId.asStateFlow()

    private val _timelineZoom = MutableStateFlow(1.0f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _selectedExportFormat = MutableStateFlow(ExportFormat.SRT)
    val selectedExportFormat: StateFlow<ExportFormat> = _selectedExportFormat.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _audioWaveform = MutableStateFlow<AudioWaveformData?>(null)
    val audioWaveform: StateFlow<AudioWaveformData?> = _audioWaveform.asStateFlow()

    val waveformAlignmentScore: StateFlow<Int> = combine(segments, _audioWaveform) { segs, wf ->
        if (wf == null) 94 else AudioWaveformExtractor.computeAlignmentScore(segs, wf)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 94)

    private val _videoExportProgress = MutableStateFlow(VideoExportProgress())
    val videoExportProgress: StateFlow<VideoExportProgress> = _videoExportProgress.asStateFlow()

    private val _isAiTranscribing = MutableStateFlow(false)
    val isAiTranscribing: StateFlow<Boolean> = _isAiTranscribing.asStateFlow()

    private val _undoStack = MutableStateFlow<List<List<CaptionSegmentEntity>>>(emptyList())
    private val _redoStack = MutableStateFlow<List<List<CaptionSegmentEntity>>>(emptyList())

    val canUndo: StateFlow<Boolean> = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = MutableStateFlow(false)

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            val defaultProjId = repository.ensureSeedData()
            _activeProjectId.value = defaultProjId
        }
        viewModelScope.launch {
            combine(activeProject, segments) { proj, segs -> proj to segs }
                .collect { (proj, segs) ->
                    if (proj != null) {
                        val dur = max(proj.durationMs, (segs.maxOfOrNull { it.endMs } ?: 12000L) + 1000L)
                        _audioWaveform.value = AudioWaveformExtractor.generateStudioVocalWaveform(
                            durationMs = dur,
                            segments = segs,
                            binDurationMs = 50L
                        )
                    }
                }
        }
    }

    fun showStatus(message: String) {
        _statusBannerMessage.value = message
        viewModelScope.launch {
            delay(3200L)
            if (_statusBannerMessage.value == message) {
                _statusBannerMessage.value = null
            }
        }
    }

    fun openProjectHub() {
        pausePlayback()
        _isProjectHubOpen.value = true
    }

    fun closeProjectHub() {
        _isProjectHubOpen.value = false
    }

    fun selectProject(projectId: Long) {
        pausePlayback()
        _activeProjectId.value = projectId
        _playheadMs.value = 0L
        _selectedSegmentId.value = null
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        updateUndoRedoFlags()
        _isProjectHubOpen.value = false
    }

    fun createNewProject(
        title: String,
        aspectRatio: AspectRatioMode,
        backdrop: CanvasBackdrop,
        videoUri: String? = null
    ) {
        viewModelScope.launch {
            val newId = repository.createNewProject(
                title = title,
                aspectRatio = aspectRatio,
                backdrop = backdrop,
                videoUri = videoUri
            )
            selectProject(newId)
            showStatus("Đã tạo dự án \"${title.ifBlank { "Dự Án Phụ Đề Mới" }}\"")
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            val remaining = projects.value.filter { it.id != projectId }
            if (_activeProjectId.value == projectId) {
                _activeProjectId.value = remaining.firstOrNull()?.id
            }
            showStatus("Đã xóa dự án")
        }
    }

    fun setBottomTab(tab: StudioBottomTab) {
        _activeBottomTab.value = tab
    }

    fun setExportFormat(format: ExportFormat) {
        _selectedExportFormat.value = format
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    private fun startPlayback() {
        val totalDuration = computeProjectDuration()
        if (_playheadMs.value >= totalDuration - 80L) {
            _playheadMs.value = 0L
        }
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val tickIntervalMs = 33L
            while (_isPlaying.value) {
                delay(tickIntervalMs)
                val step = (tickIntervalMs * _playbackSpeed.value).roundToLong()
                val next = _playheadMs.value + step
                val maxDur = computeProjectDuration()
                if (next >= maxDur) {
                    _playheadMs.value = 0L
                } else {
                    _playheadMs.value = next
                }
                val currentCue = segments.value.firstOrNull {
                    _playheadMs.value in it.startMs..it.endMs
                }
                if (currentCue != null) {
                    _selectedSegmentId.value = currentCue.id
                }
            }
        }
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
        val idx = speeds.indexOfFirst { it == _playbackSpeed.value }
        _playbackSpeed.value = speeds[(idx + 1) % speeds.size]
    }

    fun seekTo(ms: Long) {
        val maxDur = computeProjectDuration()
        val clamped = ms.coerceIn(0L, maxDur)
        _playheadMs.value = clamped
        val matching = segments.value.firstOrNull { clamped in it.startMs..it.endMs }
        if (matching != null) {
            _selectedSegmentId.value = matching.id
        }
    }

    fun selectSegment(segment: CaptionSegmentEntity, movePlayhead: Boolean = true) {
        _selectedSegmentId.value = segment.id
        if (movePlayhead && _playheadMs.value !in segment.startMs..segment.endMs) {
            _playheadMs.value = segment.startMs + 50L
        }
    }

    fun adjustTimelineZoom(delta: Float) {
        _timelineZoom.value = (_timelineZoom.value + delta).coerceIn(0.6f, 2.6f)
    }

    private suspend fun recordUndoSnapshot() {
        val projectId = _activeProjectId.value ?: return
        val currentList = repository.getSegmentsOnce(projectId)
        _undoStack.value = (_undoStack.value + listOf(currentList)).takeLast(20)
        _redoStack.value = emptyList()
        updateUndoRedoFlags()
    }

    private fun updateUndoRedoFlags() {
        (canUndo as MutableStateFlow).value = _undoStack.value.isNotEmpty()
        (canRedo as MutableStateFlow).value = _redoStack.value.isNotEmpty()
    }

    fun undo() {
        val projectId = _activeProjectId.value ?: return
        val stack = _undoStack.value
        if (stack.isEmpty()) return
        viewModelScope.launch {
            val current = repository.getSegmentsOnce(projectId)
            val previous = stack.last()
            _undoStack.value = stack.dropLast(1)
            _redoStack.value = (_redoStack.value + listOf(current)).takeLast(20)
            updateUndoRedoFlags()
            repository.replaceAllSegments(projectId, previous)
            showStatus("Đã hoàn tác thao tác phụ đề")
        }
    }

    fun redo() {
        val projectId = _activeProjectId.value ?: return
        val stack = _redoStack.value
        if (stack.isEmpty()) return
        viewModelScope.launch {
            val current = repository.getSegmentsOnce(projectId)
            val next = stack.last()
            _redoStack.value = stack.dropLast(1)
            _undoStack.value = (_undoStack.value + listOf(current)).takeLast(20)
            updateUndoRedoFlags()
            repository.replaceAllSegments(projectId, next)
            showStatus("Đã làm lại thao tác phụ đề")
        }
    }

    // --- Thao tác Timeline Hendy Vietsub ---

    fun addCaptionAtPlayhead() {
        val projectId = _activeProjectId.value ?: return
        viewModelScope.launch {
            recordUndoSnapshot()
            val currentList = segments.value
            val cursor = _playheadMs.value
            val overlapping = currentList.firstOrNull { cursor in it.startMs until it.endMs }
            val start = overlapping?.endMs?.plus(60L) ?: cursor
            val end = start + 2400L
            val defaultText = "Câu phụ đề mới"
            val words = distributeWordsEvenly(defaultText, start, end)
            val newSeg = CaptionSegmentEntity(
                projectId = projectId,
                startMs = start,
                endMs = end,
                text = defaultText,
                secondaryText = "",
                speakerTag = "Người nói 1",
                wordsSerialized = words.serializeWords()
            )
            repository.saveSegment(newSeg)
            extendProjectDurationIfNeeded(end)
            _playheadMs.value = start + 100L
            showStatus("Đã thêm câu phụ đề tại ${com.example.data.formatTimecodeShort(start)}")
        }
    }

    fun splitAtPlayhead() {
        val projectId = _activeProjectId.value ?: return
        val currentList = segments.value
        val cursor = _playheadMs.value
        val target = currentList.firstOrNull { cursor in (it.startMs + 250L)..(it.endMs - 250L) }
            ?: currentList.firstOrNull { it.id == _selectedSegmentId.value }

        if (target == null) {
            showStatus("Hãy chọn câu phụ đề hoặc đặt kim phát vào giữa câu để tách")
            return
        }

        val splitPoint = if (cursor in (target.startMs + 250L)..(target.endMs - 250L)) {
            cursor
        } else {
            (target.startMs + target.endMs) / 2L
        }

        if (target.durationMs < 500L) {
            showStatus("Câu phụ đề quá ngắn để tách tiếp")
            return
        }

        viewModelScope.launch {
            recordUndoSnapshot()
            val words = target.parsedWords()
            val (firstText, secondText) = if (words.size >= 2) {
                val pivotIdx = words.indexOfFirst { it.endMs > splitPoint }.coerceIn(1, words.size - 1)
                val part1 = words.take(pivotIdx).joinToString(" ") { it.word }
                val part2 = words.drop(pivotIdx).joinToString(" ") { it.word }
                part1 to part2
            } else {
                target.text to "${target.text} (2)"
            }

            val firstWords = distributeWordsEvenly(firstText, target.startMs, splitPoint - 20L, words)
            val secondWords = distributeWordsEvenly(secondText, splitPoint + 20L, target.endMs, words)

            repository.saveSegment(
                target.copy(
                    endMs = splitPoint - 20L,
                    text = firstText,
                    wordsSerialized = firstWords.serializeWords()
                )
            )
            repository.saveSegment(
                CaptionSegmentEntity(
                    projectId = projectId,
                    startMs = splitPoint + 20L,
                    endMs = target.endMs,
                    text = secondText,
                    secondaryText = target.secondaryText,
                    speakerTag = target.speakerTag,
                    wordsSerialized = secondWords.serializeWords()
                )
            )
            showStatus("Đã tách đôi câu phụ đề tại ${com.example.data.formatTimecodeShort(splitPoint)}")
        }
    }

    fun mergeWithNext() {
        val currentList = segments.value
        val selected = currentList.firstOrNull { it.id == _selectedSegmentId.value }
            ?: currentList.firstOrNull { _playheadMs.value in it.startMs..it.endMs }
        if (selected == null) {
            showStatus("Hãy chọn một câu phụ đề để gộp với câu kế tiếp")
            return
        }
        val idx = currentList.indexOfFirst { it.id == selected.id }
        val nextSeg = currentList.getOrNull(idx + 1)
        if (nextSeg == null) {
            showStatus("Không có câu phụ đề kế tiếp để gộp")
            return
        }

        viewModelScope.launch {
            recordUndoSnapshot()
            val combinedText = "${selected.text.trim()} ${nextSeg.text.trim()}".trim()
            val combinedSecondary = listOf(selected.secondaryText.trim(), nextSeg.secondaryText.trim())
                .filter { it.isNotEmpty() }
                .joinToString(" ")
            val newEnd = max(selected.endMs, nextSeg.endMs)
            val existingWords = selected.parsedWords() + nextSeg.parsedWords()
            val redistributed = distributeWordsEvenly(combinedText, selected.startMs, newEnd, existingWords)

            repository.saveSegment(
                selected.copy(
                    endMs = newEnd,
                    text = combinedText,
                    secondaryText = combinedSecondary,
                    wordsSerialized = redistributed.serializeWords()
                )
            )
            repository.deleteSegment(nextSeg.id)
            showStatus("Đã gộp 2 câu phụ đề liền kề")
        }
    }

    fun duplicateSelectedSegment() {
        val currentList = segments.value
        val selected = currentList.firstOrNull { it.id == _selectedSegmentId.value }
            ?: currentList.firstOrNull { _playheadMs.value in it.startMs..it.endMs }
            ?: return

        viewModelScope.launch {
            recordUndoSnapshot()
            val dur = selected.durationMs
            val newStart = selected.endMs + 80L
            val newEnd = newStart + dur
            val newWords = distributeWordsEvenly(selected.text, newStart, newEnd, selected.parsedWords())
            repository.saveSegment(
                selected.copy(
                    id = 0L,
                    startMs = newStart,
                    endMs = newEnd,
                    wordsSerialized = newWords.serializeWords()
                )
            )
            extendProjectDurationIfNeeded(newEnd)
            showStatus("Đã nhân bản câu phụ đề")
        }
    }

    fun deleteSelectedSegment() {
        val currentList = segments.value
        val selected = currentList.firstOrNull { it.id == _selectedSegmentId.value }
            ?: currentList.firstOrNull { _playheadMs.value in it.startMs..it.endMs }
            ?: return

        viewModelScope.launch {
            recordUndoSnapshot()
            repository.deleteSegment(selected.id)
            _selectedSegmentId.value = null
            showStatus("Đã xóa câu phụ đề")
        }
    }

    fun updateSegmentContent(
        segment: CaptionSegmentEntity,
        newText: String,
        newSecondaryText: String,
        newSpeaker: String,
        newStartMs: Long,
        newEndMs: Long
    ) {
        val safeStart = max(0L, newStartMs)
        val safeEnd = max(safeStart + 300L, newEndMs)
        viewModelScope.launch {
            recordUndoSnapshot()
            val updatedWords = distributeWordsEvenly(newText, safeStart, safeEnd, segment.parsedWords())
            repository.saveSegment(
                segment.copy(
                    text = newText.ifBlank { "..." },
                    secondaryText = newSecondaryText,
                    speakerTag = newSpeaker.ifBlank { "Người nói 1" },
                    startMs = safeStart,
                    endMs = safeEnd,
                    wordsSerialized = updatedWords.serializeWords()
                )
            )
            extendProjectDurationIfNeeded(safeEnd)
        }
    }

    fun nudgeSegmentTiming(segment: CaptionSegmentEntity, deltaStartMs: Long, deltaEndMs: Long) {
        val newStart = max(0L, segment.startMs + deltaStartMs)
        val newEnd = max(newStart + 300L, segment.endMs + deltaEndMs)
        viewModelScope.launch {
            val updatedWords = distributeWordsEvenly(segment.text, newStart, newEnd, segment.parsedWords())
            repository.saveSegment(
                segment.copy(
                    startMs = newStart,
                    endMs = newEnd,
                    wordsSerialized = updatedWords.serializeWords()
                )
            )
            extendProjectDurationIfNeeded(newEnd)
        }
    }

    fun toggleWordEmphasis(segment: CaptionSegmentEntity, wordIndex: Int) {
        val words = segment.parsedWords().toMutableList()
        if (wordIndex !in words.indices) return
        val target = words[wordIndex]
        words[wordIndex] = target.copy(isEmphasized = !target.isEmphasized)
        viewModelScope.launch {
            repository.saveSegment(segment.copy(wordsSerialized = words.serializeWords()))
        }
    }

    fun shiftAllSegments(offsetMs: Long) {
        val projectId = _activeProjectId.value ?: return
        val current = segments.value
        if (current.isEmpty()) return
        viewModelScope.launch {
            recordUndoSnapshot()
            val shifted = current.map { seg ->
                val newStart = max(0L, seg.startMs + offsetMs)
                val newEnd = max(newStart + 300L, seg.endMs + offsetMs)
                val words = distributeWordsEvenly(seg.text, newStart, newEnd, seg.parsedWords())
                seg.copy(
                    startMs = newStart,
                    endMs = newEnd,
                    wordsSerialized = words.serializeWords()
                )
            }
            repository.replaceAllSegments(projectId, shifted)
            val maxEnd = shifted.maxOfOrNull { it.endMs } ?: 10000L
            extendProjectDurationIfNeeded(maxEnd)
            val sign = if (offsetMs >= 0) "+${offsetMs}ms" else "${offsetMs}ms"
            showStatus("Đã dịch chuyển toàn bộ ${shifted.size} câu phụ đề ($sign)")
        }
    }

    fun importSrtOrVttContent(rawContent: String, fileName: String) {
        val projectId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val parsed = SubtitleParserExporter.parseSrtOrVtt(rawContent, projectId)
            if (parsed.isEmpty()) {
                showStatus("Không tìm thấy dữ liệu phụ đề SRT/VTT hợp lệ trong $fileName")
                return@launch
            }
            recordUndoSnapshot()
            repository.replaceAllSegments(projectId, parsed)
            val maxEnd = parsed.maxOfOrNull { it.endMs } ?: 12000L
            extendProjectDurationIfNeeded(maxEnd)
            _playheadMs.value = parsed.first().startMs
            showStatus("Đã nhập ${parsed.size} câu phụ đề từ $fileName")
        }
    }

    fun applySmartScriptSync(
        rawScript: String,
        cadence: ScriptCadenceMode,
        appendToExisting: Boolean,
        autoHighlight: Boolean
    ) {
        val projectId = _activeProjectId.value ?: return
        if (rawScript.isBlank()) return
        viewModelScope.launch {
            recordUndoSnapshot()
            val existing = if (appendToExisting) segments.value else emptyList()
            val startOffset = if (appendToExisting && existing.isNotEmpty()) {
                existing.maxOf { it.endMs } + 200L
            } else {
                0L
            }
            val generated = SubtitleParserExporter.autoSyncScript(
                rawScript = rawScript,
                projectId = projectId,
                startOffsetMs = startOffset,
                cadence = cadence,
                autoHighlightKeywords = autoHighlight
            )
            if (generated.isEmpty()) return@launch
            val combined = existing + generated
            repository.replaceAllSegments(projectId, combined)
            val maxEnd = combined.maxOfOrNull { it.endMs } ?: 12000L
            extendProjectDurationIfNeeded(maxEnd)
            _playheadMs.value = generated.first().startMs
            showStatus("Đã tự động căn giờ ${generated.size} câu phụ đề (${cadence.label})")
        }
    }

    fun appendVoiceDictatedText(spokenText: String) {
        val projectId = _activeProjectId.value ?: return
        val clean = spokenText.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            recordUndoSnapshot()
            val start = _playheadMs.value
            val generated = SubtitleParserExporter.autoSyncScript(
                rawScript = clean,
                projectId = projectId,
                startOffsetMs = start,
                cadence = ScriptCadenceMode.SOCIAL_BALANCED,
                autoHighlightKeywords = true
            )
            for (seg in generated) {
                repository.saveSegment(seg)
            }
            val newEnd = generated.maxOfOrNull { it.endMs } ?: (start + 2500L)
            extendProjectDurationIfNeeded(newEnd)
            _playheadMs.value = newEnd + 100L
            showStatus("Đã thêm ${generated.size} câu phụ đề từ giọng nói")
        }
    }

    // --- Cài đặt Khung hình & Kiểu chữ ---

    fun attachVideoUri(uriString: String) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.updateProject(proj.copy(videoUri = uriString))
            showStatus("Đã tải video từ thiết bị lên khung trình chiếu")
        }
    }

    fun clearVideoUri() {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.updateProject(proj.copy(videoUri = null))
            showStatus("Đã chuyển sang phông nền Studio")
        }
    }

    fun cycleAspectRatio() {
        val proj = activeProject.value ?: return
        val next = proj.aspectRatioMode.next()
        viewModelScope.launch {
            repository.updateProject(proj.copy(aspectRatio = next.name))
        }
    }

    fun setBackdrop(backdrop: CanvasBackdrop) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.updateProject(proj.copy(backdrop = backdrop.name, videoUri = null))
        }
    }

    fun toggleSafeZones() {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.updateProject(proj.copy(showSafeZones = !proj.showSafeZones))
        }
    }

    fun toggleBilingual() {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.updateProject(proj.copy(showBilingual = !proj.showBilingual))
        }
    }

    fun updateVerticalPosition(ratio: Float) {
        val proj = activeProject.value ?: return
        val clamped = ratio.coerceIn(0.14f, 0.88f)
        viewModelScope.launch {
            repository.updateProject(proj.copy(verticalPositionRatio = clamped))
        }
    }

    fun applyStylePreset(preset: StylePresetEntity) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.updateProject(
                proj.copy(
                    fontOption = preset.fontOption,
                    fontSizeSp = preset.fontSizeSp,
                    textColorHex = preset.textColorHex,
                    activeWordColorHex = preset.activeWordColorHex,
                    strokeColorHex = preset.strokeColorHex,
                    strokeWidthDp = preset.strokeWidthDp,
                    shadowBlur = preset.shadowBlur,
                    bgColorHex = preset.bgColorHex,
                    bgOpacity = preset.bgOpacity,
                    bgCornerRadiusDp = preset.bgCornerRadiusDp,
                    isUppercase = preset.isUppercase,
                    animationMode = preset.animationMode
                )
            )
            showStatus("Đã áp dụng mẫu kiểu chữ \"${preset.name}\"")
        }
    }

    fun saveCurrentStyleAsPreset(name: String) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.saveCustomPreset(name, proj)
            showStatus("Đã lưu kiểu chữ \"$name\" vào bộ sưu tập")
        }
    }

    fun deleteCustomPreset(presetId: Long) {
        viewModelScope.launch {
            repository.deleteCustomPreset(presetId)
            showStatus("Đã xóa mẫu kiểu chữ tùy chỉnh")
        }
    }

    fun updateStyleProperty(transform: (SubtitleProjectEntity) -> SubtitleProjectEntity) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            repository.updateProject(transform(proj))
        }
    }

    fun setFontOption(font: SubtitleFontOption) = updateStyleProperty { it.copy(fontOption = font.name) }
    fun setAnimationMode(mode: WordAnimationMode) = updateStyleProperty { it.copy(animationMode = mode.name) }

    fun generateExportPreview(): String {
        val proj = activeProject.value ?: return ""
        return SubtitleParserExporter.exportSubtitles(
            project = proj,
            segments = segments.value,
            format = _selectedExportFormat.value
        )
    }

    // --- Audio Waveform Extraction & Vocal Alignment ---

    fun extractRealWaveformFromVideoIfAvailable(context: Context) {
        val proj = activeProject.value ?: return
        val segs = segments.value
        val dur = computeProjectDuration()
        viewModelScope.launch {
            val extracted = AudioWaveformExtractor.extractWaveform(
                context = context,
                videoUriString = proj.videoUri,
                fallbackDurationMs = dur,
                segments = segs,
                binDurationMs = 50L
            )
            _audioWaveform.value = extracted
        }
    }

    fun snapSelectedSegmentToWaveform() {
        val wf = _audioWaveform.value ?: return
        val currentList = segments.value
        val selected = currentList.firstOrNull { it.id == _selectedSegmentId.value }
            ?: currentList.firstOrNull { _playheadMs.value in it.startMs..it.endMs }
            ?: return

        viewModelScope.launch {
            recordUndoSnapshot()
            val snapped = AudioWaveformExtractor.snapSegmentToNearestSpeechRegion(selected, wf)
            repository.saveSegment(snapped)
            extendProjectDurationIfNeeded(snapped.endMs)
            showStatus("Đã khớp câu phụ đề vào đỉnh sóng âm giọng nói (${com.example.data.formatTimecodeShort(snapped.startMs)})")
        }
    }

    fun autoAlignAllSubtitlesToWaveform() {
        val projectId = _activeProjectId.value ?: return
        val wf = _audioWaveform.value ?: return
        val currentList = segments.value
        if (currentList.isEmpty()) return

        viewModelScope.launch {
            recordUndoSnapshot()
            val aligned = AudioWaveformExtractor.autoAlignAllSegmentsToWaveform(currentList, wf)
            repository.replaceAllSegments(projectId, aligned)
            val maxEnd = aligned.maxOfOrNull { it.endMs } ?: 12000L
            extendProjectDurationIfNeeded(maxEnd)
            val newScore = AudioWaveformExtractor.computeAlignmentScore(aligned, wf)
            showStatus("Đã tự động khớp ${aligned.size} câu phụ đề với sóng âm (Độ khớp: $newScore%)")
        }
    }

    fun setSelectedSegmentBoundaryAtPlayhead(isStartBoundary: Boolean) {
        val currentList = segments.value
        val selected = currentList.firstOrNull { it.id == _selectedSegmentId.value }
            ?: currentList.firstOrNull { _playheadMs.value in it.startMs..it.endMs }
            ?: currentList.minByOrNull { kotlin.math.abs(it.startMs - _playheadMs.value) }
            ?: return

        val cursor = _playheadMs.value
        val newStart = if (isStartBoundary) cursor.coerceAtLeast(0L) else selected.startMs
        val newEnd = if (isStartBoundary) max(newStart + 400L, selected.endMs) else max(selected.startMs + 400L, cursor)

        viewModelScope.launch {
            recordUndoSnapshot()
            val updatedWords = distributeWordsEvenly(selected.text, newStart, newEnd, selected.parsedWords())
            repository.saveSegment(
                selected.copy(
                    startMs = newStart,
                    endMs = newEnd,
                    wordsSerialized = updatedWords.serializeWords()
                )
            )
            extendProjectDurationIfNeeded(newEnd)
            val label = if (isStartBoundary) "điểm bắt đầu" else "điểm kết thúc"
            showStatus("Đã đặt $label câu phụ đề tại kim sóng âm ${com.example.data.formatTimecodeShort(cursor)}")
        }
    }

    // --- Export Video with Hardcoded Subtitles to User's Device Gallery ---

    fun exportHardcodedVideoToGallery(
        context: Context,
        quality: VideoExportQuality = VideoExportQuality.HD_720P,
        burnKaraokeHighlight: Boolean = true,
        burnBilingualLine: Boolean = true
    ) {
        val proj = activeProject.value ?: return
        val currentSegments = segments.value
        if (_videoExportProgress.value.isExporting) return

        viewModelScope.launch {
            val finalProgress = HardcodedVideoExporter.exportVideoWithHardcodedSubtitlesToGallery(
                context = context,
                project = proj,
                segments = currentSegments,
                quality = quality,
                burnKaraokeHighlight = burnKaraokeHighlight,
                burnBilingualLine = burnBilingualLine,
                onProgress = { progressUpdate ->
                    _videoExportProgress.value = progressUpdate
                }
            )
            showStatus("Đã lưu Video MP4 kèm phụ đề cứng vào Bộ sưu tập (${finalProgress.finishedDisplayPath})!")
        }
    }

    // --- AI-Powered Voice Recognition with Timestamps using Gemini API (`gemini-3.5-transcribe`) ---

    fun transcribeVideoOrAudioWithGemini(
        context: Context,
        rawAudioWavBytes: ByteArray? = null,
        spokenLanguageHint: String = "Tự động phát hiện",
        translateToVietnamese: Boolean = true,
        appendToExisting: Boolean = false
    ) {
        val projectId = _activeProjectId.value ?: return
        val proj = activeProject.value
        if (_isAiTranscribing.value) return

        _isAiTranscribing.value = true
        viewModelScope.launch {
            try {
                recordUndoSnapshot()
                val (transcribedSegments, statusNote) = GeminiAiRepository.transcribeVideoOrAudioWithTimestamps(
                    context = context,
                    projectId = projectId,
                    videoUriString = proj?.videoUri,
                    rawAudioWavBytes = rawAudioWavBytes,
                    spokenLanguageHint = spokenLanguageHint,
                    translateToVietnamese = translateToVietnamese
                )
                if (transcribedSegments.isNotEmpty()) {
                    val existing = if (appendToExisting) segments.value else emptyList()
                    val offset = if (appendToExisting && existing.isNotEmpty()) {
                        existing.maxOf { it.endMs } + 160L
                    } else {
                        0L
                    }
                    val shiftedNew = if (offset > 0L) {
                        transcribedSegments.map { seg ->
                            val s = seg.startMs + offset
                            val e = seg.endMs + offset
                            val w = distributeWordsEvenly(seg.text, s, e, seg.parsedWords())
                            seg.copy(startMs = s, endMs = e, wordsSerialized = w.serializeWords())
                        }
                    } else {
                        transcribedSegments
                    }
                    val combined = existing + shiftedNew
                    repository.replaceAllSegments(projectId, combined)
                    val maxEnd = combined.maxOfOrNull { it.endMs } ?: 12000L
                    extendProjectDurationIfNeeded(maxEnd)
                    _playheadMs.value = shiftedNew.first().startMs
                }
                showStatus(statusNote)
            } finally {
                _isAiTranscribing.value = false
            }
        }
    }

    private fun computeProjectDuration(): Long {
        val projDur = activeProject.value?.durationMs ?: 15000L
        val maxSegEnd = segments.value.maxOfOrNull { it.endMs } ?: 0L
        return max(projDur, maxSegEnd + 500L)
    }

    private suspend fun extendProjectDurationIfNeeded(newEndMs: Long) {
        val proj = activeProject.value ?: return
        if (newEndMs + 500L > proj.durationMs) {
            repository.updateProject(proj.copy(durationMs = newEndMs + 1000L))
        }
    }

    class Factory(private val repository: SubCutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudioViewModel(repository) as T
        }
    }
}
