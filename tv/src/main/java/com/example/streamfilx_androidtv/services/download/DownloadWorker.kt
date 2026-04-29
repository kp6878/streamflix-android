package com.example.streamfilx_androidtv.services.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.streamfilx_androidtv.data.db.DownloadDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return@withContext Result.failure()
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return@withContext Result.failure()

        val entity = downloadDao.getById(downloadId) ?: return@withContext Result.failure()
        val startByte = entity.downloadedBytes

        downloadDao.updateStatus(downloadId, STATUS_DOWNLOADING)

        try {
            val request = Request.Builder()
                .url(streamUrl)
                .apply { if (startByte > 0) header("Range", "bytes=$startByte-") }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    downloadDao.updateStatus(downloadId, STATUS_FAILED, "HTTP ${response.code}")
                    return@withContext Result.failure()
                }

                val responseLength = response.body?.contentLength() ?: 0L
                val totalBytes = startByte + responseLength
                val file = File(filePath).also { it.parentFile?.mkdirs() }

                FileOutputStream(file, startByte > 0).use { output ->
                    response.body?.byteStream()?.use { input ->
                        val buffer = ByteArray(8_192)
                        var bytesRead: Int
                        var downloaded = startByte
                        var lastUpdateBytes = downloaded

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isStopped) {
                                downloadDao.updateStatus(downloadId, STATUS_PAUSED)
                                return@withContext Result.success()
                            }
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            // Update DB every 512 KB
                            if (downloaded - lastUpdateBytes > 512 * 1024) {
                                val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                                downloadDao.updateProgress(downloadId, downloaded, totalBytes, progress)
                                lastUpdateBytes = downloaded
                            }
                        }

                        downloadDao.updateProgress(downloadId, downloaded, downloaded, 1.0f)
                        downloadDao.markCompleted(downloadId)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (isStopped) {
                downloadDao.updateStatus(downloadId, STATUS_PAUSED)
                Result.success()
            } else {
                downloadDao.updateStatus(downloadId, STATUS_FAILED, e.message)
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "downloadId"
        const val KEY_STREAM_URL = "streamUrl"
        const val KEY_FILE_PATH = "filePath"

        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
