package dev.anilbeesetti.nextplayer.core.subtitle.session

import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionResetScheduler @Inject constructor(
    @param:SubtitleScope private val scope: CoroutineScope,
) {

    companion object {
        const val SESSION_RESET_INTERVAL_MS = 3 * 60 * 1000L
    }

    private var resetJob: Job? = null

    fun start(onReset: () -> Unit) {
        stop()
        resetJob = scope.launch {
            while (currentCoroutineContext().isActive) {
                delay(SESSION_RESET_INTERVAL_MS)
                onReset()
            }
        }
    }

    fun stop() {
        resetJob?.cancel()
        resetJob = null
    }
}