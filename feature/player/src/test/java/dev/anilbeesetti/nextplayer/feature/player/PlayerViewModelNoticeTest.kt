package dev.anilbeesetti.nextplayer.feature.player

import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadPipelineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerViewModelNoticeTest {

    @Test
    fun `warmup notice is emitted only once for the same generation`() {
        val state = LookaheadSessionState(
            generationId = 3L,
            status = LookaheadPipelineStatus.WARMING,
        )

        val first = resolveLookaheadNotice(
            liveSubtitleActive = true,
            state = state,
            lastNoticeKey = null,
        )
        assertEquals("Preparing lookahead subtitle...", first.notice)
        assertEquals("warmup:3", first.noticeKey)

        val second = resolveLookaheadNotice(
            liveSubtitleActive = true,
            state = state,
            lastNoticeKey = first.noticeKey,
        )
        assertNull(second.notice)
        assertEquals("warmup:3", second.noticeKey)
    }

    @Test
    fun `new generation emits warmup notice again`() {
        val state = LookaheadSessionState(
            generationId = 4L,
            status = LookaheadPipelineStatus.WARMING,
        )

        val result = resolveLookaheadNotice(
            liveSubtitleActive = true,
            state = state,
            lastNoticeKey = "warmup:3",
        )

        assertEquals("Preparing lookahead subtitle...", result.notice)
        assertEquals("warmup:4", result.noticeKey)
    }
}
