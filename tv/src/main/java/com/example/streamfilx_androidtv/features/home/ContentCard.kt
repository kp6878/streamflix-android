package com.example.streamfilx_androidtv.features.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.streamfilx_androidtv.core.models.MetaPreview

private val CARD_WIDTH = 160.dp
private val CARD_HEIGHT = 240.dp
private val CARD_SHAPE = RoundedCornerShape(12.dp)
private val ACCENT_COLOR = Color(0xFFE50914)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentCard(
    meta: MetaPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "cardScale",
    )

    val elevation by animateFloatAsState(
        targetValue = if (isFocused) 8f else 2f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "cardElevation",
    )

    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Box(
            modifier = Modifier
                .width(CARD_WIDTH)
                .height(CARD_HEIGHT)
                .shadow(elevation.dp, CARD_SHAPE)
                .then(
                    if (isFocused)
                        Modifier.border(2.dp, ACCENT_COLOR, CARD_SHAPE)
                    else
                        Modifier
                ),
        ) {
            Card(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { isFocused = it.isFocused },
                shape = CardDefaults.shape(CARD_SHAPE),
                colors = CardDefaults.colors(containerColor = Color(0xFF1A1A1A)),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Poster image
                    AsyncImage(
                        model = meta.poster,
                        contentDescription = meta.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Placeholder when no image
                    if (meta.poster == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2A2A2A)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = meta.name.take(2).uppercase(),
                                color = Color(0xFF555555),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // IMDB rating badge — top right
                    meta.imdbRating?.let { rating ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.75f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "★ $rating",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Focus highlight overlay
                    if (isFocused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.08f))
                                .clip(CARD_SHAPE),
                        )
                    }
                }
            }
        }

        // Title below card (always visible for TV legibility)
        Spacer(Modifier.height(6.dp))
        Text(
            text = meta.name,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(CARD_WIDTH),
        )
        meta.year?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
            )
        }
    }
}
