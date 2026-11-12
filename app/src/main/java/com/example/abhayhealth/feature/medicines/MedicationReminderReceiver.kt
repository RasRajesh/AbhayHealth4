package com.example.abhayhealth.feature.medicines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.abhayhealth.feature.records.MedicalRecordDatabase

class MedicationReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MEDICATION_REMINDER = "com.example.abhayhealth.ACTION_MEDICATION_REMINDER"
        const val ACTION_MARK_TAKEN = "com.example.abhayhealth.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE_REMINDER = "com.example.abhayhealth.ACTION_SNOOZE_REMINDER"

        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_MEDICATION_DOSAGE = "extra_medication_dosage"
        const val EXTRA_MEDICATION_SLOT = "extra_medication_slot"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        private const val TAG = "MedicationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "onReceive triggered with action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Device reboot or package update detected. Rescheduling alarms...")
                MedicationAlarmScheduler.rescheduleAllAlarms(context)
            }

            ACTION_MEDICATION_REMINDER -> {
                val medId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
                val name = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Prescription Medicine"
                val dosage = intent.getStringExtra(EXTRA_MEDICATION_DOSAGE) ?: "1 Dose"
                val slot = intent.getStringExtra(EXTRA_MEDICATION_SLOT) ?: "Scheduled Time"

                // 1. Fire heads-up notification
                MedicationNotificationHelper.showMedicationNotification(
                    context = context,
                    medicationId = medId,
                    name = name,
                    dosage = dosage,
                    timeSlot = slot
                )

                // 2. Automatically schedule next day's alarm for this daily medicine slot
                if (!slot.contains("Snoozed", ignoreCase = true)) {
                    MedicationAlarmScheduler.scheduleMedicationAlarm(
                        context = context,
                        medicationId = medId,
                        name = name,
                        dosage = dosage,
                        timeSlot = slot
                    )
                }
            }

            ACTION_MARK_TAKEN -> {
                val medId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                val medName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medicine"

                if (notifId != -1) {
                    MedicationNotificationHelper.dismissNotification(context, notifId)
                }

                // Update database
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = MedicalRecordDatabase.getInstance(context)
                        db.medicationDao().markDoseTaken(medId)
                        Log.d(TAG, "Marked dose as taken for medication $medId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to mark dose taken", e)
                    }
                }

                Toast.makeText(context, "✓ $medName marked as taken", Toast.LENGTH_SHORT).show()
            }

            ACTION_SNOOZE_REMINDER -> {
                val medId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                val name = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(EXTRA_MEDICATION_DOSAGE) ?: "1 Dose"
                val slot = intent.getStringExtra(EXTRA_MEDICATION_SLOT) ?: "Scheduled Time"

                if (notifId != -1) {
                    MedicationNotificationHelper.dismissNotification(context, notifId)
                }

                MedicationAlarmScheduler.scheduleSnooze(
                    context = context,
                    medicationId = medId,
                    name = name,
                    dosage = dosage,
                    timeSlot = slot,
                    snoozeMinutes = 10
                )

                Toast.makeText(context, "⏰ Reminder snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
