package com.example.streamfilx_androidtv.features.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
private val ACCENT_COLOR = Color(0xFFE50914)
private val BUTTON_SHAPE = RoundedCornerShape(6.dp)

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
        // Use Crossfade for smooth hero item transitions
        Crossfade(
            targetState = pagerState.currentPage,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "heroCrossfade",
        ) { page ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pagerPage ->
                if (pagerPage == page) {
                    HeroBannerPage(
                        meta = items[pagerPage],
                        onPlayClick = { onPlayClick(items[pagerPage]) },
                        onDetailClick = { onItemClick(items[pagerPage]) },
                    )
                }
            }
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
                            if (isSelected) ACCENT_COLOR else Color.White.copy(alpha = 0.4f),
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
                    Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                }
                meta.imdbRating?.let {
                    if (meta.year != null) {
                        Text(
                            text = " · ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                        )
                    }
                    Text(
                        text = "★ $it",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                meta.genres?.take(2)?.let { genres ->
                    if (genres.isNotEmpty()) {
                        Text(
                            text = " · ${genres.joinToString(", ")}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
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
                HeroButton(
                    label = "▶  Play",
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    focusedContainerColor = ACCENT_COLOR,
                    focusedContentColor = Color.White,
                    onClick = onPlayClick,
                )

                Spacer(Modifier.width(12.dp))

                HeroButton(
                    label = "More Info",
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.25f),
                    focusedContentColor = Color.White,
                    onClick = onDetailClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    focusedContainerColor: Color,
    focusedContentColor: Color,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "heroBtnScale",
    )

    val elevation by animateFloatAsState(
        targetValue = if (isFocused) 8f else 2f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "heroBtnElevation",
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation.dp, BUTTON_SHAPE)
            .then(
                if (isFocused)
                    Modifier.border(2.dp, ACCENT_COLOR, BUTTON_SHAPE)
                else
                    Modifier
            )
            .onFocusChanged { isFocused = it.isFocused },
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.colors(
                containerColor = containerColor,
                contentColor = contentColor,
                focusedContainerColor = focusedContainerColor,
                focusedContentColor = focusedContentColor,
            ),
            shape = ButtonDefaults.shape(BUTTON_SHAPE),
        ) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
