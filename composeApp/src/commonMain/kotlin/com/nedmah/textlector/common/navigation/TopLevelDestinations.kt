package com.nedmah.textlector.common.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.ic_import
import textlector.composeapp.generated.resources.ic_library
import textlector.composeapp.generated.resources.ic_settings
import textlector.composeapp.generated.resources.nav_import
import textlector.composeapp.generated.resources.nav_library
import textlector.composeapp.generated.resources.nav_settings

data class TopLevelDestination(
    val route: Any,
    val icon: DrawableResource,
    val label: StringResource,
)

val topLevelDestinations = listOf(
    TopLevelDestination(LibraryRoute, Res.drawable.ic_library, Res.string.nav_library),
    TopLevelDestination(ImportRoute, Res.drawable.ic_import, Res.string.nav_import),
    TopLevelDestination(SettingsRoute, Res.drawable.ic_settings, Res.string.nav_settings),
)

fun NavController.navigateToTopLevel(route: Any) = navigate(route) {
    popUpTo(LibraryRoute) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

fun NavDestination?.isTopLevel(route: Any): Boolean =
    this?.route?.contains(route::class.qualifiedName ?: "") == true