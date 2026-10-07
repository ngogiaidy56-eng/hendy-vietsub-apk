package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StylePresetEntity
import com.example.data.SubtitleFontOption
import com.example.data.SubtitleProjectEntity
import com.example.data.WordAnimationMode
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KineticLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.SwatchPalette
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.parseHexColor
import kotlin.math.roundToInt

@Composable
fun StyleStudioPanel(
    project: SubtitleProjectEntity,
    presets: List<StylePresetEntity>,
    showWordFxFirst: Boolean,
    onApplyPreset: (StylePresetEntity) -> Unit,
    onOpenSavePresetModal: () -> Unit,
    onDeleteCustomPreset: (Long) -> Unit,
    onSelectFont: (SubtitleFontOption) -> Unit,
    onSelectAnimationMode: (WordAnimationMode) -> Unit,
    onUpdateProjectStyle: ((SubtitleProjectEntity) -> SubtitleProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(256.dp)
            .background(StudioSurface)
            .verticalScroll(scrollState)
            .padding(vertical = 10.dp)
            .testTag("style_studio_panel"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showWordFxFirst) {
            WordFxSelectorSection(
                selectedMode = project.wordAnimation,
                onSelectMode = onSelectAnimationMode
            )
        }

        // 1. CapCut 1-Tap Style Templates Row
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MẪU PHỤ ĐỀ & PHONG CÁCH ĐÃ LƯU",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
                Surface(
                    color = ElectricCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable(onClick = onOpenSavePresetModal)
                        .testTag("save_custom_preset_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Lưu kiểu chữ hiện tại",
                            tint = ElectricCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Lưu kiểu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    val presetFont = runCatching {
                        SubtitleFontOption.valueOf(preset.fontOption).toFontFamily()
                    }.getOrDefault(SubtitleFontOption.MONTSERRAT.toFontFamily())

                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(1.dp, StudioCardBorder, RoundedCornerShape(10.dp))
                            .clickable { onApplyPreset(preset) }
                            .testTag("preset_card_${preset.name}")
                    ) {
                        Column(
                            modifier = Modifier
                                .width(126.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = parseHexColor(preset.activeWordColorHex).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = preset.badge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = parseHexColor(preset.activeWordColorHex),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                if (!preset.isBuiltIn) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete custom preset",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onDeleteCustomPreset(preset.id) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (preset.isUppercase) "VIETSUB" else "Vietsub",
                                fontFamily = presetFont,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = parseHexColor(preset.textColorHex)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.name,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. Bundled Google Fonts Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PHÔNG CHỮ STUDIO TÍCH HỢP",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SubtitleFontOption.entries) { fontOpt ->
                    val isSelected = project.subtitleFont == fontOpt
                    Surface(
                        color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ElectricCyan else StudioCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFont(fontOpt) }
                            .testTag("font_option_${fontOpt.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = fontOpt.displayName,
                                fontFamily = fontOpt.toFontFamily(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ElectricCyan else TextPrimary
                            )
                            Text(
                                text = fontOpt.sampleTag,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 3. Color Swatches: Text Color, Active Karaoke Word Color, Stroke Color
        ColorSwatchRow(
            title = "MÀU TỪ KARAOKE ĐANG ĐỌC",
            selectedHex = project.activeWordColorHex,
            onSelectHex = { hex -> onUpdateProjectStyle { it.copy(activeWordColorHex = hex) } }
        )

        ColorSwatchRow(
            title = "MÀU CHỮ PHỤ ĐỀ CHÍNH",
            selectedHex = project.textColorHex,
            onSelectHex = { hex -> onUpdateProjectStyle { it.copy(textColorHex = hex) } }
        )

        ColorSwatchRow(
            title = "MÀU VIỀN CHỮ (STROKE)",
            selectedHex = project.strokeColorHex,
            onSelectHex = { hex -> onUpdateProjectStyle { it.copy(strokeColorHex = hex) } }
        )

        // 4. Precision Sliders: Font Size, Stroke Width, Background Box Opacity & Uppercase
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StyleSliderControl(
                label = "Cỡ chữ",
                valueText = "${project.fontSizeSp.roundToInt()} sp",
                value = project.fontSizeSp,
                valueRange = 15f..38f,
                onValueChange = { v -> onUpdateProjectStyle { it.copy(fontSizeSp = v) } }
            )

            StyleSliderControl(
                label = "Độ dày viền chữ",
                valueText = "${(project.strokeWidthDp * 10).roundToInt() / 10f} dp",
                value = project.strokeWidthDp,
                valueRange = 0f..7f,
                onValueChange = { v -> onUpdateProjectStyle { it.copy(strokeWidthDp = v) } }
            )

            StyleSliderControl(
                label = "Độ mờ nền hộp chữ",
                valueText = "${(project.bgOpacity * 100).roundToInt()}%",
                value = project.bgOpacity,
                valueRange = 0f..0.95f,
                onValueChange = { v -> onUpdateProjectStyle { it.copy(bgOpacity = v) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Luôn viết IN HOA toàn bộ chữ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Switch(
                    checked = project.isUppercase,
                    onCheckedChange = { checked -> onUpdateProjectStyle { it.copy(isUppercase = checked) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("uppercase_switch")
                )
            }
        }

        if (!showWordFxFirst) {
            WordFxSelectorSection(
                selectedMode = project.wordAnimation,
                onSelectMode = onSelectAnimationMode
            )
        }
    }
}

@Composable
private fun WordFxSelectorSection(
    selectedMode: WordAnimationMode,
    onSelectMode: (WordAnimationMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesomeMotion,
                contentDescription = null,
                tint = KineticLime,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "HIỆU ỨNG KARAOKE TỪNG TỪ & CHUYỂN ĐỘNG",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KineticLime
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(WordAnimationMode.entries) { mode ->
                val isSelected = selectedMode == mode
                Surface(
                    color = if (isSelected) KineticLime.copy(alpha = 0.16f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .width(150.dp)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) KineticLime else StudioCardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectMode(mode) }
                        .testTag("word_fx_${mode.name}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = mode.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) KineticLime else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mode.description,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    selectedHex: String,
    onSelectHex: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(SwatchPalette) { (hex, color) ->
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else StudioCardBorder,
                            shape = CircleShape
                        )
                        .clickable { onSelectHex(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected $hex",
                            tint = if (hex == "#FFFFFF" || hex == "#CCFF00" || hex == "#FFE600" || hex == "#00F0FF") {
                                Color.Black
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleSliderControl(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Text(
                text = valueText,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = StudioSurfaceElevated
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}
