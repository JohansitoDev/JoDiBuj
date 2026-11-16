package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.DrawingCatalog
import com.example.data.DrawingPrompt
import com.example.data.GameRoom
import com.example.data.LevelSystem
import com.example.data.StrokeData
import com.example.data.UserProfile
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCleanInput
import com.example.ui.components.DuoOutlinedButton
import com.example.ui.components.DuoProgressBar
import com.example.ui.components.PromptIllustrationBadge
import com.example.ui.components.StatPill
import com.example.ui.components.iconForAvatarKey
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.BeeYellow
import com.example.ui.theme.BorderGray
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.CardinalRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.FeatherGreen
import com.example.ui.theme.FeatherGreenDark
import com.example.ui.theme.FoxOrange
import com.example.ui.theme.LogoPink
import com.example.ui.theme.LogoPurple
import com.example.ui.theme.LogoPurpleDark
import com.example.ui.theme.MacawBlue
import com.example.ui.theme.MacawBlueDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState

@Composable
fun HomeScreen(
    profileState: UiState<UserProfile?>,
    waitingRoomsState: UiState<List<GameRoom>>,
    isBusy: Boolean,
    isGuestMode: Boolean = false,
    onConnectGoogle: () -> Unit = {},
    onCreateRoom: (DrawingPrompt) -> Unit,
    onJoinRoom: (String) -> Unit,
    onSoloPracticeCompleted: (Int) -> Unit,
) {
    var roomCodeInput by remember { mutableStateOf("") }
    var soloPracticePrompt by remember { mutableStateOf<DrawingPrompt?>(null) }

    val profile = (profileState as? UiState.Success)?.data

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (profileState is UiState.Loading || profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MacawBlue)
            }
        } else {
            val currentLevel = profile.level.toInt().coerceAtLeast(1)
            val xpProgress = LevelSystem.xpProgressInLevel(profile.totalPoints)
            val xpLeft = LevelSystem.xpToNextLevel(profile.totalPoints)

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Stats Bar (Duolingo-inspired)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "¡Hola, ${profile.username}!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = DeepNavy,
                            )
                            Text(
                                text = "Nivel $currentLevel · ${LevelSystem.titleForLevel(currentLevel)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatPill(
                                icon = Icons.Rounded.LocalFireDepartment,
                                iconTint = FoxOrange,
                                valueText = "${profile.currentStreak}",
                            )
                            StatPill(
                                icon = Icons.Rounded.Bolt,
                                iconTint = BeeYellow,
                                valueText = "${profile.totalPoints}",
                            )
                        }
                    }
                }

                // Level XP Progress
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SoftSurface)
                            .border(1.5.dp, BorderGray, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.level_progress_title, currentLevel),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = "$xpLeft XP para Nivel ${currentLevel + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FeatherGreenDark,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        DuoProgressBar(progress = xpProgress, fillColor = FeatherGreen)
                    }
                }

                // Multiplayer Room Section (up to 6 players)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(PureWhite)
                            .border(2.dp, BorderGray, RoundedCornerShape(22.dp))
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MacawBlue.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Group,
                                    contentDescription = null,
                                    tint = MacawBlue,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.multiplayer_card_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DeepNavy,
                                )
                                Text(
                                    text = stringResource(R.string.multiplayer_card_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (isGuestMode) {
                            Text(
                                text = stringResource(R.string.guest_multiplayer_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(14.dp))
                            DuoButton(
                                onClick = onConnectGoogle,
                                containerColor = MacawBlue,
                                lipColor = MacawBlueDark,
                                testTag = "home_guest_connect_google_button",
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_connect_google),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        } else {
                            DuoButton(
                                onClick = { onCreateRoom(DrawingCatalog.randomPrompt()) },
                                containerColor = FeatherGreen,
                                lipColor = FeatherGreenDark,
                                isLoading = isBusy,
                                testTag = "create_room_button",
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_create_room),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                DuoCleanInput(
                                    value = roomCodeInput,
                                    onValueChange = { roomCodeInput = it.uppercase().take(12) },
                                    label = stringResource(R.string.input_room_code_label),
                                    placeholder = "EJ: JD48K2",
                                    leadingIcon = Icons.Rounded.MeetingRoom,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Done,
                                    ),
                                    modifier = Modifier.weight(1.25f),
                                    testTag = "join_room_code_input",
                                )
                                DuoButton(
                                    onClick = {
                                        if (roomCodeInput.trim().length >= 4) {
                                            onJoinRoom(roomCodeInput.trim())
                                        }
                                    },
                                    containerColor = MacawBlue,
                                    lipColor = MacawBlueDark,
                                    enabled = roomCodeInput.trim().length >= 4 && !isBusy,
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .padding(top = 4.dp),
                                    testTag = "join_room_button",
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_join_room),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }

                // Active waiting rooms list
                val waitingRooms = (waitingRoomsState as? UiState.Success)?.data.orEmpty()
                if (waitingRooms.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.open_rooms_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepNavy,
                        )
                    }
                    itemsIndexed(waitingRooms.take(4), key = { _, r -> r.roomCode }) { _, room ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(PureWhite)
                                .border(1.5.dp, BorderGray, RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sala #${room.roomCode} · ${room.hostName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "${room.playerUids.size}/${room.maxPlayers} jugadores · Tema: ${room.promptWord}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            DuoOutlinedButton(
                                onClick = { onJoinRoom(room.roomCode) },
                                accentColor = MacawBlue,
                                height = 44.dp,
                                modifier = Modifier.width(104.dp),
                            ) {
                                Text(stringResource(R.string.btn_enter_room), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Levels Progression Section
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Camino de Niveles",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                    )
                    Text(
                        text = "Completa cada nivel para desbloquear el siguiente de forma automática",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }

                itemsIndexed(DrawingCatalog.gameLevels, key = { _, level -> "level_${level.levelNumber}" }) { lvlIdx, gameLvl ->
                    val isUnlocked = LevelSystem.isLevelUnlocked(
                        levelNumber = gameLvl.levelNumber,
                        userLevel = currentLevel,
                        completedLevel = profile.completedLevel.toInt(),
                    )
                    val headerBg = parseColorSafe(gameLvl.themeColorHex)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isUnlocked) PureWhite else SoftSurface)
                            .border(
                                width = if (isUnlocked) 2.dp else 1.5.dp,
                                color = if (isUnlocked) headerBg else BorderGray,
                                shape = RoundedCornerShape(22.dp),
                            )
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isUnlocked) headerBg else BorderGray),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isUnlocked) iconForAvatarKey(gameLvl.iconKey) else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = gameLvl.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isUnlocked) DeepNavy else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = if (isUnlocked) gameLvl.description else "🔒 Bloqueado: Pasa el Nivel ${gameLvl.levelNumber - 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isUnlocked) FeatherGreenDark else CardinalRed,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Prompt nodes for this level
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            gameLvl.prompts.forEachIndexed { pIdx, prompt ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .testTag("level_node_${prompt.key}")
                                        .clickable(enabled = isUnlocked) {
                                            soloPracticePrompt = prompt
                                        },
                                ) {
                                    Box(
                                        modifier = Modifier.size(72.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .offset(y = 4.dp)
                                                .clip(CircleShape)
                                                .background(if (isUnlocked) headerBg.copy(alpha = 0.4f) else BorderGrayDark),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .background(if (isUnlocked) PureWhite else SoftSurface)
                                                .border(
                                                    width = 2.5.dp,
                                                    color = if (isUnlocked) headerBg else BorderGray,
                                                    shape = CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (isUnlocked) {
                                                PromptIllustrationBadge(
                                                    promptKey = prompt.key,
                                                    size = 44.dp,
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.Lock,
                                                    contentDescription = "Bloqueado",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = prompt.wordEs,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isUnlocked) TextPrimary else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "+${prompt.xpReward} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUnlocked) FeatherGreenDark else TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Solo Level Practice Dialog
    val activePrompt = soloPracticePrompt
    if (activePrompt != null) {
        SoloPracticeCanvasDialog(
            prompt = activePrompt,
            onDismiss = { soloPracticePrompt = null },
            onCompleteChallenge = {
                soloPracticePrompt = null
                onSoloPracticeCompleted(activePrompt.xpReward)
            },
            onCreateRoomWithPrompt = {
                soloPracticePrompt = null
                onCreateRoom(activePrompt)
            },
        )
    }
}

@Composable
private fun SoloPracticeCanvasDialog(
    prompt: DrawingPrompt,
    onDismiss: () -> Unit,
    onCompleteChallenge: () -> Unit,
    onCreateRoomWithPrompt: () -> Unit,
) {
    val strokes = remember { mutableStateListOf<StrokeData>() }
    var activeStrokePoints by remember { mutableStateOf<List<Double>>(emptyList()) }
    var selectedColorHex by remember { mutableStateOf("#1CB0F6") }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    val palette = listOf("#3C3C3C", "#1CB0F6", "#58CC02", "#FF9600", "#EC4899", "#8B5CF6", "#FF4B4B")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Cerrar", tint = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Reto: ${prompt.wordEs}",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                    )
                    Text(
                        text = prompt.hintEs,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                PromptIllustrationBadge(promptKey = prompt.key, size = 56.dp)
            }

            Spacer(Modifier.height(12.dp))

            // Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PureWhite)
                    .border(2.dp, BorderGray, RoundedCornerShape(22.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(selectedColorHex) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val nx = (offset.x / canvasSize.width.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                                val ny = (offset.y / canvasSize.height.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                                activeStrokePoints = listOf(nx, ny)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val nx = (change.position.x / canvasSize.width.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                                val ny = (change.position.y / canvasSize.height.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                                activeStrokePoints = activeStrokePoints + listOf(nx, ny)
                            },
                            onDragEnd = {
                                if (activeStrokePoints.size >= 2) {
                                    strokes.add(StrokeData(selectedColorHex, 10.0, activeStrokePoints))
                                }
                                activeStrokePoints = emptyList()
                            },
                            onDragCancel = { activeStrokePoints = emptyList() },
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun renderStroke(colorHex: String, width: Float, pts: List<Double>) {
                        if (pts.size < 2) return
                        val path = Path()
                        path.moveTo((pts[0] * size.width).toFloat(), (pts[1] * size.height).toFloat())
                        var i = 2
                        while (i + 1 < pts.size) {
                            path.lineTo((pts[i] * size.width).toFloat(), (pts[i + 1] * size.height).toFloat())
                            i += 2
                        }
                        drawPath(
                            path = path,
                            color = parseColorSafe(colorHex),
                            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                    strokes.forEach { s -> renderStroke(s.colorHex, s.strokeWidth.toFloat(), s.points) }
                    if (activeStrokePoints.size >= 2) {
                        renderStroke(selectedColorHex, 10f, activeStrokePoints)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Color Palette & Tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { hex ->
                        val color = parseColorSafe(hex)
                        val isSel = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (isSel) 3.dp else 1.dp, if (isSel) DeepNavy else BorderGray, CircleShape)
                                .clickable { selectedColorHex = hex },
                        )
                    }
                }
                Row {
                    IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Deshacer", tint = TextPrimary)
                    }
                    IconButton(onClick = { strokes.clear() }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Limpiar", tint = CardinalRed)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DuoOutlinedButton(
                    onClick = onCreateRoomWithPrompt,
                    accentColor = MacawBlue,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Jugar en sala", style = MaterialTheme.typography.labelLarge)
                }
                DuoButton(
                    onClick = onCompleteChallenge,
                    enabled = strokes.isNotEmpty(),
                    containerColor = FeatherGreen,
                    lipColor = FeatherGreenDark,
                    modifier = Modifier.weight(1.2f),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Completar (+${prompt.xpReward} XP)", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
