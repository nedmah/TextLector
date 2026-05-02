package com.nedmah.textlector.common.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nedmah.textlector.ui.presentation.components.BottomNavBar
import com.nedmah.textlector.ui.presentation.components.MiniPlayer
import com.nedmah.textlector.ui.presentation.player.PlayerIntent
import com.nedmah.textlector.ui.presentation.player.PlayerViewModel
import org.koin.compose.koinInject

@Composable
fun TextLectorNavGraph() {

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()

    val playerViewModel : PlayerViewModel = koinInject()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    val showBottomBar = currentBackStack?.destination?.route in listOf(
        LibraryRoute::class.qualifiedName,
        ImportRoute::class.qualifiedName,
        SettingsRoute::class.qualifiedName
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    if (playerState.isLoaded) {
                        MiniPlayer(
                            title = playerState.document?.title ?: "",
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                            progress = playerState.progress,
                            onTap = {
                                playerState.document?.id?.let { id ->
                                    navController.navigate(ReaderRoute(id)) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onPlayPause = {
                                if (playerState.isPlaying)
                                    playerViewModel.onIntent(PlayerIntent.Pause)
                                else
                                    playerViewModel.onIntent(PlayerIntent.Play)
                            }
                        )
                    }
                    BottomNavBar(navController = navController)
                }

            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = LibraryRoute,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
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

                composable<ReaderRoute>(
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(320)
                        )
                    },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(320)
                        )
                    }
                ) { backStackEntry ->
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
        }
    }

}
