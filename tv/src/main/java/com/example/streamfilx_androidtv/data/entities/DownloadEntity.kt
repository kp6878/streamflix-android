package com.example.streamfilx_androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val contentType: String,
    val contentName: String,
    val episodeTitle: String?,
    val season: Int?,
    val episode: Int?,
    val poster: String?,
    val streamUrl: String,
    val filePath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val progress: Float,            // 0.0–1.0
    val status: String,             // QUEUED | DOWNLOADING | PAUSED | COMPLETED | FAILED
    val createdAt: Long,
    val completedAt: Long?,
    val errorMessage: String?,
) {
    val episodeInfo: String?
        get() {
            val s = season ?: return null
            val e = episode ?: return null
            return "S%02dE%02d".format(s, e)
        }

    val sizeLabel: String
        get() {
            val mb = totalBytes / (1024 * 1024)
            return if (mb > 0) "$mb MB" else ""
        }
}
