package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AvatarCatalog
import com.example.ui.theme.BorderGray
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.FeatherGreen
import com.example.ui.theme.FeatherGreenDark
import com.example.ui.theme.MacawBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DuoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = FeatherGreen,
    lipColor: Color = FeatherGreenDark,
    contentColor: Color = PureWhite,
    borderColor: Color? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 54.dp,
    testTag: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val lipHeight = 4.dp
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) lipHeight else 0.dp,
        label = "duo_button_press",
    )
    val effectiveContainer = if (enabled) containerColor else BorderGray
    val effectiveLip = if (enabled) lipColor else BorderGrayDark
    val effectiveContent = if (enabled) contentColor else TextSecondary

    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagMod)
            .minimumInteractiveComponentSize()
            .height(height + lipHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick,
            ),
    ) {
        // Bottom 3D lip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(16.dp))
                .background(effectiveLip),
        )
        // Top tactile button face
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(16.dp))
                .background(effectiveContainer)
                .then(
                    if (borderColor != null) {
                        Modifier.border(
                            BorderStroke(2.dp, borderColor),
                            RoundedCornerShape(16.dp),
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = effectiveContent,
                    strokeWidth = 2.5.dp,
                )
            } else {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides effectiveContent
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun DuoOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    accentColor: Color = TextPrimary,
    testTag: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    DuoButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = PureWhite,
        lipColor = BorderGray,
        contentColor = accentColor,
        borderColor = BorderGray,
        enabled = enabled,
        height = height,
        testTag = testTag,
        content = content,
    )
}

@Composable
fun DuoCleanInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String? = null,
) {
    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(tagMod),
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = TextSecondary) }
        } else {
            null
        },
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = TextSecondary) }
        } else {
            null
        },
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        isError = isError,
        supportingText = if (!supportingText.isNullOrBlank()) {
            { Text(supportingText) }
        } else {
            null
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            errorContainerColor = PureWhite,
            focusedBorderColor = MacawBlue,
            unfocusedBorderColor = BorderGray,
            focusedLabelColor = MacawBlue,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
    )
}

@Composable
fun DuoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = SoftSurface,
    fillColor: Color = FeatherGreen,
    height: Dp = 16.dp,
) {
    val clamped = progress.coerceIn(0.05f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .border(1.dp, BorderGray, RoundedCornerShape(50)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(fillColor),
        ) {
            // Subtle Duolingo top sheen highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height * 0.32f)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PureWhite.copy(alpha = 0.28f)),
            )
        }
    }
}

@Composable
fun PlayerAvatar(
    iconKey: String,
    colorHex: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val bgColor = remember(colorHex) { parseColorSafe(colorHex) }
    val icon = remember(iconKey) { iconForAvatarKey(iconKey) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, PureWhite, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconKey,
            tint = PureWhite,
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

fun iconForAvatarKey(key: String): ImageVector = when (key) {
    "cat" -> Icons.Rounded.Pets
    "fish" -> Icons.Rounded.SetMeal
    "star" -> Icons.Rounded.Star
    "rocket" -> Icons.Rounded.RocketLaunch
    "pencil" -> Icons.Rounded.Create
    "palette" -> Icons.Rounded.Palette
    "crown" -> Icons.Rounded.EmojiEvents
    "robot" -> Icons.Rounded.SmartToy
    "heart" -> Icons.Rounded.Favorite
    "sparkles" -> Icons.Rounded.AutoAwesome
    "fire" -> Icons.Rounded.LocalFireDepartment
    "face" -> Icons.Rounded.Face
    else -> Icons.Rounded.Brush
}

@Composable
fun AvatarSelectionDialog(
    initialIconKey: String,
    initialColorHex: String,
    onDismiss: () -> Unit,
    onAvatarSelected: (iconKey: String, colorHex: String) -> Unit,
) {
    var selectedIcon by remember { mutableStateOf(initialIconKey) }
    var selectedColor by remember { mutableStateOf(initialColorHex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .background(PureWhite)
                .border(2.dp, BorderGray, RoundedCornerShape(26.dp))
                .padding(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Elige tu Avatar",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepNavy,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(14.dp))

                // Avatar Preview
                PlayerAvatar(
                    iconKey = selectedIcon,
                    colorHex = selectedColor,
                    size = 80.dp,
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Ícono de Avatar:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                // Grid of icons
                val rows = AvatarCatalog.icons.chunked(6)
                rows.forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowIcons.forEach { (key, label) ->
                            val isSel = selectedIcon == key
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) parseColorSafe(selectedColor) else SoftSurface)
                                    .border(1.5.dp, if (isSel) DeepNavy else BorderGray, CircleShape)
                                    .clickable { selectedIcon = key },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = iconForAvatarKey(key),
                                    contentDescription = label,
                                    tint = if (isSel) PureWhite else TextSecondary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Color de Fondo:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                val colorRows = AvatarCatalog.colors.chunked(5)
                colorRows.forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowColors.forEach { hex ->
                            val c = parseColorSafe(hex)
                            val isSel = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (isSel) 3.dp else 0.dp, if (isSel) DeepNavy else c, CircleShape)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSel) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DuoOutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                    ) {
                        Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                    }
                    DuoButton(
                        onClick = {
                            onAvatarSelected(selectedIcon, selectedColor)
                            onDismiss()
                        },
                        containerColor = FeatherGreen,
                        lipColor = FeatherGreenDark,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                    ) {
                        Text("Guardar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

fun parseColorSafe(hex: String, fallback: Color = MacawBlue): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val colorInt = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000L or colorInt)
            8 -> Color(colorInt)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun StatPill(
    icon: ImageVector,
    iconTint: Color,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PureWhite)
            .border(1.5.dp, BorderGray, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
    }
}
