package com.nedmah.textlector.common.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nedmah.textlector.ui.presentation.components.BottomNavBar
import com.nedmah.textlector.ui.presentation.import_from.ImportScreenRoot
import com.nedmah.textlector.ui.presentation.library.LibraryScreenRoot
import com.nedmah.textlector.ui.presentation.reader.ReaderScreenRoot
import com.nedmah.textlector.ui.presentation.settings.SettingsScreenRoot

@Composable
fun TextLectorNavGraph() {

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()

    val showBottomBar = currentBackStack?.destination?.route in listOf(
        LibraryRoute::class.qualifiedName,
        ImportRoute::class.qualifiedName,
        SettingsRoute::class.qualifiedName
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                com.nedmah.textlector.ui.presentation.components.BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = LibraryRoute
            ) {
                composable<LibraryRoute> {
                    com.nedmah.textlector.ui.presentation.library.LibraryScreenRoot(
                        onNavigateToReader = { documentId ->
                            navController.navigate(ReaderRoute(documentId))
                        },
                        onNavigateToImport = {
                            navController.navigate(ImportRoute)
                        }
                    )
                }

                composable<ImportRoute> {
                    com.nedmah.textlector.ui.presentation.import_from.ImportScreenRoot(
                        onNavigateToReader = { documentId ->
                            navController.navigate(ReaderRoute(documentId))
                        }
                    )
                }

                composable<ReaderRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<ReaderRoute>()
                    com.nedmah.textlector.ui.presentation.reader.ReaderScreenRoot(
                        documentId = route.documentId,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<SettingsRoute> {
                    com.nedmah.textlector.ui.presentation.settings.SettingsScreenRoot()
                }
            }

            if (showBottomBar) {
//                MiniPlayer(
//                    modifier = Modifier.align(Alignment.BottomCenter),
//                    onTap = { documentId ->
//                        navController.navigate(ReaderRoute(documentId)) {
//                            launchSingleTop = true
//                        }
//                    }
//                )
            }
        }
    }

}
