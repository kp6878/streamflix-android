package com.example.streamfilx_androidtv.features.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.example.streamfilx_androidtv.core.models.EpisodeInfo
import com.example.streamfilx_androidtv.core.models.MetaItem
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.core.models.NowPlayingItem
import com.example.streamfilx_androidtv.core.models.Video
import com.example.streamfilx_androidtv.features.home.ContentCard
import com.example.streamfilx_androidtv.features.home.ROW_HORIZONTAL_PADDING
import com.example.streamfilx_androidtv.navigation.Screen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    navController: NavController,
    onStartPlayback: (NowPlayingItem) -> Unit = {},
    viewModel: ContentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedEpisode by remember { mutableStateOf<Video?>(null) }
    // null = no sheet, true = play mode, false = download mode
    var sheetInPlayMode by remember { mutableStateOf<Boolean?>(null) }

    val streamId = remember(selectedEpisode, viewModel.id) {
        val ep = selectedEpisode
        if (ep != null) ep.id else viewModel.id
    }

    // Close sheet on back press instead of popping the screen
    BackHandler(enabled = sheetInPlayMode != null) { sheetInPlayMode = null }

    if (uiState.downloadStarted) { viewModel.clearDownloadStarted() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }

            uiState.error != null -> DetailErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadContent,
            )

            uiState.meta != null -> DetailContent(
                meta = uiState.meta!!,
                uiState = uiState,
                onPlayClick = { episode ->
                    selectedEpisode = episode
                    sheetInPlayMode = true
                },
                onDownloadClick = { episode ->
                    selectedEpisode = episode
                    sheetInPlayMode = false
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onSeasonSelect = viewModel::selectSeason,
                onItemClick = { preview ->
                    navController.navigate(Screen.Detail.route(preview.type, preview.id))
                },
            )
        }

        // Stream selection overlay — shared for play and download
        if (sheetInPlayMode != null) {
            StreamSelectionSheet(
                type = viewModel.type,
                id = streamId,
                onStreamResolved = { url, infoHash, fileIdx, quality ->
                    val playMode = sheetInPlayMode == true
                    sheetInPlayMode = null
                    if (playMode) {
                        onStartPlayback(buildNowPlayingItem(
                            meta = uiState.meta,
                            viewModel = viewModel,
                            episode = selectedEpisode,
                            url = url,
                            infoHash = infoHash,
                            fileIdx = fileIdx,
                            quality = quality,
                        ))
                    } else {
                        viewModel.startDownload(url, quality, selectedEpisode)
                    }
                },
                onDismiss = { sheetInPlayMode = null },
            )
        }
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailContent(
    meta: MetaItem,
    uiState: ContentDetailUiState,
    onPlayClick: (Video?) -> Unit,
    onDownloadClick: (Video?) -> Unit,
    onToggleFavorite: () -> Unit,
    onSeasonSelect: (Int) -> Unit,
    onItemClick: (MetaPreview) -> Unit,
) {
    TvLazyColumn(modifier = Modifier.fillMaxSize()) {
        // Hero
        item(key = "hero") {
            HeroSection(
                meta = meta,
                isFavorite = uiState.isFavorite,
                onPlayClick = { onPlayClick(null) },
                onDownloadClick = { onDownloadClick(null) },
                onToggleFavorite = onToggleFavorite,
            )
        }

        // Cast
        if (!meta.cast.isNullOrEmpty()) {
            item(key = "cast") {
                CastRow(cast = meta.cast!!)
            }
        }

        // Episodes (series)
        if (meta.isSeries && meta.availableSeasons.isNotEmpty()) {
            item(key = "seasons") {
                SeasonPicker(
                    seasons = meta.availableSeasons,
                    selectedSeason = uiState.selectedSeason,
                    onSeasonSelect = onSeasonSelect,
                )
            }
            item(key = "episodes") {
                EpisodeList(
                    episodes = uiState.episodesForSelectedSeason,
                    onEpisodeClick = { onPlayClick(it) },
                )
            }
        }

        // More Like This
        if (uiState.moreLikeThis.isNotEmpty()) {
            item(key = "more_title") {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "More Like This",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = ROW_HORIZONTAL_PADDING, bottom = 12.dp),
                )
            }
            item(key = "more_row") {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = ROW_HORIZONTAL_PADDING),
                ) {
                    items(uiState.moreLikeThis, key = { it.id }) { preview ->
                        ContentCard(meta = preview, onClick = { onItemClick(preview) })
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ── Hero section ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroSection(
    meta: MetaItem,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { playFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp),
    ) {
        // Background image
        AsyncImage(
            model = meta.background ?: meta.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlays
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(0.9f),
                    0.55f to Color.Black.copy(0.3f),
                )
            )
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.6f to Color.Transparent,
                    1f to Color.Black,
                )
            )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 40.dp)
                .fillMaxWidth(0.52f),
        ) {
            Text(
                text = meta.name,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                meta.imdbRating?.let {
                    Text("★ $it", color = Color(0xFFFFD700), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                meta.year?.let { Text(it, color = Color.White.copy(0.7f), fontSize = 15.sp) }
                meta.runtime?.let { Text(it, color = Color.White.copy(0.7f), fontSize = 15.sp) }
                meta.genres?.take(2)?.joinToString(", ")?.let {
                    Text(it, color = Color.White.copy(0.7f), fontSize = 15.sp)
                }
            }

            // Description
            meta.description?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color.White.copy(0.85f),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.focusRequester(playFocusRequester),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        focusedContainerColor = Color(0xFFE50914),
                        focusedContentColor = Color.White,
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                ) {
                    Text("▶  Play", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                Button(
                    onClick = onDownloadClick,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(0.12f),
                        contentColor = Color.White,
                        focusedContainerColor = Color(0xFF1A3A5A),
                        focusedContentColor = Color.White,
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                ) {
                    Text("⬇  Download", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                val favBg by animateColorAsState(
                    if (isFavorite) Color(0xFF1A3A1A) else Color.White.copy(0.12f),
                    tween(200), label = "fav",
                )
                val favContent by animateColorAsState(
                    if (isFavorite) Color(0xFF4CAF50) else Color.White,
                    tween(200), label = "favText",
                )
                Button(
                    onClick = onToggleFavorite,
                    colors = ButtonDefaults.colors(
                        containerColor = favBg,
                        contentColor = favContent,
                        focusedContainerColor = if (isFavorite) Color(0xFF2A4A2A) else Color.White.copy(0.2f),
                        focusedContentColor = favContent,
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                ) {
                    Text(
                        text = if (isFavorite) "✓  In My List" else "+  My List",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

// ── Cast row ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CastRow(cast: List<String>) {
    Column(modifier = Modifier.padding(start = ROW_HORIZONTAL_PADDING, top = 24.dp)) {
        Text("Cast", color = Color(0xFFB3B3B3), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        TvLazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cast.take(10)) { name ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1F1F1F), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(name, color = Color.White.copy(0.9f), fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Season picker ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeasonPicker(
    seasons: List<Int>,
    selectedSeason: Int?,
    onSeasonSelect: (Int) -> Unit,
) {
    TvLazyRow(
        modifier = Modifier.padding(start = ROW_HORIZONTAL_PADDING, top = 28.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(seasons) { season ->
            val isSelected = season == selectedSeason
            var isFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFFE50914) else Color(0xFF1F1F1F))
                    .border(1.dp, if (isFocused) Color.White.copy(0.5f) else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeasonSelect(season) },
                    )
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (season == 0) "Specials" else "Season $season",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

// ── Episode list ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeList(episodes: List<Video>, onEpisodeClick: (Video) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = ROW_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        episodes.forEach { ep ->
            EpisodeRow(episode = ep, onClick = { onEpisodeClick(ep) })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeRow(episode: Video, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(8.dp)),
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = Color(0xFF1A1A1A),
            focusedContainerColor = Color(0xFF2A2A2A),
        ),
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A1A1A)),
        ) {
            if (episode.thumbnail != null) {
                AsyncImage(
                    model = episode.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = episode.episodeNumber?.toString() ?: "?",
                        color = Color(0xFF555555),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val epNum = episode.episodeNumber?.let { "$it. " } ?: ""
                Text(
                    text = "$epNum${episode.displayTitle}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            episode.overview?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    color = Color.White.copy(0.6f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    } // Card
}

// ── NowPlayingItem builder ────────────────────────────────────────────────────

private fun buildNowPlayingItem(
    meta: MetaItem?,
    viewModel: ContentDetailViewModel,
    episode: Video?,
    url: String,
    infoHash: String?,
    fileIdx: Int?,
    quality: String?,
): NowPlayingItem {
    val sortedVideos = meta?.videos
        ?.sortedWith(compareBy({ if ((it.season ?: 0) == 0) Int.MAX_VALUE else it.season ?: 0 }, { it.episodeNumber ?: 0 }))
        ?: emptyList()

    val nextVideo = if (episode != null) {
        val idx = sortedVideos.indexOfFirst { it.season == episode.season && it.episodeNumber == episode.episodeNumber }
        if (idx >= 0 && idx + 1 < sortedVideos.size) sortedVideos[idx + 1] else null
    } else null

    val allEpisodes = sortedVideos.map { v ->
        EpisodeInfo(
            id = v.id,
            season = v.season ?: 1,
            episode = v.episodeNumber ?: 1,
            title = v.title,
            thumbnail = v.thumbnail,
            overview = v.overview,
        )
    }.ifEmpty { null }

    return NowPlayingItem(
        id = viewModel.id,
        type = viewModel.type,
        title = meta?.name ?: viewModel.id,
        streamUrl = url,
        posterUrl = meta?.poster,
        background = meta?.background,
        year = meta?.year,
        genres = meta?.genres,
        imdbRating = meta?.imdbRating,
        runtime = meta?.runtime,
        contentDescription = meta?.description,
        cast = meta?.cast,
        season = episode?.season,
        episode = episode?.episodeNumber,
        episodeName = episode?.title,
        episodeOverview = episode?.overview,
        streamQuality = quality,
        streamInfoHash = infoHash,
        streamFileIdx = fileIdx,
        nextEpisodeId = nextVideo?.id,
        nextEpisodeSeason = nextVideo?.season,
        nextEpisodeNumber = nextVideo?.episodeNumber,
        nextEpisodeName = nextVideo?.title,
        allEpisodes = allEpisodes,
    )
}

// ── Error state ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Couldn't load content", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.White.copy(0.6f), fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.colors(containerColor = Color(0xFFE50914), contentColor = Color.White),
            ) { Text("Retry") }
        }
    }
}
