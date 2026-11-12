# AbhayHealth ProGuard / R8 Optimization Rules

# Preserve Room entities and DAOs
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
}
-keep class in.abhayhealth.app.feature.records.MedicalRecord { *; }
-keep class in.abhayhealth.app.feature.records.ExtractedMedicalInfo { *; }
-keep class in.abhayhealth.app.feature.medicines.PrescriptionMedicationEntity { *; }

# Preserve Data Models for Navigation and Preferences
-keep class in.abhayhealth.app.domain.model.PatientProfile { *; }
-keep class in.abhayhealth.app.domain.model.AuthUser { *; }
-keep class in.abhayhealth.app.domain.model.ParsedMedicalResult { *; }

# Keep Firebase Auth classes
-keep class com.google.firebase.auth.** { *; }
