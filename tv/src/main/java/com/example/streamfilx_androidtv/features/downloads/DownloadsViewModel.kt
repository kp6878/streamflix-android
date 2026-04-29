package com.example.streamfilx_androidtv.features.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.data.entities.DownloadEntity
import com.example.streamfilx_androidtv.services.download.DownloadManager
import com.example.streamfilx_androidtv.services.download.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val inProgress: List<DownloadEntity> = emptyList(),
    val completed: List<DownloadEntity> = emptyList(),
    val totalStorageBytes: Long = 0L,
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        downloadManager.downloads
            .onEach { all ->
                val inProgress = all.filter { it.status != DownloadWorker.STATUS_COMPLETED }
                val completed = all.filter { it.status == DownloadWorker.STATUS_COMPLETED }
                _uiState.update {
                    it.copy(
                        inProgress = inProgress,
                        completed = completed,
                        totalStorageBytes = downloadManager.totalStorageUsedBytes(),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun pause(downloadId: String) = downloadManager.pauseDownload(downloadId)

    fun resume(downloadId: String) = viewModelScope.launch {
        downloadManager.resumeDownload(downloadId)
    }

    fun cancel(downloadId: String) = viewModelScope.launch {
        downloadManager.cancelDownload(downloadId)
    }

    fun delete(downloadId: String) = viewModelScope.launch {
        downloadManager.deleteDownload(downloadId)
    }

    fun localUri(filePath: String) = downloadManager.localUri(filePath)
}
