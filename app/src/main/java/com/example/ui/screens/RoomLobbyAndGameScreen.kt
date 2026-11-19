package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.GameRoom
import com.example.data.RoomPlayer
import com.example.data.RoomRequest
import com.example.data.RoundDrawing
import com.example.data.RoundVote
import com.example.data.StrokeData
import com.example.data.UserProfile
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoOutlinedButton
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.PromptIllustrationBadge
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.BeeYellow
import com.example.ui.theme.BeeYellowDark
import com.example.ui.theme.BorderGray
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
import com.example.ui.viewmodel.RoomSnapshotState
import com.example.ui.viewmodel.RoomViewModel
import kotlinx.coroutines.delay

@Composable
fun RoomScreen(
    viewModel: RoomViewModel,
    userProfile: UserProfile,
    onExitRoom: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val state by viewModel.roomState.collectAsStateWithLifecycle()
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val selectedColorHex by viewModel.selectedColorHex.collectAsStateWithLifecycle()
    val selectedWidth by viewModel.selectedStrokeWidth.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        message?.let {
            onShowMessage(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.room?.status) {
        if (state.room?.status == "finished") {
            viewModel.claimFinalRewardsIfNeeded(state)
        }
    }

    BackHandler {
        viewModel.leaveRoom(onExitRoom)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MacawBlue)
                }
            }
            state.room == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.error ?: "La sala se cerró.",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    DuoButton(onClick = onExitRoom) {
                        Text("Volver al inicio", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            else -> {
                val room = state.room!!
                val isHost = room.hostId == viewModel.currentUserId

                Column(
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxSize(),
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = { viewModel.leaveRoom(onExitRoom) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Salir de sala", tint = TextPrimary)
                        }
                        Text(
                            text = "Sala #${room.roomCode} · Ronda ${room.currentRound}/${room.totalRounds}",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavy,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SoftSurface)
                                .border(1.dp, BorderGray, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "${state.players.size}/${room.maxPlayers}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MacawBlue,
                            )
                        }
                    }

                    when (room.status) {
                        "waiting" -> WaitingLobbyContent(
                            room = room,
                            players = state.players,
                            pendingRequests = state.pendingRequests,
                            isHost = isHost,
                            isBusy = isBusy,
                            onStartGame = { viewModel.startDrawingPhase(room) },
                            onRespondRequest = { userId, accept -> viewModel.respondToJoinRequest(userId, accept) },
                            onShowMessage = onShowMessage,
                        )
                        "drawing" -> DrawingPhaseContent(
                            room = room,
                            players = state.players,
                            currentUserId = viewModel.currentUserId,
                            isHost = isHost,
                            isBusy = isBusy,
                            strokes = strokes,
                            selectedColorHex = selectedColorHex,
                            selectedWidth = selectedWidth,
                            onSelectColor = viewModel::setColor,
                            onSelectWidth = viewModel::setStrokeWidth,
                            onAddStroke = viewModel::addStroke,
                            onUndo = viewModel::undoStroke,
                            onClear = viewModel::clearCanvas,
                            onSubmitDrawing = { viewModel.submitCurrentDrawing(room, userProfile) },
                            onAdvanceToVoting = { viewModel.moveToVoting(room) },
                        )
                        "voting" -> VotingPhaseContent(
                            room = room,
                            state = state,
                            currentUserId = viewModel.currentUserId,
                            isHost = isHost,
                            isBusy = isBusy,
                            onVoteForPlayer = { targetUid -> viewModel.castVote(room, targetUid) },
                            onFinishVoting = { viewModel.moveToRoundResults(room) },
                        )
                        "results", "finished" -> ResultsAndPodiumContent(
                            room = room,
                            state = state,
                            isHost = isHost,
                            isBusy = isBusy,
                            onNextRound = { viewModel.advanceToNextRoundOrFinish(room) },
                            onExitRoom = { viewModel.leaveRoom(onExitRoom) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingLobbyContent(
    room: GameRoom,
    players: List<RoomPlayer>,
    pendingRequests: List<RoomRequest>,
    isHost: Boolean,
    isBusy: Boolean,
    onStartGame: () -> Unit,
    onRespondRequest: (userId: String, accept: Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isHost && pendingRequests.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BeeYellow.copy(alpha = 0.15f))
                        .border(2.dp, BeeYellow, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        text = "🔔 Solicitudes de entrada pendientes",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    pendingRequests.forEach { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PureWhite)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                PlayerAvatar(iconKey = req.avatarIcon, colorHex = req.avatarColor, size = 38.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(req.username, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DuoOutlinedButton(
                                    onClick = { onRespondRequest(req.userId, false) },
                                    accentColor = CardinalRed,
                                    height = 36.dp,
                                    modifier = Modifier.width(88.dp),
                                ) {
                                    Text("Rechazar")
                                }
                                DuoButton(
                                    onClick = { onRespondRequest(req.userId, true) },
                                    containerColor = FeatherGreen,
                                    lipColor = FeatherGreenDark,
                                    height = 36.dp,
                                    modifier = Modifier.width(88.dp),
                                ) {
                                    Text("Aceptar")
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(SoftSurface)
                    .border(2.dp, BorderGray, RoundedCornerShape(22.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.room_code_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = room.roomCode,
                    style = MaterialTheme.typography.displaySmall,
                    color = DeepNavy,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Enlace: https://jodibuj.app/room/${room.roomCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DuoOutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(room.roomCode))
                            onShowMessage("Código ${room.roomCode} copiado")
                        },
                        accentColor = MacawBlue,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copiar", style = MaterialTheme.typography.labelMedium)
                    }
                    DuoButton(
                        onClick = {
                            val shareText = "¡Únete a mi sala de dibujo en JoDiBuj! 🎨\nIntroduce este código de sala: *${room.roomCode}*\n¡Entra a la app y dibuja conmigo!"
                            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(shareText)}")
                            }
                            try {
                                context.startActivity(whatsappIntent)
                            } catch (_: Exception) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Invitar amigo"))
                            }
                        },
                        containerColor = FeatherGreen,
                        lipColor = FeatherGreenDark,
                        modifier = Modifier.weight(1.2f),
                        height = 46.dp,
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("💬 WhatsApp", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            Text(
                text = "Jugadores en la sala (${players.size}/${room.maxPlayers})",
                style = MaterialTheme.typography.titleLarge,
                color = DeepNavy,
            )
        }

        // Dynamic player slots based on room.maxPlayers
        items((0 until room.maxPlayers.toInt().coerceAtMost(20)).toList()) { slotIdx ->
            val player = players.getOrNull(slotIdx)
            if (player != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PureWhite)
                        .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerAvatar(
                        iconKey = player.avatarIcon,
                        colorHex = player.avatarColor,
                        size = 42.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = player.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (player.userId == room.hostId) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BeeYellow.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Anfitrión", style = MaterialTheme.typography.labelSmall, color = FoxOrange)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftSurface)
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Espacio ${slotIdx + 1} · Esperando amigo...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            if (isHost) {
                DuoButton(
                    onClick = onStartGame,
                    containerColor = FeatherGreen,
                    lipColor = FeatherGreenDark,
                    isLoading = isBusy,
                    testTag = "start_game_button",
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_start_game), style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = "Esperando a que ${room.hostName} inicie la partida...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawingPhaseContent(
    room: GameRoom,
    players: List<RoomPlayer>,
    currentUserId: String,
    isHost: Boolean,
    isBusy: Boolean,
    strokes: List<StrokeData>,
    selectedColorHex: String,
    selectedWidth: Float,
    onSelectColor: (String) -> Unit,
    onSelectWidth: (Float) -> Unit,
    onAddStroke: (StrokeData) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSubmitDrawing: () -> Unit,
    onAdvanceToVoting: () -> Unit,
) {
    var secondsLeft by remember(room.currentRound) { mutableIntStateOf(room.roundDurationSec.toInt()) }
    var activePoints by remember { mutableStateOf<List<Double>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    LaunchedEffect(room.currentRound) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    val myPlayer = players.firstOrNull { it.userId == currentUserId }
    val submittedAlready = (myPlayer?.hasSubmittedRound ?: 0L) >= room.currentRound
    val submittedCount = players.count { it.hasSubmittedRound >= room.currentRound }

    val palette = listOf("#3C3C3C", "#1CB0F6", "#58CC02", "#FF9600", "#EC4899", "#8B5CF6", "#FF4B4B", "#FFC800")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        // Prompt + Illustration + Timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SoftSurface)
                .border(1.5.dp, BorderGray, RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PromptIllustrationBadge(promptKey = room.promptKey, size = 60.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dibuja: ${room.promptWord}",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepNavy,
                )
                Text(
                    text = "$submittedCount/${players.size} enviados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (secondsLeft <= 10) CardinalRed.copy(alpha = 0.14f) else PureWhite)
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = if (secondsLeft <= 10) CardinalRed else MacawBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${secondsLeft}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (secondsLeft <= 10) CardinalRed else TextPrimary,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Interactive Drawing Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(PureWhite)
                .border(2.dp, BorderGray, RoundedCornerShape(22.dp))
                .onSizeChanged { canvasSize = it }
                .pointerInput(selectedColorHex, selectedWidth, submittedAlready) {
                    if (submittedAlready) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val nx = (offset.x / canvasSize.width.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                            val ny = (offset.y / canvasSize.height.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                            activePoints = listOf(nx, ny)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val nx = (change.position.x / canvasSize.width.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                            val ny = (change.position.y / canvasSize.height.coerceAtLeast(1)).toDouble().coerceIn(0.0, 1.0)
                            activePoints = activePoints + listOf(nx, ny)
                        },
                        onDragEnd = {
                            if (activePoints.size >= 2) {
                                onAddStroke(StrokeData(selectedColorHex, selectedWidth.toDouble(), activePoints))
                            }
                            activePoints = emptyList()
                        },
                        onDragCancel = { activePoints = emptyList() },
                    )
                },
        ) {
            StrokeCanvas(
                strokes = strokes,
                activeStroke = if (activePoints.size >= 2) StrokeData(selectedColorHex, selectedWidth.toDouble(), activePoints) else null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(10.dp))

        // Colors + Stroke Sizes + Undo/Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                palette.forEach { hex ->
                    val color = parseColorSafe(hex)
                    val isSel = selectedColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (isSel) 3.dp else 1.dp, if (isSel) DeepNavy else BorderGray, CircleShape)
                            .clickable { onSelectColor(hex) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(6f to 10.dp, 12f to 16.dp, 20f to 22.dp).forEach { (w, dotSize) ->
                    val isSel = selectedWidth == w
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSel) MacawBlue.copy(alpha = 0.16f) else PureWhite)
                            .clickable { onSelectWidth(w) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(TextPrimary),
                        )
                    }
                }
                IconButton(onClick = onUndo, enabled = !submittedAlready) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Deshacer", tint = TextPrimary)
                }
                IconButton(onClick = onClear, enabled = !submittedAlready) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Limpiar", tint = CardinalRed)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DuoButton(
                onClick = onSubmitDrawing,
                enabled = !submittedAlready && strokes.isNotEmpty() && !isBusy,
                containerColor = if (submittedAlready) BorderGray else FeatherGreen,
                lipColor = if (submittedAlready) BorderGray else FeatherGreenDark,
                modifier = Modifier.weight(1.2f),
                testTag = "submit_drawing_button",
            ) {
                Icon(
                    if (submittedAlready) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (submittedAlready) "¡Enviado!" else "Enviar dibujo",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (isHost) {
                DuoButton(
                    onClick = onAdvanceToVoting,
                    containerColor = LogoPurple,
                    lipColor = LogoPurpleDark,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                    testTag = "advance_to_voting_button",
                ) {
                    Icon(Icons.Rounded.HowToVote, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Votar ahora", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VotingPhaseContent(
    room: GameRoom,
    state: RoomSnapshotState,
    currentUserId: String,
    isHost: Boolean,
    isBusy: Boolean,
    onVoteForPlayer: (String) -> Unit,
    onFinishVoting: () -> Unit,
) {
    val roundDrawings = state.drawings.filter { it.round == room.currentRound }
    val roundVotes = state.votes.filter { it.round == room.currentRound }
    val myVote = roundVotes.firstOrNull { it.voterId == currentUserId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SoftSurface)
                    .border(1.5.dp, BorderGray, RoundedCornerShape(18.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = "¡Vota por el mejor ${room.promptWord}!",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepNavy,
                )
                Text(
                    text = if (myVote != null) {
                        "¡Ya emitiste tu voto en esta ronda! (${roundVotes.size} votos totales)"
                    } else {
                        "Toca el botón verde para votar por tu amigo (+50 pts)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        if (roundDrawings.isEmpty()) {
            item {
                Text(
                    text = "No se enviaron dibujos en esta ronda.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }
        } else {
            items(roundDrawings, key = { it.id }) { drawing ->
                val isMine = drawing.userId == currentUserId
                val votesReceived = roundVotes.count { it.targetUserId == drawing.userId }
                val votedForThis = myVote?.targetUserId == drawing.userId

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(PureWhite)
                        .border(
                            width = if (votedForThis) 2.5.dp else 1.5.dp,
                            color = if (votedForThis) FeatherGreen else BorderGray,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (isMine) "${drawing.username} (Tu dibujo)" else "Dibujo de ${drawing.username}",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavy,
                        )
                        Text(
                            text = "$votesReceived votos (+${votesReceived * 50} pts)",
                            style = MaterialTheme.typography.labelSmall,
                            color = FeatherGreenDark,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                            .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp)),
                    ) {
                        StrokeCanvas(
                            strokes = drawing.parsedStrokes(),
                            activeStroke = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    if (isMine) {
                        Text(
                            text = "No puedes votar por tu propio dibujo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        DuoButton(
                            onClick = { onVoteForPlayer(drawing.userId) },
                            enabled = myVote == null && !isBusy,
                            containerColor = if (votedForThis) FeatherGreen else MacawBlue,
                            lipColor = if (votedForThis) FeatherGreenDark else MacawBlueDark,
                            testTag = "vote_button_${drawing.userId}",
                        ) {
                            Icon(Icons.Rounded.HowToVote, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    votedForThis -> "¡Votaste por ${drawing.username}!"
                                    myVote != null -> "Voto ya emitido"
                                    else -> "Votar por ${drawing.username} (+50 pts)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        if (isHost) {
            item {
                DuoButton(
                    onClick = onFinishVoting,
                    containerColor = BeeYellow,
                    lipColor = BeeYellowDark,
                    contentColor = DeepNavy,
                    enabled = !isBusy,
                    testTag = "finish_voting_button",
                ) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver ganador y puntos", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ResultsAndPodiumContent(
    room: GameRoom,
    state: RoomSnapshotState,
    isHost: Boolean,
    isBusy: Boolean,
    onNextRound: () -> Unit,
    onExitRoom: () -> Unit,
) {
    val ranked = state.rankedPlayers()
    val winner = ranked.firstOrNull()
    val isFinished = room.status == "finished"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (winner != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BeeYellow.copy(alpha = 0.16f))
                        .border(2.dp, BeeYellow, RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = FoxOrange,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isFinished) "¡Campeón de la Sala!" else "Líder tras Ronda ${room.currentRound}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepNavy,
                    )
                    Spacer(Modifier.height(10.dp))
                    PlayerAvatar(
                        iconKey = winner.first.avatarIcon,
                        colorHex = winner.first.avatarColor,
                        size = 64.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = winner.first.username,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                    )
                    Text(
                        text = "${winner.second} puntos acumulados",
                        style = MaterialTheme.typography.titleMedium,
                        color = FeatherGreenDark,
                    )
                }
            }
        }

        item {
            Text(
                text = "Puntuación de la sala",
                style = MaterialTheme.typography.titleLarge,
                color = DeepNavy,
            )
        }

        items(ranked, key = { it.first.userId }) { (player, score) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PureWhite)
                    .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerAvatar(
                    iconKey = player.avatarIcon,
                    colorHex = player.avatarColor,
                    size = 42.dp,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = player.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$score pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MacawBlue,
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            if (!isFinished && isHost) {
                DuoButton(
                    onClick = onNextRound,
                    containerColor = FeatherGreen,
                    lipColor = FeatherGreenDark,
                    enabled = !isBusy,
                    testTag = "next_round_button",
                ) {
                    Text(
                        text = if (room.currentRound >= room.totalRounds) {
                            "Finalizar y reclamar puntos"
                        } else {
                            "Siguiente ronda (${room.currentRound + 1}/${room.totalRounds})"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else if (isFinished) {
                DuoButton(
                    onClick = onExitRoom,
                    containerColor = MacawBlue,
                    lipColor = MacawBlueDark,
                    testTag = "back_home_button",
                ) {
                    Text("Volver al inicio", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = "Esperando a que ${room.hostName} avance...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StrokeCanvas(
    strokes: List<StrokeData>,
    activeStroke: StrokeData?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        fun drawOne(s: StrokeData) {
            val pts = s.points
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
                color = parseColorSafe(s.colorHex),
                style = Stroke(
                    width = s.strokeWidth.toFloat(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        strokes.forEach(::drawOne)
        if (activeStroke != null) drawOne(activeStroke)
    }
}
