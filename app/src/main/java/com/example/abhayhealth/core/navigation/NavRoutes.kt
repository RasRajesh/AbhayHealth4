package com.example.abhayhealth.core.navigation

/**
 * Route definitions for AbhayHealth navigation.
 */
object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val WELCOME = "welcome"
    const val ENTER_PHONE = "enter_phone"
    const val VERIFY_OTP = "verify_otp"
    const val PROFILE_SETUP = "profile_setup"
    const val PROFILE_EDIT = "profile_edit"
    const val EXTRACTION_REVIEW = "extraction_review/{recordId}"
    const val MAIN = "main"

    fun extractionReview(recordId: Long) = "extraction_review/$recordId"

    // Bottom Navigation destinations inside Main Shell
    object MainTab {
        const val HOME = "home_tab"
        const val AI_ASSISTANT = "ai_assistant_tab"
        const val RECORDS = "records_tab"
        const val TRENDS = "trends_tab"
        const val MEDICINES = "medicines_tab"
        const val ME = "me_tab"
    }
}
