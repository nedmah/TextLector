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
import org.jetbrains.compose.resources.painterResource
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.ic_import
import textlector.composeapp.generated.resources.ic_library
import textlector.composeapp.generated.resources.ic_settings

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
        BottomNavItem(
            "Library",
            LibraryRoute
        ) {
             Icon(painterResource(Res.drawable.ic_library), contentDescription = "Library")
        },
        BottomNavItem(
            "Import",
            ImportRoute
        ) {
             Icon(painterResource(Res.drawable.ic_import), contentDescription = "Import")
        },
        BottomNavItem(
            "Settings",
            SettingsRoute
        ) {
             Icon(painterResource(Res.drawable.ic_settings), contentDescription = "Settings")
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