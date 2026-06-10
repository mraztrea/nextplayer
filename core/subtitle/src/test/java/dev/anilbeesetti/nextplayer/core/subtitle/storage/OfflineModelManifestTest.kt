package dev.anilbeesetti.nextplayer.core.subtitle.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineModelManifestTest {
    @Test
    fun validV1ManifestIsSupported() {
        val manifest = OfflineModelManifest.parse(validManifest())

        assertTrue(manifest.isV1Supported())
    }

    @Test
    fun manifestMissingEnglishTranslationIsRejected() {
        val manifest = OfflineModelManifest.parse(
            validManifest().replace(", \"ja_to_en\"", ""),
        )

        assertFalse(manifest.isV1Supported())
    }

    private fun validManifest(): String {
        return """
            {
              "manifestVersion": 1,
              "modelId": "ja-vi-en-prototype",
              "displayName": "Japanese Offline Prototype",
              "version": "1",
              "sourceLanguage": "ja",
              "targetLanguages": ["vi", "en"],
              "capabilities": ["japanese_transcript", "ja_to_vi", "ja_to_en"],
              "files": [
                {
                  "path": "model.onnx",
                  "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "sizeBytes": 1
                }
              ]
            }
        """.trimIndent()
    }
}
