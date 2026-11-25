package com.example.imagefeed.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.theme.ExpressiveShapeTokens

private val PlaceholderBackdropSize = 112.dp
private val PlaceholderIconSize = 44.dp

@Composable
fun PhotoDetailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(PlaceholderBackdropSize)
                        .clip(ExpressiveShapeTokens.SoftScallopShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(PlaceholderIconSize),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.details_placeholder_prompt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 450, heightDp = 640, name = "Placeholder - Light")
@Composable
fun PhotoDetailPlaceholderLightPreview() {
    PreviewScaffold(widthDp = 450, heightDp = 640, darkTheme = false) { _, _ ->
        PhotoDetailPlaceholder()
    }
}

@Preview(showBackground = true, widthDp = 450, heightDp = 640, name = "Placeholder - Dark")
@Composable
fun PhotoDetailPlaceholderDarkPreview() {
    PreviewScaffold(widthDp = 450, heightDp = 640, darkTheme = true) { _, _ ->
        PhotoDetailPlaceholder()
    }
}
