package com.nedmah.textlector.ui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nedmah.textlector.common.navigation.isTopLevel
import com.nedmah.textlector.common.navigation.navigateToTopLevel
import com.nedmah.textlector.common.navigation.topLevelDestinations
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.app_name

@Composable
fun SideNavBar(navController: NavController, modifier: Modifier = Modifier) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 24.dp)
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(32.dp))

        topLevelDestinations.forEach { destination ->
            val selected = currentDestination.isTopLevel(destination.route)
            val color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { navController.navigateToTopLevel(destination.route) }
                    .drawBehind {
                        if (selected) {
                            val barWidth = 3.dp.toPx()
                            drawRect(accent, Offset(size.width - barWidth, 0f), Size(barWidth, size.height))
                        }
                    }
                    .padding(horizontal = 24.dp)
            ) {
                Icon(painterResource(destination.icon), null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(destination.label), color = color, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}