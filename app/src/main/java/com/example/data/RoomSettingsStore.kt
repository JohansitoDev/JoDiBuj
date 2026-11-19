package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.roomSettingsDataStore by preferencesDataStore(name = "jodibuj_room_settings")

data class RoomSettings(
    val defaultMaxPlayers: Int = 6,
    val defaultTotalRounds: Int = 3,
    val defaultRoundDurationSec: Int = 60,
    val soundEffectsEnabled: Boolean = true,
)

object RoomSettingsStore {
    private val KEY_MAX_PLAYERS = intPreferencesKey("default_max_players")
    private val KEY_TOTAL_ROUNDS = intPreferencesKey("default_total_rounds")
    private val KEY_ROUND_DURATION = intPreferencesKey("default_round_duration_sec")
    private val KEY_SOUND_EFFECTS = booleanPreferencesKey("sound_effects_enabled")

    fun observeRoomSettings(context: Context): Flow<RoomSettings> {
        return context.applicationContext.roomSettingsDataStore.data.map { prefs ->
            RoomSettings(
                defaultMaxPlayers = prefs[KEY_MAX_PLAYERS] ?: 6,
                defaultTotalRounds = prefs[KEY_TOTAL_ROUNDS] ?: 3,
                defaultRoundDurationSec = prefs[KEY_ROUND_DURATION] ?: 60,
                soundEffectsEnabled = prefs[KEY_SOUND_EFFECTS] ?: true,
            )
        }
    }

    suspend fun saveRoomSettings(
        context: Context,
        maxPlayers: Int,
        totalRounds: Int,
        roundDurationSec: Int,
        soundEffectsEnabled: Boolean = true,
    ) {
        context.applicationContext.roomSettingsDataStore.edit { prefs ->
            prefs[KEY_MAX_PLAYERS] = maxPlayers.coerceIn(2, 20)
            prefs[KEY_TOTAL_ROUNDS] = totalRounds.coerceIn(1, 10)
            prefs[KEY_ROUND_DURATION] = roundDurationSec.coerceIn(15, 300)
            prefs[KEY_SOUND_EFFECTS] = soundEffectsEnabled
        }
    }
}
