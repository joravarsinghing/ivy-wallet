package com.ivy.navigation

/** Setup is a workflow boundary, never a substitute for the independent app-lock gate. */
fun Screen.canResumeBeforeSetup(): Boolean = when (this) {
    OnboardingScreen, DisclaimerScreen -> true
    is CSVScreen -> launchedFromOnboarding
    is ImportScreen -> launchedFromOnboarding
    else -> false
}
