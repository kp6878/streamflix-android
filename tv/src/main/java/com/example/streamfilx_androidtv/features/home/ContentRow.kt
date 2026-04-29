package com.example.streamfilx_androidtv.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.core.models.MetaPreview

val ROW_HORIZONTAL_PADDING = 48.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentRow(
    title: String,
    items: List<MetaPreview>,
    onItemClick: (MetaPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = ROW_HORIZONTAL_PADDING, bottom = 12.dp),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = ROW_HORIZONTAL_PADDING),
        ) {
            items(items, key = { it.id }) { meta ->
                ContentCard(
                    meta = meta,
                    onClick = { onItemClick(meta) },
                )
            }
        }
    }
}
