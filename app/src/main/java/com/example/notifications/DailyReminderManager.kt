package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

private val Context.reminderDataStore by preferencesDataStore(name = "jodibuj_reminders")

data class ReminderSettings(
    val enabled: Boolean = true,
    val hour: Int = 19,
    val minute: Int = 0,
)

object DailyReminderManager {
    const val CHANNEL_ID = "jodibuj_daily_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 2001

    private val KEY_ENABLED = booleanPreferencesKey("daily_enabled")
    private val KEY_HOUR = intPreferencesKey("daily_hour")
    private val KEY_MINUTE = intPreferencesKey("daily_minute")

    fun observeSettings(context: Context): Flow<ReminderSettings> {
        return context.applicationContext.reminderDataStore.data.map { prefs ->
            ReminderSettings(
                enabled = prefs[KEY_ENABLED] ?: true,
                hour = prefs[KEY_HOUR] ?: 19,
                minute = prefs[KEY_MINUTE] ?: 0,
            )
        }
    }

    suspend fun saveAndSchedule(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val appContext = context.applicationContext
        appContext.reminderDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
            prefs[KEY_HOUR] = hour.coerceIn(0, 23)
            prefs[KEY_MINUTE] = minute.coerceIn(0, 59)
        }
        ensureChannel(appContext)
        if (enabled) {
            scheduleDailyAlarm(appContext, hour, minute)
        } else {
            cancelDailyAlarm(appContext)
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent,
        )
    }

    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildAlarmPendingIntent(context))
    }

    fun showDailyNotificationNow(context: Context): Boolean {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val tapIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_art)
            .setContentTitle(appContext.getString(R.string.notification_daily_title))
            .setContentText(appContext.getString(R.string.notification_daily_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyReminderManager.showDailyNotificationNow(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = DailyReminderManager.observeSettings(context).first()
                    if (settings.enabled) {
                        DailyReminderManager.ensureChannel(context)
                        DailyReminderManager.scheduleDailyAlarm(
                            context,
                            settings.hour,
                            settings.minute,
                        )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
