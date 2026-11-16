package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.guestDataStore by preferencesDataStore(name = "jodibuj_guest")

object GuestStore {
    private val KEY_IS_GUEST = booleanPreferencesKey("is_guest_active")
    private val KEY_USERNAME = stringPreferencesKey("guest_username")
    private val KEY_BIO = stringPreferencesKey("guest_bio")
    private val KEY_AVATAR_ICON = stringPreferencesKey("guest_avatar_icon")
    private val KEY_AVATAR_COLOR = stringPreferencesKey("guest_avatar_color")
    private val KEY_POINTS = longPreferencesKey("guest_points")
    private val KEY_GAMES = longPreferencesKey("guest_games")
    private val KEY_WINS = longPreferencesKey("guest_wins")
    private val KEY_LOSSES = longPreferencesKey("guest_losses")
    private val KEY_COMPLETED_LEVEL = longPreferencesKey("guest_completed_level")

    fun observeGuestActive(context: Context): Flow<Boolean> {
        return context.applicationContext.guestDataStore.data.map { prefs ->
            prefs[KEY_IS_GUEST] ?: false
        }
    }

    fun observeGuestProfile(context: Context): Flow<UserProfile> {
        return context.applicationContext.guestDataStore.data.map { prefs ->
            val points = prefs[KEY_POINTS] ?: 0L
            val username = prefs[KEY_USERNAME]?.ifBlank { "Invitado" } ?: "Invitado"
            val games = prefs[KEY_GAMES] ?: 0L
            val wins = prefs[KEY_WINS] ?: 0L
            val losses = prefs[KEY_LOSSES] ?: (games - wins).coerceAtLeast(0L)
            val completedLvl = prefs[KEY_COMPLETED_LEVEL] ?: 1L
            UserProfile(
                id = "guest_local",
                userId = "guest_local",
                username = username,
                displayName = username,
                email = "",
                bio = prefs[KEY_BIO] ?: "Modo invitado en JoDiBuj",
                avatarIcon = prefs[KEY_AVATAR_ICON] ?: "brush",
                avatarColor = prefs[KEY_AVATAR_COLOR] ?: "#1CB0F6",
                totalPoints = points,
                level = LevelSystem.levelForPoints(points).toLong(),
                completedLevel = completedLvl,
                gamesPlayed = games,
                wins = wins,
                losses = losses,
                currentStreak = 1L,
            )
        }
    }

    suspend fun startGuestSession(
        context: Context,
        username: String,
        bio: String,
        avatarIcon: String,
        avatarColor: String,
    ) {
        val cleanName = username.trim().ifBlank { "Invitado" }.take(24)
        context.applicationContext.guestDataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = true
            prefs[KEY_USERNAME] = cleanName
            if (bio.isNotBlank()) prefs[KEY_BIO] = bio.trim().take(120)
            prefs[KEY_AVATAR_ICON] = avatarIcon
            prefs[KEY_AVATAR_COLOR] = avatarColor
        }
    }

    suspend fun updateGuestProfile(
        context: Context,
        username: String,
        bio: String,
        avatarIcon: String,
        avatarColor: String,
    ) {
        val cleanName = username.trim().ifBlank { "Invitado" }.take(24)
        context.applicationContext.guestDataStore.edit { prefs ->
            prefs[KEY_USERNAME] = cleanName
            prefs[KEY_BIO] = bio.trim().take(120)
            prefs[KEY_AVATAR_ICON] = avatarIcon
            prefs[KEY_AVATAR_COLOR] = avatarColor
        }
    }

    suspend fun addGuestPoints(context: Context, xp: Int, wonMatch: Boolean = true, levelCompleted: Int? = null) {
        context.applicationContext.guestDataStore.edit { prefs ->
            val current = prefs[KEY_POINTS] ?: 0L
            val games = prefs[KEY_GAMES] ?: 0L
            val wins = prefs[KEY_WINS] ?: 0L
            val losses = prefs[KEY_LOSSES] ?: 0L
            val completedLvl = prefs[KEY_COMPLETED_LEVEL] ?: 1L

            prefs[KEY_POINTS] = (current + xp.coerceAtLeast(0)).coerceAtMost(10_000_000L)
            prefs[KEY_GAMES] = games + 1L
            if (wonMatch) {
                prefs[KEY_WINS] = wins + 1L
            } else {
                prefs[KEY_LOSSES] = losses + 1L
            }
            if (levelCompleted != null && levelCompleted >= completedLvl) {
                prefs[KEY_COMPLETED_LEVEL] = (levelCompleted + 1).toLong()
            }
        }
    }

    suspend fun exitGuestSession(context: Context) {
        context.applicationContext.guestDataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = false
        }
    }
}
