package com.nedmah.textlector.ui.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nedmah.textlector.common.navigation.ImportRoute
import com.nedmah.textlector.common.navigation.LibraryRoute
import com.nedmah.textlector.common.navigation.SettingsRoute

data class BottomNavItem(
    val label: String,
    val route: Any,
    val icon: @Composable () -> Unit
)

@Composable
fun BottomNavBar(navController: NavController) {

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val items = listOf(
        _root_ide_package_.com.nedmah.textlector.ui.presentation.components.BottomNavItem(
            "Library",
            LibraryRoute
        ) {
//             Icon(Icons.Default.MenuBook, contentDescription = "Library")
        },
        _root_ide_package_.com.nedmah.textlector.ui.presentation.components.BottomNavItem(
            "Import",
            ImportRoute
        ) {
//             Icon(Icons.Default.Add, contentDescription = "Import")
        },
        _root_ide_package_.com.nedmah.textlector.ui.presentation.components.BottomNavItem(
            "Settings",
            SettingsRoute
        ) {
//             Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    )

    NavigationBar {
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
                label = { Text(item.label) }
            )
        }
    }

}