package com.donutsmp.usbdeck.download

enum class DownloadStatus {
    Queued,
    Running,
    Verifying,
    Completed,
    Failed,
    Cancelled,
    Incomplete
}

data class DownloadRecord(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sourceUrl: String,
    val sourceLabel: String,
    val category: String,
    val destinationLabel: String,
    val expectedSha256: String?,
    val expectedSize: Long?,
    val bytesRead: Long = 0L,
    val contentLength: Long = -1L,
    val bytesPerSecond: Long = 0L,
    val status: DownloadStatus = DownloadStatus.Queued,
    val actualSha256: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
) {
    val progress: Float
        get() {
            val total = if (contentLength > 0) contentLength else expectedSize ?: -1L
            if (total <= 0L) return 0f
            return (bytesRead.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
        }
}
