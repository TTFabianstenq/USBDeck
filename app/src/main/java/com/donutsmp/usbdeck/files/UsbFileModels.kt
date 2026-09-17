package com.donutsmp.usbdeck.files

import android.net.Uri

enum class SortMode { Name, Size, Date }

data class UsbEntry(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
)

data class BrowserState(
    val rootUri: String? = null,
    val currentUri: String? = null,
    val crumbs: List<Pair<String, String>> = emptyList(),
    val entries: List<UsbEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val sortMode: SortMode = SortMode.Name,
    val selected: Set<String> = emptySet(),
    val canWrite: Boolean = false
)

sealed class FileOpResult {
    data class Success(val message: String) : FileOpResult()
    data class Failed(val message: String) : FileOpResult()
    data object Cancelled : FileOpResult()
    data object Disconnected : FileOpResult()
}
