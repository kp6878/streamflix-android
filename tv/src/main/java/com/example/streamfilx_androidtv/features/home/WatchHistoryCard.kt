package com.example.streamfilx_androidtv.features.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.streamfilx_androidtv.core.models.WatchHistoryItem

private val HISTORY_CARD_WIDTH = 200.dp
private val HISTORY_CARD_HEIGHT = 113.dp  // 16:9

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WatchHistoryCard(
    item: WatchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(150),
        label = "histCardScale",
    )

    Column(
        modifier = modifier
            .width(HISTORY_CARD_WIDTH)
            .scale(scale),
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(HISTORY_CARD_WIDTH)
                .height(HISTORY_CARD_HEIGHT)
                .onFocusChanged { isFocused = it.isFocused },
            shape = CardDefaults.shape(RoundedCornerShape(6.dp)),
            colors = CardDefaults.colors(containerColor = Color(0xFF1A1A1A)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Thumbnail or poster
                val imageUrl = item.thumbnail ?: item.poster
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.contentName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                if (imageUrl == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.contentName.take(2).uppercase(),
                            color = Color(0xFF555555),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Gradient + progress bar at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    // Dark gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(0.85f)),
                                )
                            ),
                    )

                    // Progress bar (4dp high, full-width track, red fill)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White.copy(0.25f))
                            .align(Alignment.BottomCenter),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.progress.toFloat().coerceIn(0f, 1f))
                                .height(4.dp)
                                .background(Color(0xFFE50914)),
                        )
                    }
                }

                // Focus highlight
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.08f))
                            .clip(RoundedCornerShape(6.dp)),
                    )
                }
            }
        }

        // Metadata below card
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.contentName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(HISTORY_CARD_WIDTH),
        )
        val subtitle = item.episodeInfo ?: item.timeRemaining.takeIf { it.isNotEmpty() }
        subtitle?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(HISTORY_CARD_WIDTH),
            )
        }
    }
}
