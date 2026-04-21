package com.nedmah.textlector.ui.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nedmah.textlector.domain.model.SourceType
import org.jetbrains.compose.resources.painterResource
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.ic_camera
import textlector.composeapp.generated.resources.ic_epub
import textlector.composeapp.generated.resources.ic_fb2
import textlector.composeapp.generated.resources.ic_pdf_doc
import textlector.composeapp.generated.resources.ic_text_doc
import textlector.composeapp.generated.resources.ic_url

@Composable
fun DocTypeIcon(
    sourceType: SourceType,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (sourceType) {
        is SourceType.Pdf -> painterResource(Res.drawable.ic_pdf_doc) to MaterialTheme.colorScheme.error
        is SourceType.Url -> painterResource(Res.drawable.ic_url) to MaterialTheme.colorScheme.primary
        is SourceType.Epub -> painterResource(Res.drawable.ic_epub) to MaterialTheme.colorScheme.primary
        is SourceType.Fb2 -> painterResource(Res.drawable.ic_fb2) to MaterialTheme.colorScheme.primary
        SourceType.Camera -> painterResource(Res.drawable.ic_camera) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> painterResource(Res.drawable.ic_text_doc) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}