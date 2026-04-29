package com.example.streamfilx_androidtv.features.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.features.home.ContentCard

private const val LOAD_MORE_THRESHOLD = 10

@Composable
private fun adaptiveGridColumns(): Int {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidthDp >= 1200 -> 6  // large tablet / TV landscape
        screenWidthDp >= 840  -> 5  // tablet landscape
        screenWidthDp >= 600  -> 4  // tablet portrait
        screenWidthDp >= 480  -> 3  // large phone landscape
        else                  -> 2  // phone portrait
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentGrid(
    items: List<MetaPreview>,
    onItemClick: (MetaPreview) -> Unit,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    if (items.isEmpty() && !isLoadingMore) {
        emptyContent?.invoke()
        return
    }

    val gridState = rememberLazyGridState()

    // Detect when user is near the end of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= items.size - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMore && !isLoadingMore) {
            onLoadMore?.invoke()
        }
    }

    val columns = adaptiveGridColumns()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { meta ->
            ContentCard(
                meta = meta,
                onClick = { onItemClick(meta) },
            )
        }

        if (isLoadingMore) {
            item(span = { GridItemSpan(columns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFFE50914))
                }
            }
        }
    }
}

// ── Shimmer placeholder grid shown while initial load is happening ────────────

@Composable
fun ShimmerContentGrid(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptiveGridColumns()),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
    ) {
        items(16) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(240.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = alpha)),
            )
        }
    }
}
