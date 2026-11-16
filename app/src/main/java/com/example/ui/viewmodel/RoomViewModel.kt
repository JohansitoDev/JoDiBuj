package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DrawingCatalog
import com.example.data.GameRoom
import com.example.data.JoDiBujRepository
import com.example.data.RoomPlayer
import com.example.data.RoundDrawing
import com.example.data.RoundVote
import com.example.data.StrokeData
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoomSnapshotState(
    val room: GameRoom? = null,
    val players: List<RoomPlayer> = emptyList(),
    val drawings: List<RoundDrawing> = emptyList(),
    val votes: List<RoundVote> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    fun scoreForPlayer(userId: String): Int {
        val votePoints = votes.filter { it.targetUserId == userId }.sumOf { it.points.toInt() }
        val submissionPoints = drawings.count { it.userId == userId } * 15
        return votePoints + submissionPoints
    }

    fun rankedPlayers(): List<Pair<RoomPlayer, Int>> {
        return players
            .map { player -> player to scoreForPlayer(player.userId) }
            .sortedByDescending { it.second }
    }
}

class RoomViewModel(
    private val repository: JoDiBujRepository,
    private val roomCode: String,
    val currentUserId: String,
) : ViewModel() {

    val roomState: StateFlow<RoomSnapshotState> = combine(
        repository.observeRoom(roomCode),
        repository.observeRoomPlayers(roomCode),
        repository.observeRoomDrawings(roomCode),
        repository.observeRoomVotes(roomCode),
    ) { room, players, drawings, votes ->
        RoomSnapshotState(
            room = room,
            players = players,
            drawings = drawings,
            votes = votes,
            isLoading = false,
            error = if (room == null) "La sala ya no está disponible." else null,
        )
    }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing room $roomCode", fsErr ?: error)
            emit(
                RoomSnapshotState(
                    isLoading = false,
                    error = (fsErr ?: error).message ?: "Error de conexión con la sala",
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = RoomSnapshotState(isLoading = true),
        )

    private val _strokes = MutableStateFlow<List<StrokeData>>(emptyList())
    val strokes: StateFlow<List<StrokeData>> = _strokes.asStateFlow()

    private val _selectedColorHex = MutableStateFlow("#1CB0F6")
    val selectedColorHex: StateFlow<String> = _selectedColorHex.asStateFlow()

    private val _selectedStrokeWidth = MutableStateFlow(10f)
    val selectedStrokeWidth: StateFlow<Float> = _selectedStrokeWidth.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _claimedRewards = MutableStateFlow(false)
    val claimedRewards: StateFlow<Boolean> = _claimedRewards.asStateFlow()

    fun setColor(hex: String) {
        _selectedColorHex.value = hex
    }

    fun setStrokeWidth(width: Float) {
        _selectedStrokeWidth.value = width
    }

    fun addStroke(stroke: StrokeData) {
        if (_strokes.value.size < 150) {
            _strokes.value = _strokes.value + stroke
        }
    }

    fun undoStroke() {
        val current = _strokes.value
        if (current.isNotEmpty()) {
            _strokes.value = current.dropLast(1)
        }
    }

    fun clearCanvas() {
        _strokes.value = emptyList()
    }

    fun startDrawingPhase(room: GameRoom) {
        viewModelScope.launch {
            _isBusy.value = true
            _strokes.value = emptyList()
            val prompt = DrawingCatalog.byKey(room.promptKey)
            repository.updateRoomState(
                roomCode = room.roomCode,
                newStatus = "drawing",
                prompt = prompt,
                currentRound = 1,
            ).onFailure { _message.value = it.message }
            _isBusy.value = false
        }
    }

    fun submitCurrentDrawing(room: GameRoom, profile: UserProfile) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitDrawing(
                roomCode = room.roomCode,
                round = room.currentRound.toInt(),
                promptWord = room.promptWord,
                strokes = _strokes.value,
                profile = profile,
            )
            _isBusy.value = false
            result.fold(
                onSuccess = { _message.value = "¡Dibujo enviado!" },
                onFailure = { _message.value = it.message ?: "Error al enviar dibujo" },
            )
        }
    }

    fun moveToVoting(room: GameRoom) {
        viewModelScope.launch {
            _isBusy.value = true
            val prompt = DrawingCatalog.byKey(room.promptKey)
            repository.updateRoomState(
                roomCode = room.roomCode,
                newStatus = "voting",
                prompt = prompt,
                currentRound = room.currentRound.toInt(),
            ).onFailure { _message.value = it.message }
            _isBusy.value = false
        }
    }

    fun moveToRoundResults(room: GameRoom) {
        viewModelScope.launch {
            _isBusy.value = true
            val prompt = DrawingCatalog.byKey(room.promptKey)
            repository.updateRoomState(
                roomCode = room.roomCode,
                newStatus = "results",
                prompt = prompt,
                currentRound = room.currentRound.toInt(),
            ).onFailure { _message.value = it.message }
            _isBusy.value = false
        }
    }

    fun advanceToNextRoundOrFinish(room: GameRoom) {
        viewModelScope.launch {
            _isBusy.value = true
            _strokes.value = emptyList()
            if (room.currentRound >= room.totalRounds) {
                val prompt = DrawingCatalog.byKey(room.promptKey)
                repository.updateRoomState(
                    roomCode = room.roomCode,
                    newStatus = "finished",
                    prompt = prompt,
                    currentRound = room.currentRound.toInt(),
                ).onFailure { _message.value = it.message }
            } else {
                val nextPrompt = DrawingCatalog.randomPrompt(excludeKey = room.promptKey)
                repository.updateRoomState(
                    roomCode = room.roomCode,
                    newStatus = "drawing",
                    prompt = nextPrompt,
                    currentRound = room.currentRound.toInt() + 1,
                ).onFailure { _message.value = it.message }
            }
            _isBusy.value = false
        }
    }

    fun castVote(room: GameRoom, targetUserId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.castVote(
                roomCode = room.roomCode,
                round = room.currentRound.toInt(),
                targetUserId = targetUserId,
                points = 50,
            )
            _isBusy.value = false
            result.fold(
                onSuccess = { _message.value = "¡Voto registrado (+50 pts para tu amigo)!" },
                onFailure = { _message.value = it.message ?: "No se pudo votar" },
            )
        }
    }

    fun claimFinalRewardsIfNeeded(state: RoomSnapshotState) {
        if (_claimedRewards.value) return
        val room = state.room ?: return
        if (room.status != "finished") return
        _claimedRewards.value = true

        viewModelScope.launch {
            val ranked = state.rankedPlayers()
            val myScore = state.scoreForPlayer(currentUserId).coerceAtLeast(20)
            val topScore = ranked.firstOrNull()?.second ?: 0
            val won = myScore >= topScore && ranked.isNotEmpty()
            repository.awardMatchPoints(pointsEarned = myScore, wonMatch = won)
                .onSuccess { updated ->
                    _message.value = "¡+$myScore pts sumados a tu perfil! Nivel ${updated.level}"
                }
        }
    }

    fun leaveRoom(onLeft: () -> Unit) {
        viewModelScope.launch {
            repository.leaveRoom(roomCode)
            onLeft()
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private companion object {
        const val TAG = "RoomViewModel"
    }
}
