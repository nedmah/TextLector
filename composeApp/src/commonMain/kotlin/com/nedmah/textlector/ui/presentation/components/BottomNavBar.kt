package com.nedmah.textlector.ui.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nedmah.textlector.common.navigation.ImportRoute
import com.nedmah.textlector.common.navigation.LibraryRoute
import com.nedmah.textlector.common.navigation.SettingsRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.ic_import
import textlector.composeapp.generated.resources.ic_library
import textlector.composeapp.generated.resources.ic_settings
import textlector.composeapp.generated.resources.nav_import
import textlector.composeapp.generated.resources.nav_library
import textlector.composeapp.generated.resources.nav_settings

data class BottomNavItem(
    val route: Any,
    val icon: @Composable () -> Unit
)

@Composable
fun BottomNavBar(navController: NavController) {

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val items = listOf(
        BottomNavItem(
            LibraryRoute
        ) {
            Icon(painterResource(Res.drawable.ic_library), contentDescription = "Library")
        },
        BottomNavItem(
            ImportRoute
        ) {
            Icon(painterResource(Res.drawable.ic_import), contentDescription = "Import")
        },
        BottomNavItem(
            SettingsRoute
        ) {
            Icon(painterResource(Res.drawable.ic_settings), contentDescription = "Settings")
        }
    )

    NavigationBar {
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute?.contains(
                        item.route::class.qualifiedName ?: ""
                    ) == true,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(LibraryRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = item.icon,
                    label = {
                        Text(
                            stringResource(
                                when (item.route) {
                                    is LibraryRoute -> Res.string.nav_library
                                    is ImportRoute -> Res.string.nav_import
                                    else -> Res.string.nav_settings
                                }
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }
        }
    }

}