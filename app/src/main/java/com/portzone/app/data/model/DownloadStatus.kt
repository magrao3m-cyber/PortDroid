package com.portzone.app.data.model

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    data class Enqueued(val downloadId: Long) : DownloadStatus
    data class Downloading(val progressPercent: Int) : DownloadStatus
    data class Completed(val destinationPath: String) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}
