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
import com.nedmah.textlector.common.navigation.isTopLevel
import com.nedmah.textlector.common.navigation.navigateToTopLevel
import com.nedmah.textlector.common.navigation.topLevelDestinations
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavBar(navController: NavController) {

    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination

    NavigationBar {
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            topLevelDestinations.forEach { destination ->
                NavigationBarItem(
                    selected = currentDestination.isTopLevel(destination.route),
                    onClick = {
                        navController.navigateToTopLevel(destination.route)
                    },
                    icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
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