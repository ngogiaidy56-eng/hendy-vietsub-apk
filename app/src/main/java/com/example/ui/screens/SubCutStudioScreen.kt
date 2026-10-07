package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CanvasBackdrop
import com.example.data.CaptionSegmentEntity
import com.example.data.LiveTranslationEngine
import com.example.data.ScriptCadenceMode
import com.example.data.TranslationLanguage
import com.example.ui.components.BatchScriptPanel
import com.example.ui.components.CapCutAutoCaptionsSheet
import com.example.ui.components.EditCaptionDialog
import com.example.ui.components.ExportPanel
import com.example.ui.components.LiveVoiceDictationDialog
import com.example.ui.components.NewProjectDialog
import com.example.ui.components.SaveCustomPresetDialog
import com.example.ui.components.SmartScriptSyncDialog
import com.example.ui.components.StyleStudioPanel
import com.example.ui.components.TimelineTrackView
import com.example.ui.components.VideoPreviewStage
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KineticLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.viewmodel.StudioBottomTab
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

@Composable
fun SubCutStudioScreen(
    viewModel: StudioViewModel,
    initialExternalTextToTranslate: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val segments by viewModel.segments.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val isProjectHubOpen by viewModel.isProjectHubOpen.collectAsStateWithLifecycle()
    val activeBottomTab by viewModel.activeBottomTab.collectAsStateWithLifecycle()
    val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playheadMs.let { viewModel.isPlaying.collectAsStateWithLifecycle() }
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val selectedSegmentId by viewModel.selectedSegmentId.collectAsStateWithLifecycle()
    val timelineZoom by viewModel.timelineZoom.collectAsStateWithLifecycle()
    val selectedExportFormat by viewModel.selectedExportFormat.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    // Modal & Sub-mode States
    var editingSegment by remember { mutableStateOf<CaptionSegmentEntity?>(null) }
    var showSmartScriptModal by remember { mutableStateOf(false) }
    var showVoiceDictationModal by remember { mutableStateOf(false) }
    var showSavePresetModal by remember { mutableStateOf(false) }
    var showNewProjectModal by remember { mutableStateOf(false) }
    var showAutoCaptionsSheet by remember { mutableStateOf(false) }
    var showSubtitleSubToolbar by remember { mutableStateOf(true) }
    var showFloatingBlackPillInEditor by remember { mutableStateOf(false) }
    var showLiveWebTranslateScreen by remember {
        mutableStateOf(!initialExternalTextToTranslate.isNullOrBlank())
    }
    var pendingExportTextToSave by remember { mutableStateOf("") }

    LaunchedEffect(initialExternalTextToTranslate) {
        if (!initialExternalTextToTranslate.isNullOrBlank()) {
            showLiveWebTranslateScreen = true
        }
    }

    // 1. Zero-Permission Android Photo Picker for Local Device Video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.attachVideoUri(uri.toString())
            viewModel.closeProjectHub()
        }
    }

    // 2. Zero-Permission SAF OpenDocument for Importing .SRT / .VTT Files
    val importSrtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "subtitles.srt"
                    viewModel.importSrtOrVttContent(content, name)
                    viewModel.closeProjectHub()
                }
            }.onFailure {
                viewModel.showStatus("Không thể đọc file: ${it.localizedMessage}")
            }
        }
    }

    // 3. Zero-Permission SAF CreateDocument for Saving .SRT / .VTT / .ASS / .TXT Files
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && pendingExportTextToSave.isNotBlank()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(pendingExportTextToSave)
                }
                viewModel.showStatus("Đã lưu file phụ đề vào bộ nhớ thiết bị!")
            }.onFailure {
                viewModel.showStatus("Không thể lưu file: ${it.localizedMessage}")
            }
        }
    }

    // Full-screen Real-Time Website & Cross-App Translator Mode
    if (showLiveWebTranslateScreen) {
        WebAppLiveTranslateScreen(
            initialSharedTextOrUrl = initialExternalTextToTranslate,
            onSendToSubtitleTimeline = { translatedText ->
                viewModel.appendVoiceDictatedText(translatedText)
                showLiveWebTranslateScreen = false
                viewModel.closeProjectHub()
            },
            onClose = { showLiveWebTranslateScreen = false }
        )
        return
    }

    // Handle Back button inside Editor to return to CapCut Home Screen
    if (!isProjectHubOpen) {
        BackHandler {
            if (activeBottomTab != StudioBottomTab.TIMELINE) {
                viewModel.setBottomTab(StudioBottomTab.TIMELINE)
            } else {
                viewModel.openProjectHub()
            }
        }
    }

    if (isProjectHubOpen || activeProject == null) {
        ProjectHubScreen(
            projects = projects,
            presets = presets,
            activeProjectId = activeProject?.id,
            onSelectProject = { viewModel.selectProject(it) },
            onDeleteProject = { viewModel.deleteProject(it) },
            onCloseHub = { viewModel.closeProjectHub() },
            onOpenNewProjectModal = { showNewProjectModal = true },
            onPickVideoForCurrentOrNew = {
                videoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onImportSrtFile = {
                importSrtLauncher.launch(arrayOf("*/*"))
            },
            onOpenSmartScript = {
                viewModel.closeProjectHub()
                showSmartScriptModal = true
            },
            onOpenAutoCaptionsEditor = {
                viewModel.closeProjectHub()
                showAutoCaptionsSheet = true
            },
            onOpenLiveWebTranslate = {
                showLiveWebTranslateScreen = true
            },
            onApplyPresetAndOpenEditor = { preset ->
                viewModel.applyStylePreset(preset)
                viewModel.closeProjectHub()
            }
        )
    } else {
        val currentProject = activeProject!!
        val activeSegmentUnderPlayhead = segments.firstOrNull { playheadMs in it.startMs..it.endMs }
        val selectedSegment = segments.firstOrNull { it.id == selectedSegmentId } ?: activeSegmentUnderPlayhead

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E0E10))
                .testTag("studio_main_scaffold"),
            containerColor = Color(0xFF0E0E10),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CapCutEditorTopBar(
                    resolutionLabel = "AI UHD ▾",
                    onCloseToHome = { viewModel.openProjectHub() },
                    onSearchScript = { viewModel.setBottomTab(StudioBottomTab.BATCH_SCRIPT) },
                    onCycleResolutionOrRatio = { viewModel.cycleAspectRatio() },
                    onExportClick = { viewModel.setBottomTab(StudioBottomTab.EXPORT) },
                    onOpenAutoCaptions = { showAutoCaptionsSheet = true }
                )
            },
            bottomBar = {
                if (showSubtitleSubToolbar) {
                    // Exact Subtitle Sub-Toolbar from Image 4 (`photo_6`)
                    CapCutSubtitleSubToolbar(
                        onBackToMainToolbar = {
                            showSubtitleSubToolbar = false
                            viewModel.setBottomTab(StudioBottomTab.TIMELINE)
                        },
                        onEnterSubtitle = { viewModel.addCaptionAtPlayhead() },
                        onOpenAutoCaptions = { showAutoCaptionsSheet = true },
                        onOpenSubtitleTemplates = { viewModel.setBottomTab(StudioBottomTab.STYLE_STUDIO) },
                        onOpenAutoLyrics = { showVoiceDictationModal = true },
                        onImportSrtFile = { importSrtLauncher.launch(arrayOf("*/*")) }
                    )
                } else {
                    // Exact Main Editor Toolbar from Image 3 (`photo_7`)
                    CapCutMainEditorToolbar(
                        onSelectEdit = { viewModel.setBottomTab(StudioBottomTab.TIMELINE) },
                        onSelectAudio = { showVoiceDictationModal = true },
                        onSelectText = {
                            showSubtitleSubToolbar = true
                            viewModel.setBottomTab(StudioBottomTab.TIMELINE)
                        },
                        onSelectEffects = { viewModel.setBottomTab(StudioBottomTab.WORD_FX) },
                        onSelectOverlay = { showFloatingBlackPillInEditor = !showFloatingBlackPillInEditor },
                        onSelectSubtitles = {
                            showSubtitleSubToolbar = true
                            showAutoCaptionsSheet = true
                        },
                        onSelectFilters = {
                            val all = CanvasBackdrop.entries
                            val next = all[(currentProject.backdropMode.ordinal + 1) % all.size]
                            viewModel.setBackdrop(next)
                        },
                        onSelectAdjust = { viewModel.setBottomTab(StudioBottomTab.STYLE_STUDIO) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Upper Half: Live Video / Canvas Stage with Floating Paper-Plane & Cyan Atom Buttons
                    Box(modifier = Modifier.weight(1f)) {
                        VideoPreviewStage(
                            project = currentProject,
                            activeSegment = activeSegmentUnderPlayhead,
                            playheadMs = playheadMs,
                            isPlaying = isPlaying,
                            onUpdateVerticalPosition = { viewModel.updateVerticalPosition(it) },
                            onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                            onToggleSafeZones = { viewModel.toggleSafeZones() },
                            onToggleBilingual = { viewModel.toggleBilingual() },
                            onPickVideo = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onCycleBackdrop = {
                                val all = CanvasBackdrop.entries
                                val next = all[(currentProject.backdropMode.ordinal + 1) % all.size]
                                viewModel.setBackdrop(next)
                            },
                            onSegmentTap = { seg -> editingSegment = seg },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Right-side Floating Blue Paper-Plane Button (from Images 4 & 7)
                        FloatingPaperPlaneBubble(
                            onClick = { showLiveWebTranslateScreen = true },
                            sizeDp = 44,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 64.dp, end = 14.dp)
                        )

                        // Optional Floating Black Pill Bar from Image 2 (`photo_8`) directly inside Editor
                        if (showFloatingBlackPillInEditor) {
                            CapCutFloatingBlackPillBar(
                                onAutoCaptureTranslate = { showAutoCaptionsSheet = true },
                                onRegionOrClipboardTranslate = { showLiveWebTranslateScreen = true },
                                onShareOrSendToTimeline = { viewModel.setBottomTab(StudioBottomTab.EXPORT) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        } else {
                            // Bottom-left glowing cyan atom button (from Images 2, 3, 4, 7)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 14.dp, bottom = 10.dp)
                                    .size(38.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFF1E242B))
                                    .border(
                                        1.dp,
                                        Color(0xFF00E5FF).copy(alpha = 0.4f),
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { showFloatingBlackPillInEditor = true }
                                    .testTag("editor_atom_pill_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Bật thanh nổi AI",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Lower Half: CapCut Multi-Track Timeline or Active Studio Inspector
                    when (activeBottomTab) {
                        StudioBottomTab.TIMELINE -> {
                            TimelineTrackView(
                                segments = segments,
                                selectedSegment = selectedSegment,
                                playheadMs = playheadMs,
                                totalDurationMs = currentProject.durationMs,
                                isPlaying = isPlaying,
                                playbackSpeed = playbackSpeed,
                                timelineZoom = timelineZoom,
                                canUndo = canUndo,
                                canRedo = canRedo,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSeekTo = { viewModel.seekTo(it) },
                                onCycleSpeed = { viewModel.cyclePlaybackSpeed() },
                                onUndo = { viewModel.undo() },
                                onRedo = { viewModel.redo() },
                                onZoomDelta = { viewModel.adjustTimelineZoom(it) },
                                onSelectSegment = { viewModel.selectSegment(it) },
                                onAddCaption = { viewModel.addCaptionAtPlayhead() },
                                onSplitAtPlayhead = { viewModel.splitAtPlayhead() },
                                onMergeWithNext = { viewModel.mergeWithNext() },
                                onDuplicateSegment = { viewModel.duplicateSelectedSegment() },
                                onDeleteSegment = { viewModel.deleteSelectedSegment() },
                                onNudgeTiming = { seg, ds, de -> viewModel.nudgeSegmentTiming(seg, ds, de) },
                                onToggleWordEmphasis = { seg, idx -> viewModel.toggleWordEmphasis(seg, idx) },
                                onOpenEditModal = { seg -> editingSegment = seg },
                                onOpenVoiceModal = { showVoiceDictationModal = true },
                                onOpenSmartScriptModal = { showAutoCaptionsSheet = true }
                            )
                        }

                        StudioBottomTab.BATCH_SCRIPT -> {
                            BatchScriptPanel(
                                segments = segments,
                                selectedSegmentId = selectedSegment?.id,
                                playheadMs = playheadMs,
                                onSelectAndSeek = { seg -> viewModel.selectSegment(seg, movePlayhead = true) },
                                onEditSegment = { seg -> editingSegment = seg },
                                onShiftAllSegments = { offset -> viewModel.shiftAllSegments(offset) },
                                onAddCue = { viewModel.addCaptionAtPlayhead() },
                                onOpenSmartScript = { showSmartScriptModal = true }
                            )
                        }

                        StudioBottomTab.STYLE_STUDIO, StudioBottomTab.WORD_FX -> {
                            StyleStudioPanel(
                                project = currentProject,
                                presets = presets,
                                showWordFxFirst = activeBottomTab == StudioBottomTab.WORD_FX,
                                onApplyPreset = { viewModel.applyStylePreset(it) },
                                onOpenSavePresetModal = { showSavePresetModal = true },
                                onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) },
                                onSelectFont = { viewModel.setFontOption(it) },
                                onSelectAnimationMode = { viewModel.setAnimationMode(it) },
                                onUpdateProjectStyle = { transform -> viewModel.updateStyleProperty(transform) }
                            )
                        }

                        StudioBottomTab.EXPORT -> {
                            val exportPreview = remember(currentProject, segments, selectedExportFormat) {
                                viewModel.generateExportPreview()
                            }
                            ExportPanel(
                                selectedFormat = selectedExportFormat,
                                exportedPreviewText = exportPreview,
                                onSelectFormat = { viewModel.setExportFormat(it) },
                                onSaveToDevice = { format, text ->
                                    pendingExportTextToSave = text
                                    val cleanTitle = currentProject.title
                                        .lowercase()
                                        .replace(Regex("[^a-z0-9]+"), "_")
                                        .trim('_')
                                        .ifBlank { "hendy_vietsub" }
                                    saveFileLauncher.launch("$cleanTitle.${format.extension}")
                                },
                                onCopyToClipboard = { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Hendy Vietsub", text))
                                    viewModel.showStatus("Đã sao chép phụ đề .${selectedExportFormat.extension.uppercase()} vào bộ nhớ tạm!")
                                },
                                onShareScript = { format, text ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "${currentProject.title}.${format.extension}")
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ phụ đề"))
                                },
                                onImportSrtFile = {
                                    importSrtLauncher.launch(arrayOf("*/*"))
                                }
                            )
                        }
                    }
                }

                // Floating Toast / Status Banner
                AnimatedVisibility(
                    visible = statusBannerMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                ) {
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .border(1.dp, ElectricCyan, RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = statusBannerMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KineticLime,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }

    // --- Active Dialogs & Sheets ---
    if (showAutoCaptionsSheet) {
        CapCutAutoCaptionsSheet(
            presets = presets,
            onDismiss = { showAutoCaptionsSheet = false },
            onGenerateCaptions = { _, spokenLang, chosenPreset, customText, translateToVi ->
                if (chosenPreset != null) {
                    viewModel.applyStylePreset(chosenPreset)
                }
                scope.launch {
                    val finalScript = if (translateToVi && spokenLang != TranslationLanguage.VIETNAMESE) {
                        LiveTranslationEngine.translateText(
                            rawText = customText,
                            sourceLang = spokenLang,
                            targetLang = TranslationLanguage.VIETNAMESE
                        )
                    } else {
                        customText
                    }
                    viewModel.applySmartScriptSync(
                        rawScript = finalScript,
                        cadence = ScriptCadenceMode.VIRAL_FAST,
                        appendToExisting = false,
                        autoHighlight = true
                    )
                }
            },
            onOpenVoiceRecognizer = { showVoiceDictationModal = true },
            onOpenLiveWebTranslate = { showLiveWebTranslateScreen = true }
        )
    }

    editingSegment?.let { seg ->
        EditCaptionDialog(
            segment = seg,
            onDismiss = { editingSegment = null },
            onSave = { primary, secondary, speaker, start, end ->
                viewModel.updateSegmentContent(seg, primary, secondary, speaker, start, end)
            }
        )
    }

    if (showSmartScriptModal) {
        SmartScriptSyncDialog(
            onDismiss = { showSmartScriptModal = false },
            onConfirmSync = { rawScript, cadence, append, highlight ->
                viewModel.applySmartScriptSync(rawScript, cadence, append, highlight)
            }
        )
    }

    if (showVoiceDictationModal) {
        LiveVoiceDictationDialog(
            playheadMs = playheadMs,
            onDismiss = { showVoiceDictationModal = false },
            onInsertDictatedText = { text -> viewModel.appendVoiceDictatedText(text) }
        )
    }

    if (showSavePresetModal) {
        SaveCustomPresetDialog(
            onDismiss = { showSavePresetModal = false },
            onSavePreset = { name -> viewModel.saveCurrentStyleAsPreset(name) }
        )
    }

    if (showNewProjectModal) {
        NewProjectDialog(
            onDismiss = { showNewProjectModal = false },
            onCreateProject = { title, ratio, backdrop ->
                viewModel.createNewProject(title, ratio, backdrop)
            }
        )
    }
}

/**
 * Reproduces the exact Dark Editor Top Bar from Images 4 & 7 (`✕`, `🔍`, `💎 Dùng thử`, `AI UHD ▾`, `Xuất`).
 */
@Composable
private fun CapCutEditorTopBar(
    resolutionLabel: String,
    onCloseToHome: () -> Unit,
    onSearchScript: () -> Unit,
    onCycleResolutionOrRatio: () -> Unit,
    onExportClick: () -> Unit,
    onOpenAutoCaptions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E0E10))
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: ✕ and 🔍
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onCloseToHome,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("open_projects_hub_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Đóng về trang chủ",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onSearchScript,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm phụ đề",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        // Right: [💎 Dùng thử] [AI UHD ▾] [Xuất]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color(0xFF1F2128),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable(onClick = onOpenAutoCaptions)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = Color(0xFF00D8F6),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Dùng thử",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Surface(
                color = Color(0xFF1F2128),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable(onClick = onCycleResolutionOrRatio)
            ) {
                Text(
                    text = resolutionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }

            Surface(
                color = Color(0xFF00C8E0),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable(onClick = onExportClick)
                    .testTag("top_export_btn")
            ) {
                Text(
                    text = "Xuất",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0E0E10),
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Reproduces the exact Main Editor Toolbar from Image 3 (`photo_7`):
 * Chỉnh sửa | Âm thanh | Văn bản | Hiệu ứng | Lớp phủ | Phụ đề | Bộ lọc | Tuỳ chỉnh
 */
@Composable
private fun CapCutMainEditorToolbar(
    onSelectEdit: () -> Unit,
    onSelectAudio: () -> Unit,
    onSelectText: () -> Unit,
    onSelectEffects: () -> Unit,
    onSelectOverlay: () -> Unit,
    onSelectSubtitles: () -> Unit,
    onSelectFilters: () -> Unit,
    onSelectAdjust: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111216))
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CapCutBottomToolItem(Icons.Default.ContentCut, "Chỉnh sửa", onSelectEdit)
        CapCutBottomToolItem(Icons.Default.MusicNote, "Âm thanh", onSelectAudio)
        CapCutBottomToolItem(Icons.Default.TextFields, "Văn bản", onSelectText)
        CapCutBottomToolItem(Icons.Default.AutoAwesome, "Hiệu ứng", onSelectEffects)
        CapCutBottomToolItem(Icons.Default.Layers, "Lớp phủ", onSelectOverlay)
        CapCutBottomToolItem(Icons.Default.Subtitles, "Phụ đề", onSelectSubtitles)
        CapCutBottomToolItem(Icons.Default.BlurOn, "Bộ lọc", onSelectFilters)
        CapCutBottomToolItem(Icons.Default.Tune, "Tuỳ chỉnh", onSelectAdjust)
    }
}

/**
 * Reproduces the exact Subtitle Sub-Toolbar from Image 4 (`photo_6`):
 * [<] | Nhập phụ đề | Phụ đề tự động | Mẫu phụ đề | Lời bài hát tự động | Nhập phụ đề
 */
@Composable
private fun CapCutSubtitleSubToolbar(
    onBackToMainToolbar: () -> Unit,
    onEnterSubtitle: () -> Unit,
    onOpenAutoCaptions: () -> Unit,
    onOpenSubtitleTemplates: () -> Unit,
    onOpenAutoLyrics: () -> Unit,
    onImportSrtFile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111216))
            .navigationBarsPadding()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left "<" Back box button from Image 4
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF23252C))
                .clickable(onClick = onBackToMainToolbar)
                .testTag("subtitle_toolbar_back_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Quay lại thanh công cụ chính",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapCutBottomToolItem(Icons.Default.AddBox, "Nhập phụ đề", onEnterSubtitle)
            CapCutBottomToolItem(Icons.Default.ClosedCaption, "Phụ đề tự động", onOpenAutoCaptions)
            CapCutBottomToolItem(Icons.Default.Subtitles, "Mẫu phụ đề", onOpenSubtitleTemplates)
            CapCutBottomToolItem(Icons.Default.MusicNote, "Lời bài hát tự\nđộng", onOpenAutoLyrics)
            CapCutBottomToolItem(Icons.Default.FolderOpen, "Nhập phụ đề", onImportSrtFile)
        }
    }
}

@Composable
private fun CapCutBottomToolItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = Color(0xFFD1D5DB),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
