/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/** Routes for the profile screen (the first-run welcome is shown by BaajaWelcomeGate). */
fun NavGraphBuilder.onboardingRoutes(navController: NavHostController) {
    composable("profile") {
        ProfileScreen(navController)
    }
}
