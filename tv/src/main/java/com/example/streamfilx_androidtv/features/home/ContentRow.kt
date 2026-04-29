package com.example.streamfilx_androidtv.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.core.models.MetaPreview

val ROW_HORIZONTAL_PADDING = 32.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentRow(
    title: String,
    items: List<MetaPreview>,
    onItemClick: (MetaPreview) -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
) {
    if (items.isEmpty()) return

    val listState = rememberTvLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }

    if (requestInitialFocus) {
        LaunchedEffect(Unit) {
            firstItemFocusRequester.requestFocus()
        }
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = ROW_HORIZONTAL_PADDING, bottom = 12.dp),
        )

        TvLazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = ROW_HORIZONTAL_PADDING),
        ) {
            items(items, key = { it.id }) { meta ->
                val isFirst = items.indexOf(meta) == 0
                ContentCard(
                    meta = meta,
                    onClick = { onItemClick(meta) },
                    modifier = if (isFirst && requestInitialFocus)
                        Modifier.focusRequester(firstItemFocusRequester)
                    else
                        Modifier,
                )
            }
        }
    }
}
