package com.example.abhayhealth.feature.medicines

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.abhayhealth.feature.records.MedicalRecordDatabase
import java.util.Calendar

data class ScheduledSlotInfo(
    val slotTag: String,
    val displayName: String,
    val timeLabel: String,
    val hour: Int,
    val minute: Int
)

object MedicationAlarmScheduler {

    private const val TAG = "MedicationAlarmScheduler"

    val DEFAULT_SLOTS = listOf(
        ScheduledSlotInfo("MORNING", "Morning", "08:30 AM", 8, 30),
        ScheduledSlotInfo("AFTERNOON", "Afternoon", "01:30 PM", 13, 30),
        ScheduledSlotInfo("EVENING", "Evening", "07:00 PM", 19, 0),
        ScheduledSlotInfo("NIGHT", "Night", "09:30 PM", 21, 30)
    )

    fun formatTimeLabel(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(java.util.Locale.US, "%02d:%02d %s", h12, minute, amPm)
    }

    fun parseSlotTime(timeSlot: String): Pair<Int, Int> {
        val upper = timeSlot.uppercase()
        val isPm = upper.contains("PM")
        val isAm = upper.contains("AM")

        val regex = Regex("(\\d{1,2}):(\\d{2})")
        val match = regex.find(timeSlot)
        if (match != null) {
            val (hStr, mStr) = match.destructured
            var h = hStr.toIntOrNull() ?: 8
            val m = mStr.toIntOrNull() ?: 30
            if (isPm && h < 12) {
                h += 12
            } else if (isAm && h == 12) {
                h = 0
            }
            return Pair(h.coerceIn(0, 23), m.coerceIn(0, 59))
        }

        return when {
            upper.contains("MORNING") -> Pair(8, 30)
            upper.contains("AFTERNOON") -> Pair(13, 30)
            upper.contains("EVENING") -> Pair(19, 0)
            upper.contains("NIGHT") -> Pair(21, 30)
            else -> Pair(9, 0)
        }
    }

    fun calculateNextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun scheduleMedicationAlarm(
        context: Context,
        medicationId: String,
        name: String,
        dosage: String,
        timeSlot: String
    ) {
        val (hour, minute) = parseSlotTime(timeSlot)
        val triggerAtMillis = calculateNextTriggerTime(hour, minute)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = MedicationReminderReceiver.ACTION_MEDICATION_REMINDER
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, name)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_SLOT, timeSlot)
        }

        val requestCode = kotlin.math.abs(medicationId.hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for $name at $triggerAtMillis (hour=$hour, min=$minute)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to windowed alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelMedicationAlarm(context: Context, medicationId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = MedicationReminderReceiver.ACTION_MEDICATION_REMINDER
        }
        val requestCode = kotlin.math.abs(medicationId.hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for medication $medicationId")
        }
    }

    fun scheduleSnooze(
        context: Context,
        medicationId: String,
        name: String,
        dosage: String,
        timeSlot: String,
        snoozeMinutes: Int = 10
    ) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = MedicationReminderReceiver.ACTION_MEDICATION_REMINDER
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, name)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_SLOT, "$timeSlot (Snoozed $snoozeMinutes min)")
        }
        val requestCode = kotlin.math.abs((medicationId + "_snooze").hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        Log.d(TAG, "Snoozed alarm for $name in $snoozeMinutes minutes")
    }

    fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MedicalRecordDatabase.getInstance(context)
                val allMedications = db.medicationDao().getAllMedicationsSync()
                Log.d(TAG, "Rescheduling ${allMedications.size} medications from database")
                for (med in allMedications) {
                    scheduleMedicationAlarm(
                        context = context,
                        medicationId = med.id,
                        name = med.name,
                        dosage = med.dosage,
                        timeSlot = med.timeSlot
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling medications", e)
            }
        }
    }
}
