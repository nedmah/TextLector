package com.nedmah.textlector.ui.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nedmah.textlector.domain.model.DocumentSortOrder
import com.nedmah.textlector.ui.presentation.components.TopBar
import com.nedmah.textlector.ui.presentation.library.components.FavoriteDocItem
import com.nedmah.textlector.ui.presentation.library.components.LibrarySkeletonScreen
import com.nedmah.textlector.ui.presentation.library.components.SwipeableDocItem
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.ic_sort
import textlector.composeapp.generated.resources.ic_success

@Composable
fun LibraryScreenRoot(
    onNavigateToReader: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { eff ->
            when (eff) {
                is LibraryEffect.NavigateToReader -> onNavigateToReader(eff.documentId)
                is LibraryEffect.NavigateToImport -> onNavigateToImport()
                is LibraryEffect.ShowError -> { /* Snackbar */ }

                LibraryEffect.DocumentDeleted -> { /* Snackbar */
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.isLoading) {
            LibrarySkeletonScreen()
        } else {
            LibraryScreen(state = state, onIntent = viewModel::onEvent)
        }
    }
}

@Composable
private fun LibraryScreen(
    state: LibraryState,
    onIntent: (LibraryIntent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // for MiniPlayer
    ) {

        item {
            TopBar(
                onSearchClick = { /* later search */ }
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.recentDocs.size} items in your collection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Favorites
        if (state.favoriteDocs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader(
                    title = "Favorites",
                    onAction = { /* navigation */ },
                    trailingContent = {
                        Text(
                            text = "see all",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.favoriteDocs,
                        key = { it.id }
                    ) { document ->
                        FavoriteDocItem(
                            document = document,
                            onClick = {
                                onIntent(LibraryIntent.SelectDocument(document.id))
                            },
                            onToggleFavorite = {
                                onIntent(LibraryIntent.ToggleFavorite(document.id, false))
                            }
                        )
                    }
                }
            }
        }

        // Recent Documents
        item {
            Spacer(modifier = Modifier.height(28.dp))

            var sortMenuExpanded by remember { mutableStateOf(false) }

            SectionHeader(
                title = "Recent Documents",
                onAction = null,
                trailingContent = {
                    Box {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sort),
                            contentDescription = null,
                            modifier = Modifier.clickable { sortMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Last Opened") },
                                onClick = {
                                    onIntent(LibraryIntent.ChangeSortType(DocumentSortOrder.LAST_OPENED))
                                    sortMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (state.sortOrder == DocumentSortOrder.LAST_OPENED) {
                                        Icon(
                                            modifier = Modifier.size(12.dp),
                                            painter = painterResource(Res.drawable.ic_success),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added") },
                                onClick = {
                                    onIntent(LibraryIntent.ChangeSortType(DocumentSortOrder.CREATED_AT))
                                    sortMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (state.sortOrder == DocumentSortOrder.CREATED_AT) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_success),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(
            items = state.recentDocs,
            key = { it.id }
        ) { document ->

            SwipeableDocItem(
                document = document,
                onClick = { onIntent(LibraryIntent.SelectDocument(document.id)) },
                onDelete = { onIntent(LibraryIntent.DeleteDocument(document.id)) },
                onFavorite = {
                    onIntent(LibraryIntent.ToggleFavorite(document.id, !document.isFavorite))
                }
            )
        }
    }

}

@Composable
private fun SectionHeader(
    title: String,
    trailingContent: @Composable ((Modifier) -> Unit)?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (trailingContent != null) {
            val modifier = if (onAction != null) {
                Modifier.clickable(onClick = onAction)
            } else {
                Modifier
            }
            trailingContent(modifier)
        }
    }
}