package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PhotoCameraBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StylePresetEntity
import com.example.data.SubtitleProjectEntity
import com.example.data.formatTimecodeShort

private enum class CapCutHomeBottomTab(val label: String) {
    EDIT("Chỉnh sửa"),
    TEMPLATES("Mẫu"),
    AI_LAB("AI Lab"),
    INBOX("Hộp thư đến"),
    ME("Tôi")
}

@Composable
fun ProjectHubScreen(
    projects: List<SubtitleProjectEntity>,
    presets: List<StylePresetEntity>,
    activeProjectId: Long?,
    onSelectProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onCloseHub: () -> Unit,
    onOpenNewProjectModal: () -> Unit,
    onPickVideoForCurrentOrNew: () -> Unit,
    onImportSrtFile: () -> Unit,
    onOpenSmartScript: () -> Unit,
    onOpenAutoCaptionsEditor: () -> Unit,
    onOpenLiveWebTranslate: () -> Unit,
    onApplyPresetAndOpenEditor: (StylePresetEntity) -> Unit
) {
    var currentMainTab by remember { mutableStateOf(CapCutHomeBottomTab.EDIT) }
    var showAllToolsSheet by remember { mutableStateOf(false) }

    BackHandler {
        when {
            showAllToolsSheet -> showAllToolsSheet = false
            currentMainTab != CapCutHomeBottomTab.EDIT -> currentMainTab = CapCutHomeBottomTab.EDIT
            else -> onCloseHub()
        }
    }

    if (showAllToolsSheet) {
        AllToolsCapCutScreen(
            onClose = { showAllToolsSheet = false },
            onNewProject = {
                showAllToolsSheet = false
                onOpenNewProjectModal()
            },
            onOpenAutoCaptions = {
                showAllToolsSheet = false
                onOpenAutoCaptionsEditor()
            },
            onOpenLiveTranslator = {
                showAllToolsSheet = false
                onOpenLiveWebTranslate()
            },
            onPickVideo = {
                showAllToolsSheet = false
                onPickVideoForCurrentOrNew()
            },
            onOpenSmartScript = {
                showAllToolsSheet = false
                onOpenSmartScript()
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("project_hub_screen"),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            CapCutMainBottomBar(
                selectedTab = currentMainTab,
                onSelectTab = { tab -> currentMainTab = tab }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (currentMainTab) {
                CapCutHomeBottomTab.EDIT -> {
                    CapCutEditHomeTabContent(
                        projects = projects,
                        activeProjectId = activeProjectId,
                        onNewVideoClick = onOpenNewProjectModal,
                        onPickVideoClick = onPickVideoForCurrentOrNew,
                        onAutoCaptionsClick = onOpenAutoCaptionsEditor,
                        onOpenAllToolsClick = { showAllToolsSheet = true },
                        onOpenLiveWebTranslateClick = onOpenLiveWebTranslate,
                        onOpenSmartScriptClick = onOpenSmartScript,
                        onImportSrtClick = onImportSrtFile,
                        onSelectProject = onSelectProject,
                        onDeleteProject = onDeleteProject
                    )
                }

                CapCutHomeBottomTab.TEMPLATES -> {
                    CapCutTemplatesTabContent(
                        presets = presets,
                        onApplyPresetAndOpen = onApplyPresetAndOpenEditor,
                        onOpenLiveTranslate = onOpenLiveWebTranslate
                    )
                }

                CapCutHomeBottomTab.AI_LAB -> {
                    WebAppLiveTranslateScreen(
                        onSendToSubtitleTimeline = { translatedText ->
                            onCloseHub()
                        },
                        onClose = { currentMainTab = CapCutHomeBottomTab.EDIT }
                    )
                }

                CapCutHomeBottomTab.INBOX -> {
                    CapCutInboxTabContent(
                        onOpenLiveTranslate = { currentMainTab = CapCutHomeBottomTab.AI_LAB },
                        onOpenAutoCaptions = onOpenAutoCaptionsEditor
                    )
                }

                CapCutHomeBottomTab.ME -> {
                    CapCutProfileMeTabContent(
                        projectCount = projects.size,
                        onOpenAllTools = { showAllToolsSheet = true },
                        onOpenLiveTranslate = { currentMainTab = CapCutHomeBottomTab.AI_LAB },
                        onOpenAutoCaptions = onOpenAutoCaptionsEditor
                    )
                }
            }

            // Floating Blue Paper-Plane / Quick Live Translate Trigger Bubble (seen in Images 1, 4, 5, 6, 7, 8)
            if (currentMainTab != CapCutHomeBottomTab.AI_LAB) {
                FloatingPaperPlaneBubble(
                    onClick = { currentMainTab = CapCutHomeBottomTab.AI_LAB },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun CapCutEditHomeTabContent(
    projects: List<SubtitleProjectEntity>,
    activeProjectId: Long?,
    onNewVideoClick: () -> Unit,
    onPickVideoClick: () -> Unit,
    onAutoCaptionsClick: () -> Unit,
    onOpenAllToolsClick: () -> Unit,
    onOpenLiveWebTranslateClick: () -> Unit,
    onOpenSmartScriptClick: () -> Unit,
    onImportSrtClick: () -> Unit,
    onSelectProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val isCompactHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120
        }
    }
    var showEmptyIllustrationDemo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // If scrolled down, show the compact cyan button bar from Image 8 (`photo_4`)
        if (isCompactHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyanGradientHeroButton(
                    isCompact = true,
                    icon = Icons.Default.Add,
                    title = "Video mới",
                    onClick = onNewVideoClick,
                    modifier = Modifier.weight(1.75f),
                    testTag = "compact_new_video_btn"
                )
                CyanGradientHeroButton(
                    isCompact = true,
                    icon = Icons.Default.Image,
                    title = "Chỉnh sửa ảnh",
                    onClick = onPickVideoClick,
                    modifier = Modifier.weight(1f),
                    testTag = "compact_edit_photo_btn"
                )
            }
        } else {
            // Top bar with "💎 Standard" pill badge (Image 6)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF2F5F8),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.clickable(onClick = onOpenLiveWebTranslateClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = Color(0xFF00C4E0),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Standard",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1D21)
                        )
                    }
                }

                // Quick badge to open Live Web/App Translation
                Surface(
                    color = Color(0xFFE6FAFC),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .border(1.dp, Color(0xFF00C4E0).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenLiveWebTranslateClick)
                        .testTag("home_top_live_translate_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Color(0xFF0099B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Dịch trực tiếp Web/App",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007B99)
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dual Cyan Gradient Hero Cards ("Video mới" + "Chỉnh sửa ảnh") from Image 6
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyanGradientHeroButton(
                        isCompact = false,
                        icon = Icons.Default.Add,
                        title = "Video mới",
                        onClick = onNewVideoClick,
                        modifier = Modifier.weight(1.75f),
                        testTag = "hero_new_video_btn"
                    )
                    CyanGradientHeroButton(
                        isCompact = false,
                        icon = Icons.Default.Image,
                        title = "Chỉnh sửa ảnh",
                        onClick = onPickVideoClick,
                        modifier = Modifier.weight(1f),
                        testTag = "hero_edit_photo_btn"
                    )
                }
            }

            // 2. 2x4 Quick Tools Grid from Image 6
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CapCutQuickToolTile(
                            icon = Icons.Default.SmartDisplay,
                            label = "AutoCut",
                            onClick = onPickVideoClick,
                            modifier = Modifier.weight(1f)
                        )
                        CapCutQuickToolTile(
                            icon = Icons.Default.FaceRetouchingNatural,
                            label = "Làm đẹp",
                            onClick = onAutoCaptionsClick,
                            modifier = Modifier.weight(1f)
                        )
                        CapCutQuickToolTile(
                            icon = Icons.Default.Translate,
                            label = "Dịch Web/App",
                            onClick = onOpenLiveWebTranslateClick,
                            modifier = Modifier.weight(1f),
                            testTag = "grid_live_translate_btn"
                        )
                        CapCutQuickToolTile(
                            icon = Icons.Default.ClosedCaption,
                            label = "Phụ đề tự động",
                            onClick = onAutoCaptionsClick,
                            modifier = Modifier.weight(1f),
                            testTag = "grid_auto_captions_btn"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CapCutQuickToolTile(
                            icon = Icons.Default.PhotoCameraBack,
                            label = "Trình chỉnh sửa ảnh",
                            onClick = onPickVideoClick,
                            modifier = Modifier.weight(1f)
                        )
                        CapCutQuickToolTile(
                            icon = Icons.Default.Subtitles,
                            label = "Nhập file SRT",
                            onClick = onImportSrtClick,
                            modifier = Modifier.weight(1f)
                        )
                        CapCutQuickToolTile(
                            icon = Icons.Default.DesktopWindows,
                            label = "Trình chỉnh sửa trên\nmáy tính",
                            onClick = onOpenSmartScriptClick,
                            modifier = Modifier.weight(1f)
                        )
                        CapCutQuickToolTile(
                            icon = Icons.Default.GridView,
                            label = "Tất cả công cụ",
                            onClick = onOpenAllToolsClick,
                            modifier = Modifier.weight(1f),
                            testTag = "grid_all_tools_btn"
                        )
                    }
                }
            }

            // 3. "Dự án" Section Header (Images 6 & 8)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dự án",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF141619)
                    )
                    IconButton(
                        onClick = { showEmptyIllustrationDemo = !showEmptyIllustrationDemo },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = "Chế độ hiển thị dự án",
                            tint = Color(0xFF141619),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 4. Exact Empty-State Illustration from Image 6 & 8 (shown when empty or toggled)
            if (projects.isEmpty() || showEmptyIllustrationDemo) {
                item {
                    CapCutEmptyProjectsIllustration(
                        onStartCreateClick = onNewVideoClick
                    )
                }
            }

            // 5. Project List Cards (Tap any project to open the Dark CapCut Video Editor)
            items(projects, key = { it.id }) { proj ->
                val isCurrent = proj.id == activeProjectId
                Surface(
                    color = if (isCurrent) Color(0xFFF0FAFC) else Color(0xFFF6F8FA),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isCurrent) Color(0xFF00C4E0) else Color(0xFFE6E9EF),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectProject(proj.id) }
                        .testTag("project_card_${proj.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Video Thumbnail Box
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF161922), Color(0xFF283246))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = proj.aspectRatioMode.shortLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = proj.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141619),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = proj.description.ifBlank { "Dự án phụ đề Hendy Vietsub" },
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = formatTimecodeShort(proj.durationMs),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0099B8)
                                )
                                Text(
                                    text = "• ${proj.wordAnimation.label}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }

                        if (projects.size > 1) {
                            IconButton(
                                onClick = { onDeleteProject(proj.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Xóa dự án",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Reproduces the exact "Tất cả công cụ" screen from Image 1 (`photo_9`)
 * with "Thao tác nhanh", "Công cụ AI", "Chỉnh sửa ảnh", and sticky bottom "+ Dự án mới" button.
 */
@Composable
private fun AllToolsCapCutScreen(
    onClose: () -> Unit,
    onNewProject: () -> Unit,
    onOpenAutoCaptions: () -> Unit,
    onOpenLiveTranslator: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenSmartScript: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE6F7FA),
                        Color(0xFFF3FAFC),
                        Color(0xFFF8FCFD)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("all_tools_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: "Tất cả công cụ" + Paper Plane Bubble + "✕"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tất cả công cụ",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF141619)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingPaperPlaneBubble(
                        onClick = onOpenLiveTranslator,
                        sizeDp = 42
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_all_tools_btn")
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF141619)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Thao tác nhanh
                item {
                    ToolCategorySection(
                        title = "Thao tác nhanh",
                        tools = listOf(
                            ToolItemData(Icons.Default.FaceRetouchingNatural, "Làm đẹp", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.ClosedCaption, "Phụ đề tự động", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.Tv, "Máy nhắc chữ", onOpenSmartScript),
                            ToolItemData(Icons.Default.CameraAlt, "Máy ảnh", onPickVideo),
                            ToolItemData(Icons.Default.AutoFixHigh, "Tự động cải thiện", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.Speed, "Điều chỉnh tốc độ", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.MicNone, "Ghi âm", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.ViewQuilt, "Ghép ảnh", onPickVideo),
                            ToolItemData(Icons.Default.CropOriginal, "Chụp khung hình", onOpenLiveTranslator),
                            ToolItemData(Icons.Default.DesktopWindows, "Trình chỉnh sửa trên\nmáy tính", onOpenSmartScript)
                        )
                    )
                }

                // Section 2: Công cụ AI
                item {
                    ToolCategorySection(
                        title = "Công cụ AI",
                        tools = listOf(
                            ToolItemData(Icons.Default.SmartDisplay, "AutoCut", onPickVideo),
                            ToolItemData(Icons.Default.Person, "Ảnh đại diện AI", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.Translate, "Trình dịch video\n& Website", onOpenLiveTranslator),
                            ToolItemData(Icons.Default.RecordVoiceOver, "Cảnh đối thoại AI", onOpenSmartScript),
                            ToolItemData(Icons.Default.Image, "Tạo hình ảnh bằng AI", onPickVideo),
                            ToolItemData(Icons.Default.AutoAwesome, "Hiệu ứng AI", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.FlashOn, "Tạo video bằng AI", onNewProject),
                            ToolItemData(Icons.Default.ContentCut, "Công cụ cắt bằng AI", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.AutoAwesome, "Xu hướng AI", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.CreateNewFolder, "Tạo tệp phương tiện", onNewProject)
                        )
                    )
                }

                // Section 3: Chỉnh sửa ảnh
                item {
                    ToolCategorySection(
                        title = "Chỉnh sửa ảnh",
                        tools = listOf(
                            ToolItemData(Icons.Default.PhotoCameraBack, "Trình chỉnh sửa ảnh", onPickVideo),
                            ToolItemData(Icons.Default.PersonRemove, "Xóa nền", onPickVideo),
                            ToolItemData(Icons.Default.LightMode, "Ánh sáng thông minh", onOpenAutoCaptions),
                            ToolItemData(Icons.Default.OpenInFull, "Mở rộng bằng AI", onPickVideo)
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }
        }

        // Bottom Sticky Cyan Gradient "+ Dự án mới" Button (Image 1)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF28DAE8),
                            Color(0xFF2ECBF2),
                            Color(0xFF39B4F6)
                        )
                    )
                )
                .clickable(onClick = onNewProject)
                .testTag("all_tools_new_project_btn"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF141619)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF28DAE8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Dự án mới",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF141619)
                )
            }
        }
    }
}

private data class ToolItemData(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun ToolCategorySection(
    title: String,
    tools: List<ToolItemData>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF141619)
        )

        val rows = tools.chunked(4)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (i in 0 until 4) {
                    val item = rowItems.getOrNull(i)
                    if (item != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = item.onClick),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = Color(0xFF1A1D21),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF1A1D21),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Reproduces the exact "Tôi" (Profile) screen from Image 5 (`photo_5`).
 */
@Composable
private fun CapCutProfileMeTabContent(
    projectCount: Int,
    onOpenAllTools: () -> Unit,
    onOpenLiveTranslate: () -> Unit,
    onOpenAutoCaptions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        // Top Right Settings Gear Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onOpenAllTools) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = Color(0xFF141619),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // User Profile Row: Avatar + "Bấm để đăng nhập" + Paper Plane Bubble
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E9EF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenAutoCaptions)
            ) {
                Text(
                    text = "Bấm để đăng nhập",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF141619)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "để đồng bộ hóa dự án và mẫu của bạn >",
                    fontSize = 12.sp,
                    color = Color(0xFF7A828E)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cyan Gradient "💎 Standard" Card (Image 5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFDCF6FD),
                            Color(0xFFBDF0FF),
                            Color(0xFFB4EAFF)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = Color(0xFF00B8D9),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Standard",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141619)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhận 7 ngày dùng bản Standard với giá 0đ",
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563)
                        )
                    }

                    Surface(
                        color = Color(0xFF141619),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.clickable(onClick = onOpenAutoCaptions)
                    ) {
                        Text(
                            text = "Nhận bản dùng th...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StandardCardFeatureItem(Icons.Default.Image, "Tài nguyên", onOpenAutoCaptions)
                    StandardCardFeatureItem(Icons.Default.GridView, "Công cụ Pro", onOpenAllTools)
                    StandardCardFeatureItem(Icons.Default.AutoAwesome, "Hiệu ứng AI", onOpenLiveTranslate)
                    StandardCardFeatureItem(Icons.Default.FaceRetouchingNatural, "Làm đẹp", onOpenAutoCaptions)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Menu Rows: Lịch sử xem, Dịch Trực Tiếp Website & Ứng dụng, Trung tâm trợ giúp
        ProfileMenuRow(
            icon = Icons.Default.History,
            title = "Lịch sử xem ($projectCount dự án)",
            onClick = onOpenAutoCaptions
        )
        ProfileMenuRow(
            icon = Icons.Default.Language,
            title = "Dịch trực tiếp Website & Ứng dụng khác",
            onClick = onOpenLiveTranslate
        )
        ProfileMenuRow(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            title = "Trung tâm trợ giúp",
            onClick = onOpenAllTools
        )
    }
}

@Composable
private fun StandardCardFeatureItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF141619),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF4B5563)
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF141619),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF141619)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF)
        )
    }
}

@Composable
private fun CapCutTemplatesTabContent(
    presets: List<StylePresetEntity>,
    onApplyPresetAndOpen: (StylePresetEntity) -> Unit,
    onOpenLiveTranslate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Mẫu Phụ Đề & Karaoke Xu Hướng",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF141619)
            )
            Text(
                text = "Chạm vào bất kỳ mẫu nào để áp dụng ngay cho dự án video của bạn",
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
        }

        items(presets, key = { it.id }) { preset ->
            Surface(
                color = Color(0xFF181A20),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onApplyPresetAndOpen(preset) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = preset.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Nhãn: ${preset.badge} • Cỡ chữ ${preset.fontSizeSp.toInt()}sp",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    Surface(
                        color = Color(0xFF00D8F6),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "Dùng mẫu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF141619),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapCutInboxTabContent(
    onOpenLiveTranslate: () -> Unit,
    onOpenAutoCaptions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Hộp thư đến",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF141619)
        )
        Surface(
            color = Color(0xFFF2FAFC),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF00C4E0).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenLiveTranslate)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🌐 Tính năng mới: Dịch trực tiếp Website & Ứng dụng khác",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF141619)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bật thanh công cụ nổi hoặc dùng trình duyệt tích hợp để dịch phụ đề theo thời gian thực trên mọi trang web và ứng dụng.",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563)
                )
            }
        }
        Surface(
            color = Color(0xFFF6F8FA),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAutoCaptions)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "✨ Phụ đề tự động & Hiệu ứng Karaoke từng từ đã sẵn sàng",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF141619)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chạm để mở trình dựng phụ đề tự động với các mẫu chữ THE QUICK, brown fox, sống nhé.",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}

@Composable
private fun CyanGradientHeroButton(
    isCompact: Boolean,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .height(if (isCompact) 50.dp else 108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF68E8F4),
                        Color(0xFF8BE2FA),
                        Color(0xFF72C6FA)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 220f)
                )
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Subtle glossy wave overlay like Image 6 & 8
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(0f, h * 0.75f)
                quadraticTo(w * 0.45f, h * 0.25f, w, h * 0.55f)
                lineTo(w, 0f)
                lineTo(0f, 0f)
                close()
            }
            drawPath(path = path, color = Color.White.copy(alpha = 0.22f))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF141619)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF7CE8F7),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (!isCompact) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF141619)
                )
            }
        }
    }
}

@Composable
private fun CapCutQuickToolTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF4F6F8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF1A1D21),
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = Color(0xFF1A1D21),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * Reproduces the exact empty-state illustration from Images 6 & 8 (Video rectangle with play icon and scissors).
 */
@Composable
private fun CapCutEmptyProjectsIllustration(
    onStartCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStartCreateClick)
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = 96.dp, height = 68.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color(0xFF141619),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(76.dp.toPx(), 50.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
                modifier = Modifier
                    .offset(x = (-6).dp, y = (-3).dp)
                    .size(22.dp)
            )
            Icon(
                imageVector = Icons.Default.ContentCut,
                contentDescription = null,
                tint = Color(0xFF141619),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Các dự án của bạn sẽ xuất hiện tại đây.\nBắt đầu tạo ngay.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FloatingPaperPlaneBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 52
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .padding(3.dp)
            .clip(CircleShape)
            .background(Color(0xFF2AABEE))
            .clickable(onClick = onClick)
            .testTag("floating_paper_plane_bubble"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Dịch trực tiếp Website & Ứng dụng khác",
            tint = Color.White,
            modifier = Modifier.size((sizeDp * 0.46f).dp)
        )
    }
}

@Composable
private fun CapCutMainBottomBar(
    selectedTab: CapCutHomeBottomTab,
    onSelectTab: (CapCutHomeBottomTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(0.5.dp, Color(0xFFE5E7EB))
    ) {
        CapCutHomeBottomTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val icon = when (tab) {
                CapCutHomeBottomTab.EDIT -> Icons.Default.ContentCut
                CapCutHomeBottomTab.TEMPLATES -> Icons.Default.ViewCarousel
                CapCutHomeBottomTab.AI_LAB -> Icons.Default.AutoAwesome
                CapCutHomeBottomTab.INBOX -> Icons.Default.NotificationsNone
                CapCutHomeBottomTab.ME -> Icons.Default.PersonOutline
            }
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF141619),
                    selectedTextColor = Color(0xFF141619),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color(0xFF9CA3AF),
                    unselectedTextColor = Color(0xFF9CA3AF)
                ),
                modifier = Modifier.testTag("home_bottom_tab_${tab.name}")
            )
        }
    }
}
