package com.example.streamfilx_androidtv.features.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamSelectionSheet(
    type: String,
    id: String,
    onStreamResolved: (url: String, infoHash: String?, fileIdx: Int?, quality: String?) -> Unit,
    onDismiss: () -> Unit,
    viewModel: StreamSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(id) { viewModel.loadStreams(type, id) }

    // Dark scrim over whole screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        // Slide-in panel from right
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(480.dp)
                    .background(Color(0xFF111111))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},  // absorb clicks so scrim doesn't close
                    ),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Select Stream",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2A2A2A))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "✕", color = Color(0xFF888888), fontSize = 13.sp)
                        }
                    }

                    // Error message
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = Color(0xFFE50914),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }

                    // Content
                    when {
                        uiState.isLoadingStreams -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFE50914))
                                Spacer(Modifier.height(12.dp))
                                Text("Fetching streams…", color = Color(0xFF888888), fontSize = 14.sp)
                            }
                        }

                        uiState.streams.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No streams found",
                                color = Color(0xFF666666),
                                fontSize = 16.sp,
                            )
                        }

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp, vertical = 8.dp
                            ),
                        ) {
                            items(uiState.streams, key = { it.stream.infoHash ?: it.stream.url ?: it.stream.displayName }) { es ->
                                val isResolving = uiState.resolvingStreamId ==
                                        (es.stream.infoHash ?: es.stream.url)

                                StreamRow(
                                    es = es,
                                    isResolving = isResolving,
                                    onClick = {
                                        viewModel.resolveStream(es) { url ->
                                            onStreamResolved(
                                                url,
                                                es.stream.infoHash,
                                                es.stream.fileIdx,
                                                es.quality.display,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StreamRow(
    es: EnrichedStream,
    isResolving: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> Color(0xFF1F1F1F)
                    es.isCached -> Color(0xFF0A1A0A)
                    else -> Color(0xFF161616)
                }
            )
            .border(
                1.dp,
                when {
                    isFocused -> Color(0xFFE50914)
                    es.isCached -> Color(0xFF1A4A1A)
                    else -> Color(0xFF222222)
                },
                RoundedCornerShape(8.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(12.dp),
    ) {
        if (isResolving) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color(0xFFE50914),
                )
                Spacer(Modifier.width(10.dp))
                Text("Resolving…", color = Color(0xFF888888), fontSize = 14.sp)
            }
        } else {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Quality badge
                    QualityBadge(es.quality)

                    // Audio codec badge
                    es.audioCodec?.let { AudioBadge(it) }

                    // Cached badge
                    if (es.isCached) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0D3B1A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("⚡ Cached", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Language flags
                    es.languages.take(2).forEach { lang ->
                        Text(lang.flag, fontSize = 14.sp)
                    }

                    Spacer(Modifier.weight(1f))

                    // File size
                    es.fileSize?.let {
                        Text(it, color = Color(0xFF888888), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Stream name
                Text(
                    text = es.stream.displayName,
                    color = Color.White.copy(0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QualityBadge(quality: com.example.streamfilx_androidtv.core.models.StreamQuality) {
    Box(
        modifier = Modifier
            .background(Color(quality.badgeColor), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(quality.display, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AudioBadge(codec: com.example.streamfilx_androidtv.core.models.AudioCodec) {
    val bg = when (codec) {
        com.example.streamfilx_androidtv.core.models.AudioCodec.TRUEHD_ATMOS,
        com.example.streamfilx_androidtv.core.models.AudioCodec.TRUEHD,
        com.example.streamfilx_androidtv.core.models.AudioCodec.ATMOS -> Color(0xFF4A1A7A)
        com.example.streamfilx_androidtv.core.models.AudioCodec.DTS_HD_MA,
        com.example.streamfilx_androidtv.core.models.AudioCodec.DTS -> Color(0xFF1A3A6A)
        com.example.streamfilx_androidtv.core.models.AudioCodec.DD_PLUS,
        com.example.streamfilx_androidtv.core.models.AudioCodec.DD_5_1 -> Color(0xFF1A4A2A)
        else -> Color(0xFF2A2A2A)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(codec.display, color = Color.White.copy(0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
