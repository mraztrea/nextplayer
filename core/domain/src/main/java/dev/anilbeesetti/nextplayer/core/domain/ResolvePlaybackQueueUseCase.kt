package dev.anilbeesetti.nextplayer.core.domain

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import dev.anilbeesetti.nextplayer.core.common.extensions.getMediaContentUri
import dev.anilbeesetti.nextplayer.core.common.extensions.getPath
import dev.anilbeesetti.nextplayer.core.common.Dispatcher
import dev.anilbeesetti.nextplayer.core.common.NextDispatchers
import dev.anilbeesetti.nextplayer.core.model.PlaybackLaunchContext
import dev.anilbeesetti.nextplayer.core.model.PlaybackQueueSnapshot
import dev.anilbeesetti.nextplayer.core.model.PlaybackSourceType
import dev.anilbeesetti.nextplayer.core.model.SiblingVideoEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ResolvePlaybackQueueUseCase @Inject constructor(
    private val getSortedPlaylistUseCase: GetSortedPlaylistUseCase,
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        currentUri: Uri,
        launchContext: PlaybackLaunchContext?,
    ): PlaybackQueueSnapshot = withContext(defaultDispatcher) {
        val currentUriString = currentUri.toString()
        launchContext?.toSnapshot(currentUriString)?.let { return@withContext it }

        val fallbackEntries = getSortedPlaylistUseCase(currentUri)
            .map { SiblingVideoEntry(it.uriString) }
            .normalizedEntries(currentUriString)
        if (fallbackEntries.isNotEmpty()) {
            return@withContext PlaybackQueueSnapshot(
                currentUriString = currentUriString,
                currentIndex = fallbackEntries.indexOfFirst { it.uriString == currentUriString }
                    .takeIf { it >= 0 } ?: 0,
                entries = fallbackEntries,
                sourceType = PlaybackSourceType.APP_SORT_FALLBACK,
            )
        }

        val documentEntries = discoverSiblingEntries(context, currentUri)
            .normalizedEntries(currentUriString)
        if (documentEntries.size > 1) {
            return@withContext PlaybackQueueSnapshot(
                currentUriString = currentUriString,
                currentIndex = documentEntries.indexOfFirst { it.uriString == currentUriString }
                    .takeIf { it >= 0 } ?: 0,
                entries = documentEntries,
                sourceType = PlaybackSourceType.SOURCE_VISIBLE_ORDER,
            )
        }

        PlaybackQueueSnapshot(
            currentUriString = currentUriString,
            currentIndex = 0,
            entries = listOf(SiblingVideoEntry(currentUriString)),
            sourceType = PlaybackSourceType.SINGLE_ITEM,
        )
    }
}

private fun discoverSiblingEntries(
    context: Context,
    currentUri: Uri,
): List<SiblingVideoEntry> {
    val fileEntries = discoverSiblingEntriesFromPath(context, currentUri)
    if (fileEntries.size > 1) return fileEntries

    if (!DocumentsContract.isDocumentUri(context, currentUri)) return fileEntries

    val authority = currentUri.authority ?: return emptyList()
    val currentDocumentId = runCatching { DocumentsContract.getDocumentId(currentUri) }
        .getOrNull() ?: return emptyList()
    val parentDocumentId = currentDocumentId.substringBeforeLast("/", missingDelimiterValue = "")
        .ifBlank { return emptyList() }

    val childrenUri = DocumentsContract.buildChildDocumentsUri(authority, parentDocumentId)
    val projection = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE,
    )

    return runCatching {
        context.contentResolver.query(childrenUri, projection, null, null, null)
            ?.use { cursor ->
                val documentIdIndex = cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)
                val displayNameIndex = cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)
                val mimeTypeIndex = cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)
                buildList {
                    while (cursor.moveToNext()) {
                        val documentId = cursor.getString(documentIdIndex) ?: continue
                        val displayName = cursor.getString(displayNameIndex).orEmpty()
                        val mimeType = cursor.getString(mimeTypeIndex).orEmpty()
                        if (!mimeType.startsWith("video/") && !displayName.isLikelyVideoFile()) continue

                        val uriString = if (documentId == currentDocumentId) {
                            currentUri.toString()
                        } else {
                            DocumentsContract.buildDocumentUri(authority, documentId).toString()
                        }
                        add(
                            displayName.lowercase() to SiblingVideoEntry(uriString),
                        )
                    }
                }.sortedBy { it.first }
                    .map { it.second }
            }
            .orEmpty()
    }.getOrDefault(emptyList())
}

private fun discoverSiblingEntriesFromPath(
    context: Context,
    currentUri: Uri,
): List<SiblingVideoEntry> {
    val currentPath = context.getPath(currentUri) ?: return emptyList()
    return discoverSiblingEntriesFromDirectory(
        currentUriString = currentUri.toString(),
        currentPath = currentPath,
        listFiles = { directory -> directory.listFiles() },
        resolvePlaybackUri = { siblingFile ->
            val fileUri = Uri.fromFile(siblingFile)
            (context.getMediaContentUri(fileUri) ?: fileUri).toString()
        },
    )
}

internal fun discoverSiblingEntriesFromDirectory(
    currentUriString: String,
    currentPath: String,
    listFiles: (File) -> Array<File>?,
    resolvePlaybackUri: (File) -> String,
): List<SiblingVideoEntry> {
    val currentFile = File(currentPath)
    val currentAbsolutePath = currentFile.absolutePath
    val parentFile = currentFile.parentFile?.takeIf { it.isDirectory } ?: return emptyList()

    return listFiles(parentFile)
        ?.asSequence()
        ?.filter { siblingFile -> siblingFile.isFile }
        ?.filter { siblingFile -> siblingFile.name.isLikelyVideoFile() }
        ?.map { siblingFile ->
            val uriString = if (siblingFile.absolutePath == currentAbsolutePath) {
                currentUriString
            } else {
                resolvePlaybackUri(siblingFile)
            }
            siblingFile.name.lowercase() to SiblingVideoEntry(uriString)
        }
        ?.sortedBy { (displayName, _) -> displayName }
        ?.map { (_, entry) -> entry }
        ?.distinctBy { entry -> entry.uriString }
        ?.toList()
        .orEmpty()
}

private fun PlaybackLaunchContext.toSnapshot(currentUriString: String): PlaybackQueueSnapshot? {
    val entries = siblings.normalizedEntries(currentUriString)
    if (entries.isEmpty()) return null
    return PlaybackQueueSnapshot(
        currentUriString = currentUriString,
        currentIndex = entries.indexOfFirst { it.uriString == currentUriString }
            .takeIf { it >= 0 } ?: 0,
        entries = entries,
        sourceType = sourceType,
    )
}

private fun List<SiblingVideoEntry>.normalizedEntries(currentUriString: String): List<SiblingVideoEntry> {
    return buildList {
        add(SiblingVideoEntry(currentUriString))
        addAll(this@normalizedEntries)
    }.distinctBy { it.uriString }
}

private fun String.isLikelyVideoFile(): Boolean {
    val extension = substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in setOf(
        "3gp",
        "asf",
        "avi",
        "flv",
        "m2ts",
        "m4v",
        "mkv",
        "mov",
        "mp4",
        "mpeg",
        "mpg",
        "mts",
        "ts",
        "webm",
        "wmv",
    )
}
