package com.example.streamfilx_androidtv.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.core.models.WatchHistoryItem
import com.example.streamfilx_androidtv.navigation.Screen

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onPlayClick: (MetaPreview) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.isLoading -> ShimmerHomeScreen()
            uiState.error != null -> ErrorScreen(
                message = uiState.error!!,
                onRetry = viewModel::loadContent,
            )
            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                HomeContent(
                    uiState = uiState,
                    onItemClick = { meta ->
                        navController.navigate(Screen.Detail.route(meta.type, meta.id))
                    },
                    onPlayClick = onPlayClick,
                    onHistoryClick = { item ->
                        navController.navigate(Screen.Detail.route(item.contentType, item.contentId))
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onItemClick: (MetaPreview) -> Unit,
    onPlayClick: (MetaPreview) -> Unit,
    onHistoryClick: (WatchHistoryItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Hero banner
        if (uiState.heroItems.isNotEmpty()) {
            item(key = "hero") {
                HeroBanner(
                    items = uiState.heroItems,
                    onPlayClick = onPlayClick,
                    onItemClick = onItemClick,
                )
            }
        }

        item(key = "hero_spacer") { Spacer(Modifier.height(24.dp)) }

        // Continue Watching row (appears above catalog rows when non-empty)
        if (uiState.continueWatching.isNotEmpty()) {
            item(key = "continue_watching") {
                ContinueWatchingRow(
                    items = uiState.continueWatching,
                    onItemClick = onHistoryClick,
                )
                Spacer(Modifier.height(28.dp))
            }
        }

        // Content rows
        items(uiState.contentRows, key = { it.title }) { row ->
            ContentRow(
                title = row.title,
                items = row.items,
                onItemClick = onItemClick,
            )
            Spacer(Modifier.height(28.dp))
        }

        // Bottom padding
        item(key = "bottom_pad") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ShimmerHomeScreen() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val shimmerColor = Color.White.copy(alpha = alpha)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Hero placeholder
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(shimmerColor.copy(alpha * 0.15f)),
            )
        }
        item { Spacer(Modifier.height(32.dp)) }
        // Two shimmer content rows
        repeat(2) {
            item {
                Box(
                    modifier = Modifier
                        .padding(start = 32.dp, bottom = 12.dp)
                        .width(160.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor.copy(alpha * 0.2f)),
                )
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(6) {
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(240.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerColor.copy(alpha * 0.18f)),
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Something went wrong",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = androidx.tv.material3.ButtonDefaults.colors(
                containerColor = Color(0xFFE50914),
                contentColor = Color.White,
            ),
        ) {
            Text("Try Again")
        }
    }
}
