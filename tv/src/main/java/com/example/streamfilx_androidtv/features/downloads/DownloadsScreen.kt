package com.example.streamfilx_androidtv.features.downloads

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.streamfilx_androidtv.core.models.NowPlayingItem
import com.example.streamfilx_androidtv.data.entities.DownloadEntity
import com.example.streamfilx_androidtv.services.download.DownloadWorker

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onPlayDownload: (NowPlayingItem) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414)),
    ) {
        if (uiState.inProgress.isEmpty() && uiState.completed.isEmpty()) {
            EmptyDownloads()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Storage indicator
                item(key = "storage") {
                    val mb = uiState.totalStorageBytes / (1024 * 1024)
                    if (mb > 0) {
                        Text(
                            text = "Downloads · $mb MB used",
                            color = Color.White.copy(0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 20.dp),
                        )
                    }
                }

                // In Progress section
                if (uiState.inProgress.isNotEmpty()) {
                    item(key = "progress_header") {
                        SectionHeader("In Progress")
                    }
                    items(uiState.inProgress, key = { it.id }) { entity ->
                        InProgressCard(
                            entity = entity,
                            onPause = { viewModel.pause(entity.id) },
                            onResume = { viewModel.resume(entity.id) },
                            onCancel = { viewModel.cancel(entity.id) },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    item(key = "progress_spacer") { Spacer(Modifier.height(12.dp)) }
                }

                // Completed section
                if (uiState.completed.isNotEmpty()) {
                    item(key = "completed_header") {
                        SectionHeader("Completed")
                    }
                    items(uiState.completed, key = { it.id }) { entity ->
                        CompletedCard(
                            entity = entity,
                            onPlay = {
                                val uri = viewModel.localUri(entity.filePath)
                                onPlayDownload(entity.toNowPlayingItem(uri))
                            },
                            onDelete = { viewModel.delete(entity.id) },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// In-progress card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InProgressCard(
    entity: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Thumbnail
        AsyncImage(
            model = entity.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 80.dp, height = 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A)),
        )

        // Info + progress
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entity.contentName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entity.episodeInfo?.let {
                Text(it, color = Color.White.copy(0.6f), fontSize = 12.sp)
            }
            val pct = (entity.progress * 100).toInt()
            val statusLabel = when (entity.status) {
                DownloadWorker.STATUS_PAUSED -> "Paused · $pct%"
                DownloadWorker.STATUS_FAILED -> "Failed"
                else -> "$pct%"
            }
            Text(statusLabel, color = Color.White.copy(0.5f), fontSize = 11.sp)
            LinearProgressIndicator(
                progress = { entity.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFE50914),
                trackColor = Color.White.copy(0.2f),
            )
        }

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entity.status == DownloadWorker.STATUS_PAUSED || entity.status == DownloadWorker.STATUS_FAILED) {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.colors(containerColor = Color(0xFF1A3A5A)),
                    shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                ) { Text("Resume", fontSize = 12.sp, color = Color.White) }
            } else if (entity.status == DownloadWorker.STATUS_DOWNLOADING) {
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.colors(containerColor = Color.White.copy(0.12f)),
                    shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                ) { Text("Pause", fontSize = 12.sp, color = Color.White) }
            }
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.colors(containerColor = Color(0xFF3A1A1A)),
                shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
            ) { Text("Cancel", fontSize = 12.sp, color = Color(0xFFE50914)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Completed card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompletedCard(
    entity: DownloadEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = entity.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 80.dp, height = 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A)),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entity.contentName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entity.episodeInfo?.let {
                Text(it, color = Color.White.copy(0.6f), fontSize = 12.sp)
            }
            if (entity.sizeLabel.isNotEmpty()) {
                Text(entity.sizeLabel, color = Color.White.copy(0.4f), fontSize = 11.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    focusedContainerColor = Color(0xFFE50914),
                    focusedContentColor = Color.White,
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
            ) { Text("▶  Play", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.colors(containerColor = Color(0xFF3A1A1A)),
                shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
            ) { Text("Delete", fontSize = 13.sp, color = Color(0xFFE50914)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyDownloads() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⬇", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("No Downloads", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Downloaded content will appear here",
                color = Color.White.copy(0.5f),
                fontSize = 14.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper
// ─────────────────────────────────────────────────────────────────────────────

private fun DownloadEntity.toNowPlayingItem(uri: Uri) = NowPlayingItem(
    id = contentId,
    type = contentType,
    title = contentName,
    streamUrl = uri.toString(),
    posterUrl = poster,
    season = season,
    episode = episode,
    episodeName = episodeTitle,
)
