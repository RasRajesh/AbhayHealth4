package com.example.abhayhealth.feature.medicines

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.abhayhealth.MainActivity
import com.example.abhayhealth.R

object MedicationNotificationHelper {

    const val CHANNEL_ID = "medication_reminders_channel"
    private const val CHANNEL_NAME = "Medication Reminders"
    private const val CHANNEL_DESC = "Timely alerts reminding you to take your prescribed medicines"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 200, 450)
                setSound(soundUri, null)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showMedicationNotification(
        context: Context,
        medicationId: String,
        name: String,
        dosage: String,
        timeSlot: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Notification permission not yet granted by user
                return
            }
        }

        val notificationId = kotlin.math.abs(medicationId.hashCode())

        // Intent to open MainActivity when user taps the notification
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "medicines")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Taken
        val markTakenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = MedicationReminderReceiver.ACTION_MARK_TAKEN
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, name)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze (10 minutes)
        val snoozeIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = MedicationReminderReceiver.ACTION_SNOOZE_REMINDER
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, name)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_SLOT, timeSlot)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💊 Medicine Reminder: $name")
            .setContentText("Dosage: $dosage • $timeSlot")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to take your scheduled medicine:\n\n• Medicine: $name\n• Dosage: $dosage\n• Schedule: $timeSlot\n\nPlease take it with water as prescribed.")
                    .setSummaryText("AbhayHealth Medication")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 450, 200, 450))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "✓ Taken", markTakenPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "⏰ Snooze 10m", snoozePendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, builder.build())
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}
