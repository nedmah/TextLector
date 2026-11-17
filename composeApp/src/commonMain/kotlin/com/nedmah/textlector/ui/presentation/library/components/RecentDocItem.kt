package com.nedmah.textlector.ui.presentation.library.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nedmah.textlector.domain.model.Document
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.action_add_favorite
import textlector.composeapp.generated.resources.action_delete
import textlector.composeapp.generated.resources.action_more
import textlector.composeapp.generated.resources.action_remove_favorite
import textlector.composeapp.generated.resources.action_rename
import textlector.composeapp.generated.resources.ic_delete
import textlector.composeapp.generated.resources.ic_edit
import textlector.composeapp.generated.resources.ic_more_ver
import textlector.composeapp.generated.resources.ic_star_empty
import textlector.composeapp.generated.resources.ic_star_filled
import textlector.composeapp.generated.resources.ic_success
import textlector.composeapp.generated.resources.library_reading_time


@Composable
fun SwipeableDocItem(
    document: Document,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onClick: () -> Unit,
    onLongClick : () -> Unit,
    modifier: Modifier = Modifier
) {

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.StartToEnd -> {
                onFavorite()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> DeleteBackground()
                SwipeToDismissBoxValue.StartToEnd -> FavoriteBackground(document.isFavorite)
                else -> {}
            }
        }
    ) {
        RecentDocItem(
            document = document,
            onClick = onClick,
            onLongClick = onLongClick,
            onFavorite = onFavorite,
            onDelete = onDelete,
        )
    }

}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_delete),
            contentDescription = "Delete",
            tint = Color.White,
            modifier = Modifier.padding(end = 20.dp)
        )
    }
}

@Composable
private fun FavoriteBackground(isFavorite: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            painter = painterResource(
                if (isFavorite) Res.drawable.ic_star_filled
                else Res.drawable.ic_star_empty
            ),
            contentDescription = "Favorite",
            tint = Color.White,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}


@Composable
private fun RecentDocItem(
    document: Document,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = if (document.totalParagraphs > 0) {
        document.lastParagraphIndex.toFloat() / document.totalParagraphs
    } else 0f

    val isCompleted = progress >= 1f

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .graphicsLayer{ scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
        ,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                DocTypeIcon(sourceType = document.sourceType)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = stringResource(Res.string.library_reading_time, document.estimatedReadingMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DocActionsMenu(
                    isFavorite = document.isFavorite,
                    onRename = onLongClick,
                    onFavorite = onFavorite,
                    onDelete = onDelete,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline
                )

                if (isCompleted) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_success),
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DocActionsMenu(
    isFavorite: Boolean,
    onRename: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_ver),
                contentDescription = stringResource(Res.string.action_more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_rename)) },
                leadingIcon = {
                    Icon(painterResource(Res.drawable.ic_edit), null, Modifier.size(20.dp))
                },
                onClick = { expanded = false; onRename() }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isFavorite) Res.string.action_remove_favorite
                            else Res.string.action_add_favorite
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        painterResource(
                            if (isFavorite) Res.drawable.ic_star_filled else Res.drawable.ic_star_empty
                        ),
                        null,
                        Modifier.size(20.dp)
                    )
                },
                onClick = { expanded = false; onFavorite() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        painterResource(Res.drawable.ic_delete),
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}