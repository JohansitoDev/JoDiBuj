package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DrawingCatalog
import com.example.data.DrawingPrompt
import com.example.data.GameRoom
import com.example.data.JoDiBujRepository
import com.example.data.LeaderboardEntry
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel(
    private val repository: JoDiBujRepository,
    private val currentUserId: String,
    private val initialGoogleName: String = "",
    private val initialGoogleEmail: String = "",
    private val pendingUsername: String = "",
    private val pendingBio: String = "",
    private val pendingAvatarIcon: String = "brush",
    private val pendingAvatarColor: String = "#1CB0F6",
) : ViewModel() {

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = extractFirestoreException(error)
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.message ?: "Error al cargar el perfil"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    val leaderboard: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeGlobalLeaderboard()
        .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = extractFirestoreException(error)
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing leaderboard", root)
            emit(UiState.Error(root.message ?: "Error al cargar la tabla global"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    val waitingRooms: StateFlow<UiState<List<GameRoom>>> = repository.observeWaitingRooms()
        .map<List<GameRoom>, UiState<List<GameRoom>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = extractFirestoreException(error)
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing waiting rooms", root)
            emit(UiState.Error(root.message ?: "Error al cargar las salas"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var didAutoInitProfile = false

    fun ensureProfileInitialized() {
        if (didAutoInitProfile) return
        didAutoInitProfile = true
        viewModelScope.launch {
            val existing = try {
                repository.getUserProfile(currentUserId)
            } catch (_: Exception) {
                null
            }
            if (existing == null) {
                val baseName = pendingUsername.ifBlank {
                    initialGoogleName.ifBlank { "Artista${currentUserId.take(4)}" }
                }
                val cleanUsername = baseName.replace(Regex("[^A-Za-z0-9_áéíóúÁÉÍÓÚñÑ ]"), "").trim()
                    .let { if (it.length < 3) "Artista${currentUserId.take(4)}" else it.take(24) }
                repository.saveUserProfile(
                    username = cleanUsername,
                    displayName = initialGoogleName.ifBlank { cleanUsername },
                    email = initialGoogleEmail,
                    bio = pendingBio.ifBlank { "¡Listo para dibujar en JoDiBuj!" },
                    avatarIcon = pendingAvatarIcon,
                    avatarColor = pendingAvatarColor,
                )
            }
        }
    }

    fun saveProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarIcon: String,
        avatarColor: String,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val current = (userProfile.value as? UiState.Success)?.data
            val result = repository.saveUserProfile(
                username = username,
                displayName = displayName,
                email = current?.email ?: initialGoogleEmail,
                bio = bio,
                avatarIcon = avatarIcon,
                avatarColor = avatarColor,
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _actionMessage.value = "¡Perfil actualizado!"
                    onComplete()
                },
                onFailure = { e ->
                    _actionMessage.value = e.message ?: "No se pudo guardar el perfil"
                },
            )
        }
    }

    fun createRoom(
        profile: UserProfile,
        prompt: DrawingPrompt = DrawingCatalog.randomPrompt(),
        totalRounds: Int = 3,
        maxPlayers: Int = 6,
        roundDurationSec: Int = 60,
        onRoomCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createRoom(
                profile = profile,
                prompt = prompt,
                totalRounds = totalRounds,
                maxPlayers = maxPlayers,
                roundDurationSec = roundDurationSec,
            )
            _isBusy.value = false
            result.fold(
                onSuccess = { code -> onRoomCreated(code) },
                onFailure = { e ->
                    _actionMessage.value = e.message ?: "Error al crear la sala"
                },
            )
        }
    }

    fun joinRoom(
        roomCode: String,
        profile: UserProfile,
        onRoomJoined: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.joinRoom(roomCode, profile)
            _isBusy.value = false
            result.fold(
                onSuccess = { code -> onRoomJoined(code) },
                onFailure = { e ->
                    _actionMessage.value = e.message ?: "No se pudo entrar a la sala"
                },
            )
        }
    }

    fun awardSoloLevelXp(xp: Int) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.awardMatchPoints(pointsEarned = xp, wonMatch = false)
            _isBusy.value = false
            result.fold(
                onSuccess = { updated ->
                    _actionMessage.value = "+$xp pts ganados · Nivel ${updated.level}"
                },
                onFailure = { e ->
                    _actionMessage.value = e.message ?: "No se pudieron guardar los puntos"
                },
            )
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private fun extractFirestoreException(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
