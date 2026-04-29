package com.example.streamfilx_androidtv.services.download

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.streamfilx_androidtv.data.db.DownloadDao
import com.example.streamfilx_androidtv.data.entities.DownloadEntity
import com.example.streamfilx_androidtv.services.download.DownloadWorker.Companion.KEY_DOWNLOAD_ID
import com.example.streamfilx_androidtv.services.download.DownloadWorker.Companion.KEY_FILE_PATH
import com.example.streamfilx_androidtv.services.download.DownloadWorker.Companion.KEY_STREAM_URL
import com.example.streamfilx_androidtv.services.download.DownloadWorker.Companion.STATUS_PAUSED
import com.example.streamfilx_androidtv.services.download.DownloadWorker.Companion.STATUS_QUEUED
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val workManager: WorkManager,
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context,
) {
    val downloads: Flow<List<DownloadEntity>> = downloadDao.observeAll()

    // ── Start ─────────────────────────────────────────────────────────────────

    suspend fun startDownload(
        contentId: String,
        contentType: String,
        contentName: String,
        episodeTitle: String? = null,
        season: Int? = null,
        episode: Int? = null,
        poster: String? = null,
        streamUrl: String,
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val filename = buildFilename(contentName, season, episode)
        val dir = File(context.filesDir, "downloads/$downloadId").also { it.mkdirs() }
        val filePath = File(dir, filename).absolutePath

        val entity = DownloadEntity(
            id = downloadId,
            contentId = contentId,
            contentType = contentType,
            contentName = contentName,
            episodeTitle = episodeTitle,
            season = season,
            episode = episode,
            poster = poster,
            streamUrl = streamUrl,
            filePath = filePath,
            totalBytes = 0L,
            downloadedBytes = 0L,
            progress = 0f,
            status = STATUS_QUEUED,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            errorMessage = null,
        )
        downloadDao.insert(entity)
        enqueueWork(downloadId, streamUrl, filePath)
        return downloadId
    }

    // ── Control ───────────────────────────────────────────────────────────────

    fun pauseDownload(downloadId: String) {
        workManager.cancelAllWorkByTag(downloadId)
        // Worker sets status to PAUSED on isStopped
    }

    suspend fun resumeDownload(downloadId: String) {
        val entity = downloadDao.getById(downloadId) ?: return
        if (entity.status != STATUS_PAUSED) return
        enqueueWork(downloadId, entity.streamUrl, entity.filePath)
    }

    suspend fun cancelDownload(downloadId: String) {
        workManager.cancelAllWorkByTag(downloadId)
        val entity = downloadDao.getById(downloadId)
        entity?.filePath?.let { File(it).parentFile?.deleteRecursively() }
        downloadDao.delete(downloadId)
    }

    suspend fun deleteDownload(downloadId: String) {
        val entity = downloadDao.getById(downloadId)
        entity?.filePath?.let { File(it).parentFile?.deleteRecursively() }
        downloadDao.delete(downloadId)
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun localUri(filePath: String): Uri = Uri.fromFile(File(filePath))

    fun totalStorageUsedBytes(): Long {
        val dir = File(context.filesDir, "downloads")
        return dir.walkTopDown().sumOf { it.length() }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun enqueueWork(downloadId: String, streamUrl: String, filePath: String) {
        val data = workDataOf(
            KEY_DOWNLOAD_ID to downloadId,
            KEY_STREAM_URL to streamUrl,
            KEY_FILE_PATH to filePath,
        )
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag(downloadId)
            .build()
        workManager.enqueue(request)
    }

    private fun buildFilename(contentName: String, season: Int?, episode: Int?): String {
        val safe = contentName.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
        return if (season != null && episode != null) {
            "${safe}_S%02dE%02d.mp4".format(season, episode)
        } else {
            "$safe.mp4"
        }
    }
}
