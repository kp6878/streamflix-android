package com.example.streamfilx_androidtv.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.features.common.ContentGrid
import com.example.streamfilx_androidtv.navigation.Screen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Filter tabs ───────────────────────────────────────────────────────
        FilterTabs(
            selected = uiState.filter,
            onFilterSelected = viewModel::setFilter,
        )

        // ── Content ───────────────────────────────────────────────────────────
        if (uiState.items.isEmpty()) {
            EmptyLibraryState(filter = uiState.filter)
        } else {
            ContentGrid(
                items = uiState.items,
                onItemClick = { meta ->
                    navController.navigate(Screen.Detail.route(meta.type, meta.id))
                },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterTabs(
    selected: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 48.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            LibraryFilter.ALL to "All",
            LibraryFilter.MOVIES to "Movies",
            LibraryFilter.SERIES to "Series",
        ).forEach { (filter, label) ->
            FilterTab(
                label = label,
                isSelected = selected == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isSelected -> Color(0xFFE50914)
                    isFocused -> Color(0xFF2A2A2A)
                    else -> Color.Transparent
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected || isFocused) Color.White else Color.White.copy(0.6f),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyLibraryState(filter: LibraryFilter) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "♡",
                color = Color(0xFF333333),
                fontSize = 64.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    LibraryFilter.ALL -> "Your list is empty"
                    LibraryFilter.MOVIES -> "No movies in your list"
                    LibraryFilter.SERIES -> "No series in your list"
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Add titles by pressing + My List on any content page",
                color = Color(0xFF888888),
                fontSize = 15.sp,
            )
        }
    }
}
