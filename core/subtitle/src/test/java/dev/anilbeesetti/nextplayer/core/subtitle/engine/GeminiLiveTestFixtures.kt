package dev.anilbeesetti.nextplayer.core.subtitle.engine

import java.util.Base64

internal object GeminiLiveTestFixtures {
    const val apiKey = "test-google-api-key"
    const val targetLanguageCode = "vi"
    const val englishLanguageCode = "en"

    val pcm100Ms16KhzMono: ByteArray = ByteArray(3_200) { index ->
        (index % 64).toByte()
    }
    val pcm100Ms16KhzMonoBase64: String = Base64.getEncoder().encodeToString(pcm100Ms16KhzMono)

    const val inputTranscriptMessage = """
        {
          "serverContent": {
            "inputTranscription": {
              "text": "hello",
              "languageCode": "en",
              "finished": true
            }
          }
        }
    """

    const val outputTranscriptMessage = """
        {
          "serverContent": {
            "outputTranscription": {
              "text": "xin chao",
              "languageCode": "vi",
              "finished": true
            }
          }
        }
    """
}
