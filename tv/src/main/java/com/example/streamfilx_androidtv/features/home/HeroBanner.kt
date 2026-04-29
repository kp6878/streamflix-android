package com.example.streamfilx_androidtv.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.streamfilx_androidtv.core.models.MetaPreview
import kotlinx.coroutines.delay

private val BANNER_HEIGHT = 420.dp
private const val AUTO_ADVANCE_MS = 7_000L

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(
    items: List<MetaPreview>,
    onPlayClick: (MetaPreview) -> Unit,
    onItemClick: (MetaPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-advance every 7 seconds
    LaunchedEffect(pagerState.currentPage) {
        delay(AUTO_ADVANCE_MS)
        val next = (pagerState.currentPage + 1) % items.size
        pagerState.animateScrollToPage(next)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BANNER_HEIGHT),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            HeroBannerPage(
                meta = items[page],
                onPlayClick = { onPlayClick(items[page]) },
                onDetailClick = { onItemClick(items[page]) },
            )
        }

        // Page indicator dots — bottom center
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            repeat(items.size) { i ->
                val isSelected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (isSelected) 20.dp else 6.dp)
                        .height(6.dp)
                        .background(
                            if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.4f),
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroBannerPage(
    meta: MetaPreview,
    onPlayClick: () -> Unit,
    onDetailClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        AsyncImage(
            model = meta.background ?: meta.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay — left and bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.85f),
                        0.6f to Color.Black.copy(alpha = 0.2f),
                    )
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black,
                    )
                ),
        )

        // Content overlay — left side
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 48.dp)
                .fillMaxWidth(0.5f),
        ) {
            // Title
            Text(
                text = meta.name,
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            // Metadata row — year · rating · genres
            Row(verticalAlignment = Alignment.CenterVertically) {
                meta.year?.let {
                    Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                }
                meta.imdbRating?.let {
                    if (meta.year != null) {
                        Text(
                            text = " · ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 15.sp,
                        )
                    }
                    Text(
                        text = "★ $it",
                        color = Color(0xFFFFD700),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                meta.genres?.take(2)?.let { genres ->
                    if (genres.isNotEmpty()) {
                        Text(
                            text = " · ${genres.joinToString(", ")}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            // Description
            meta.description?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons
            Row {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        focusedContainerColor = Color(0xFFE50914),
                        focusedContentColor = Color.White,
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                ) {
                    Text("▶  Play", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = onDetailClick,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.25f),
                        focusedContentColor = Color.White,
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                ) {
                    Text("More Info", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }
        }
    }
}
