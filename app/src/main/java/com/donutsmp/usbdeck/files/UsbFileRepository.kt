package com.donutsmp.usbdeck.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.donutsmp.usbdeck.usb.UsbConnectionState
import com.donutsmp.usbdeck.usb.UsbMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

class UsbFileRepository(
    private val context: Context,
    private val usbMonitor: UsbMonitor
) {
    private val resolver get() = context.contentResolver

    suspend fun list(folderUri: Uri, query: String, sortMode: SortMode): Result<List<UsbEntry>> =
        withContext(Dispatchers.IO) {
            if (disconnected()) return@withContext Result.failure(IOException("USB disconnected"))
            val folder = open(folderUri) ?: return@withContext Result.failure(IOException("Unable to open this folder."))
            if (!folder.exists()) return@withContext Result.failure(IOException("Folder no longer exists. The USB may have been removed."))
            val q = query.trim()
            val dirsFirst = compareByDescending<UsbEntry> { it.isDirectory }
            val cmp = when (sortMode) {
                SortMode.Name -> dirsFirst.thenBy { it.name.lowercase() }
                SortMode.Size -> dirsFirst.thenByDescending { it.size }
                SortMode.Date -> dirsFirst.thenByDescending { it.lastModified }
            }
            val items = folder.listFiles().map { doc ->
                UsbEntry(doc.uri, doc.name ?: "untitled", doc.isDirectory, if (doc.isDirectory) 0L else doc.length(), doc.lastModified(), doc.type)
            }.filter { q.isBlank() || it.name.contains(q, true) }.sortedWith(cmp)
            Result.success(items)
        }

    suspend fun createFolder(parent: Uri, name: String): FileOpResult = withContext(Dispatchers.IO) {
        guarded {
            val folder = requireFolder(parent)
            if (folder.findFile(name) != null) return@guarded FileOpResult.Failed("A file or folder named \"$name\" already exists.")
            val created = folder.createDirectory(name) ?: return@guarded FileOpResult.Failed("Could not create the folder. The volume may be read-only.")
            FileOpResult.Success("Created ${created.name}")
        }
    }

    suspend fun rename(uri: Uri, newName: String): FileOpResult = withContext(Dispatchers.IO) {
        guarded {
            if (!requireDoc(uri).renameTo(newName)) FileOpResult.Failed("Rename failed.")
            else FileOpResult.Success("Renamed to $newName")
        }
    }

    suspend fun delete(uris: List<Uri>): FileOpResult = withContext(Dispatchers.IO) {
        guarded {
            var deleted = 0
            val failures = mutableListOf<String>()
            uris.forEach { uri ->
                coroutineContext.ensureActive(); checkConnected()
                val doc = open(uri)
                if (doc == null || !doc.delete()) failures += uri.lastPathSegment ?: uri.toString() else deleted++
            }
            when {
                failures.isEmpty() -> FileOpResult.Success("Deleted $deleted item(s)")
                deleted == 0 -> FileOpResult.Failed("Could not delete: ${failures.joinToString()}")
                else -> FileOpResult.Failed("Deleted $deleted, failed: ${failures.joinToString()}")
            }
        }
    }

    suspend fun copy(sources: List<Uri>, destinationFolder: Uri, overwrite: Boolean): FileOpResult =
        withContext(Dispatchers.IO) {
            guarded {
                val dest = requireFolder(destinationFolder)
                sources.forEach { src ->
                    coroutineContext.ensureActive(); checkConnected()
                    copyRecursive(requireDoc(src), dest, overwrite)
                }
                FileOpResult.Success("Copied ${sources.size} item(s)")
            }
        }

    fun openWrite(parent: DocumentFile, fileName: String, mime: String, overwrite: Boolean): DocumentFile {
        val existing = parent.findFile(fileName)
        if (existing != null) {
            if (!overwrite) return parent.createFile(mime, uniqueName(parent, fileName)) ?: throw IOException("Could not create $fileName")
            existing.delete()
        }
        return parent.createFile(mime, fileName) ?: throw IOException("Could not create $fileName. Volume may be read-only.")
    }

    fun uniqueName(parent: DocumentFile, fileName: String): String {
        if (parent.findFile(fileName) == null) return fileName
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var i = 1
        while (true) {
            val candidate = "$base ($i)$ext"
            if (parent.findFile(candidate) == null) return candidate
            i++
        }
    }

    private fun copyRecursive(src: DocumentFile, destFolder: DocumentFile, overwrite: Boolean) {
        checkConnected()
        val name = src.name ?: "untitled"
        if (src.isDirectory) {
            val existing = destFolder.findFile(name)
            val target = when {
                existing != null && existing.isDirectory -> existing
                existing != null && overwrite -> { existing.delete(); destFolder.createDirectory(name) }
                existing != null -> destFolder.createDirectory(uniqueName(destFolder, name))
                else -> destFolder.createDirectory(name)
            } ?: throw IOException("Could not create folder $name")
            src.listFiles().forEach { copyRecursive(it, target, overwrite) }
            return
        }
        val target = openWrite(destFolder, name, src.type ?: "application/octet-stream", overwrite)
        resolver.openInputStream(src.uri).use { input ->
            resolver.openOutputStream(target.uri, "w").use { output ->
                if (input == null || output == null) { target.delete(); throw IOException("Could not open streams for $name") }
                input.copyTo(output); output.flush()
            }
        }
    }

    private suspend fun guarded(block: suspend () -> FileOpResult): FileOpResult = try {
        checkConnected(); block()
    } catch (_: DisconnectException) { FileOpResult.Disconnected }
    catch (_: kotlinx.coroutines.CancellationException) { FileOpResult.Cancelled }
    catch (t: Throwable) { FileOpResult.Failed(userMessage(t)) }

    private fun disconnected() = usbMonitor.snapshot.value.connection == UsbConnectionState.Disconnected
    private fun checkConnected() { if (disconnected()) throw DisconnectException() }
    private fun open(uri: Uri) = DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
    private fun requireFolder(uri: Uri): DocumentFile {
        val doc = open(uri) ?: throw IOException("Folder is not accessible.")
        if (!doc.exists() || !doc.isDirectory) throw IOException("Folder is not accessible.")
        if (!doc.canWrite()) throw IOException("This location is read-only.")
        return doc
    }
    private fun requireDoc(uri: Uri) = open(uri) ?: throw IOException("File is not accessible.")
    private fun userMessage(t: Throwable): String {
        val raw = t.message ?: t.javaClass.simpleName
        return when {
            raw.contains("not found", true) -> "File not found. The USB may have been removed."
            raw.contains("space", true) -> "Not enough free space on the USB drive."
            raw.contains("read-only", true) -> "The USB drive is read-only."
            raw.contains("permission", true) -> "Access denied. Grant folder permission again."
            else -> raw
        }
    }
    private class DisconnectException : IOException("USB disconnected")
}
