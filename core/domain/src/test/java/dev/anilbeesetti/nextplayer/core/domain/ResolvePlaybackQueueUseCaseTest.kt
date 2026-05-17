package dev.anilbeesetti.nextplayer.core.domain

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolvePlaybackQueueUseCaseTest {

    @Test
    fun discoverSiblingEntriesFromDirectory_keepsCurrentUriAndSortsVideoSiblings() {
        val tempDir = Files.createTempDirectory("resolve-playback-queue").toFile()
        try {
            val alphaFile = tempDir.resolve("Alpha.mp4").apply { writeText("alpha") }
            val betaFile = tempDir.resolve("Beta.mkv").apply { writeText("beta") }
            tempDir.resolve("notes.txt").writeText("ignore")

            val currentUriString = "content://com.cxinventor.file.explorer/open/Beta.mkv"

            val entries = discoverSiblingEntriesFromDirectory(
                currentUriString = currentUriString,
                currentPath = betaFile.absolutePath,
                listFiles = { directory -> directory.listFiles() },
                resolvePlaybackUri = { siblingFile -> "resolved://${siblingFile.name}" },
            )

            assertEquals(
                listOf("resolved://${alphaFile.name}", currentUriString),
                entries.map { entry -> entry.uriString },
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun discoverSiblingEntriesFromDirectory_returnsSingleEntryWhenNoSiblingVideoExists() {
        val tempDir = Files.createTempDirectory("resolve-playback-single").toFile()
        try {
            val currentFile = tempDir.resolve("Only.mp4").apply { writeText("only") }
            tempDir.resolve("notes.txt").writeText("ignore")

            val currentUriString = "content://com.cxinventor.file.explorer/open/Only.mp4"

            val entries = discoverSiblingEntriesFromDirectory(
                currentUriString = currentUriString,
                currentPath = currentFile.absolutePath,
                listFiles = { directory -> directory.listFiles() },
                resolvePlaybackUri = { siblingFile -> "resolved://${siblingFile.name}" },
            )

            assertEquals(listOf(currentUriString), entries.map { entry -> entry.uriString })
        } finally {
            tempDir.deleteRecursively()
        }
    }
}