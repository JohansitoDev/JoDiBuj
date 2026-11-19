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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.data.RoomSettingsStore
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
    onCreateRoom: (prompt: DrawingPrompt, totalRounds: Int, maxPlayers: Int, roundDurationSec: Int) -> Unit,
    onJoinRoom: (String) -> Unit,
    onSoloPracticeCompleted: (Int) -> Unit,
) {
    var roomCodeInput by remember { mutableStateOf("") }
    var soloPracticePrompt by remember { mutableStateOf<DrawingPrompt?>(null) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }

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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
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

                // Multiplayer Room Section (up to 10 players)
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

                        DuoButton(
                            onClick = { showCreateRoomDialog = true },
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

                itemsIndexed(DrawingCatalog.gameLevels, key = { _, level -> "level_${level.levelNumber}" }) { _, gameLvl ->
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            gameLvl.prompts.forEach { prompt ->
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

    // Create Room Dialog with guest count & room settings
    if (showCreateRoomDialog) {
        CreateRoomDialog(
            isBusy = isBusy,
            onDismiss = { showCreateRoomDialog = false },
            onCreateRoomConfigured = { prompt, rounds, maxPlayers, durationSec ->
                showCreateRoomDialog = false
                onCreateRoom(prompt, rounds, maxPlayers, durationSec)
            },
        )
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
                showCreateRoomDialog = true
            },
        )
    }
}

@Composable
fun CreateRoomDialog(
    isBusy: Boolean = false,
    onDismiss: () -> Unit,
    onCreateRoomConfigured: (
        prompt: DrawingPrompt,
        totalRounds: Int,
        maxPlayers: Int,
        roundDurationSec: Int,
    ) -> Unit,
) {
    var selectedPrompt by remember { mutableStateOf(DrawingCatalog.randomPrompt()) }
    var selectedMaxPlayers by remember { mutableStateOf(6) }
    var selectedRounds by remember { mutableStateOf(3) }
    var selectedDurationSec by remember { mutableStateOf(60) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Crear Sala de Juego",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepNavy,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Configura la sala e invita a tus amigos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(16.dp))

                // Tema / Palabra a Dibujar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SoftSurface)
                        .border(1.5.dp, BorderGray, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Tema de Dibujo:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(selectedPrompt.wordEs, style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.Bold)
                        }
                        PromptIllustrationBadge(promptKey = selectedPrompt.key, size = 48.dp)
                    }
                    Spacer(Modifier.height(8.dp))
                    DuoOutlinedButton(
                        onClick = {
                            selectedPrompt = DrawingCatalog.randomPrompt(excludeKey = selectedPrompt.key)
                        },
                        height = 38.dp,
                        accentColor = MacawBlue,
                    ) {
                        Text("🎲 Cambiar Tema Al Azar", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Cantidad de Invitados (Jugadores Máximos)
                Text(
                    text = "Cantidad de Invitados (Máx Jugadores):",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(2, 3, 4, 6, 8, 10).forEach { count ->
                        val isSel = selectedMaxPlayers == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) MacawBlue else SoftSurface)
                                .border(1.5.dp, if (isSel) MacawBlueDark else BorderGray, RoundedCornerShape(12.dp))
                                .clickable { selectedMaxPlayers = count },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSel) PureWhite else TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Número de Rondas
                Text(
                    text = "Número de Rondas:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1 to "1 R.", 2 to "2 R.", 3 to "3 R.", 5 to "5 R.").forEach { (rounds, label) ->
                        val isSel = selectedRounds == rounds
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) FeatherGreen else SoftSurface)
                                .border(1.5.dp, if (isSel) FeatherGreenDark else BorderGray, RoundedCornerShape(12.dp))
                                .clickable { selectedRounds = rounds },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSel) PureWhite else TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Tiempo por Ronda
                Text(
                    text = "Tiempo por Ronda:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(30 to "30s", 45 to "45s", 60 to "60s", 90 to "90s", 120 to "120s").forEach { (sec, label) ->
                        val isSel = selectedDurationSec == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) FoxOrange else SoftSurface)
                                .border(1.5.dp, if (isSel) DeepNavy else BorderGray, RoundedCornerShape(12.dp))
                                .clickable { selectedDurationSec = sec },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSel) PureWhite else TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DuoOutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                    ) {
                        Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                    }
                    DuoButton(
                        onClick = {
                            onCreateRoomConfigured(
                                selectedPrompt,
                                selectedRounds,
                                selectedMaxPlayers,
                                selectedDurationSec,
                            )
                        },
                        containerColor = FeatherGreen,
                        lipColor = FeatherGreenDark,
                        isLoading = isBusy,
                        modifier = Modifier.weight(1.3f),
                        height = 48.dp,
                    ) {
                        Text("✨ Crear y Abrir", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(26.dp))
                .background(PureWhite)
                .border(2.dp, BorderGray, RoundedCornerShape(26.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Práctica Solo: ${prompt.wordEs}",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = prompt.hintEs,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Cerrar", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Canvas drawing area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SoftSurface)
                    .border(1.5.dp, BorderGray, RoundedCornerShape(20.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activeStrokePoints = listOf(offset.x.toDouble(), offset.y.toDouble())
                            },
                            onDrag = { change, _ ->
                                val pos = change.position
                                activeStrokePoints = activeStrokePoints + listOf(pos.x.toDouble(), pos.y.toDouble())
                            },
                            onDragEnd = {
                                if (activeStrokePoints.size >= 4) {
                                    strokes.add(StrokeData(selectedColorHex, 8.0, activeStrokePoints))
                                }
                                activeStrokePoints = emptyList()
                            },
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    strokes.forEach { stroke ->
                        if (stroke.points.size >= 4) {
                            val path = Path().apply {
                                moveTo(stroke.points[0].toFloat(), stroke.points[1].toFloat())
                                for (i in 2 until stroke.points.size step 2) {
                                    lineTo(stroke.points[i].toFloat(), stroke.points[i + 1].toFloat())
                                }
                            }
                            drawPath(
                                path = path,
                                color = parseColorSafe(stroke.colorHex),
                                style = Stroke(
                                    width = stroke.strokeWidth.toFloat(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                        }
                    }
                    if (activeStrokePoints.size >= 4) {
                        val activePath = Path().apply {
                            moveTo(activeStrokePoints[0].toFloat(), activeStrokePoints[1].toFloat())
                            for (i in 2 until activeStrokePoints.size step 2) {
                                lineTo(activeStrokePoints[i].toFloat(), activeStrokePoints[i + 1].toFloat())
                            }
                        }
                        drawPath(
                            path = activePath,
                            color = parseColorSafe(selectedColorHex),
                            style = Stroke(
                                width = 8f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Palette selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                palette.forEach { hex ->
                    val color = parseColorSafe(hex)
                    val isSel = selectedColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (isSel) 3.dp else 1.dp, if (isSel) DeepNavy else PureWhite, CircleShape)
                            .clickable { selectedColorHex = hex },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DuoOutlinedButton(
                    onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deshacer", style = MaterialTheme.typography.labelMedium)
                }
                DuoButton(
                    onClick = onCompleteChallenge,
                    containerColor = FeatherGreen,
                    lipColor = FeatherGreenDark,
                    modifier = Modifier.weight(1.2f),
                    height = 46.dp,
                ) {
                    Text("¡Terminar (+${prompt.xpReward} XP)", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
