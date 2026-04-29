package com.example.streamfilx_androidtv.features.browse

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import com.example.streamfilx_androidtv.features.common.ContentGrid
import com.example.streamfilx_androidtv.features.common.ShimmerContentGrid
import com.example.streamfilx_androidtv.navigation.Screen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowseScreen(
    navController: NavController,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Filter bar ────────────────────────────────────────────────────────
        FilterBar(
            selectedType = uiState.selectedType,
            selectedGenre = uiState.selectedGenre,
            onTypeSelected = viewModel::selectType,
            onGenreSelected = viewModel::selectGenre,
        )

        // ── Content ───────────────────────────────────────────────────────────
        when {
            uiState.isLoading -> ShimmerContentGrid()

            uiState.error != null -> BrowseErrorState(
                message = uiState.error!!,
                onRetry = viewModel::retry,
            )

            else -> ContentGrid(
                items = uiState.items,
                onItemClick = { meta ->
                    navController.navigate(Screen.Detail.route(meta.type, meta.id))
                },
                isLoadingMore = uiState.isLoadingMore,
                hasMore = uiState.hasMore,
                onLoadMore = viewModel::loadMore,
                emptyContent = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No content found",
                            color = Color(0xFF666666),
                            fontSize = 18.sp,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterBar(
    selectedType: String,
    selectedGenre: String?,
    onTypeSelected: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Type toggle
            listOf("movie" to "Movies", "series" to "Series").forEach { (type, label) ->
                TypeChip(
                    label = label,
                    isSelected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                )
            }

            Spacer(Modifier.width(16.dp))

            // Genre chips — horizontally scrollable
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // "All" chip
                GenreChip(
                    label = "All",
                    isSelected = selectedGenre == null,
                    onClick = { onGenreSelected(null) },
                )
                GENRES.forEach { genre ->
                    GenreChip(
                        label = genre,
                        isSelected = selectedGenre == genre,
                        onClick = { onGenreSelected(genre) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TypeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFFE50914)
            isFocused -> Color(0xFF2A2A2A)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "typeBg",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected || isFocused) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GenreChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFFE50914)
            isFocused -> Color.White.copy(alpha = 0.5f)
            else -> Color(0xFF333333)
        },
        animationSpec = tween(150),
        label = "genreBorder",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFE50914).copy(alpha = 0.15f) else Color.Transparent)
            .then(
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFFE50914) else if (isFocused) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BrowseErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Couldn't load content", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(text = message, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.colors(containerColor = Color(0xFFE50914), contentColor = Color.White),
            ) { Text("Try Again") }
        }
    }
}
