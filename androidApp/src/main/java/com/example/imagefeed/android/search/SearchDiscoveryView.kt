package com.example.imagefeed.android.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagefeed.android.R
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.calculateBottomContentPadding
import com.example.imagefeed.android.designsystem.MatteSurface
import com.example.imagefeed.android.util.bounceClick

private val POPULAR_TOPICS =
    listOf(
        "nature",
        "travel",
        "architecture",
        "wallpapers",
        "neon",
        "minimalist",
        "urban",
        "textures",
    )

@Composable
fun SearchSuggestionsAndHistory(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = calculateBottomContentPadding(16.dp),
                ).verticalScroll(rememberScrollState()),
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.search_section_recents),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    stringResource(R.string.search_action_clear_all),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.bounceClick { onClearAll() },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            history.forEach { item ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .bounceClick { onItemClick(item) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.content_desc_history),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onDeleteClick(item) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.content_desc_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            stringResource(R.string.search_section_popular_topics),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))

        val isCompact = LocalAdaptiveLayoutInfo.current.isCompactWidth
        Grid(
            modifier = Modifier.fillMaxWidth(),
            config = {
                repeat(if (isCompact) 2 else 4) { column(1.fr) }
                gap(10.dp)
            },
        ) {
            POPULAR_TOPICS.forEachIndexed { index, topic ->
                PopularTopicMatteTile(
                    topic = topic,
                    index = index,
                    onClick = { onItemClick(topic) },
                )
            }
        }
    }
}

@Composable
private fun PopularTopicMatteTile(
    topic: String,
    index: Int,
    onClick: () -> Unit,
) {
    val (containerColor, contentColor) =
        when (index % 4) {
            0 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            1 -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
            2 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }

    MatteSurface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .bounceClick(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = topic.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
