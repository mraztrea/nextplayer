package dev.anilbeesetti.nextplayer.feature.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.anilbeesetti.nextplayer.core.common.extensions.getMediaContentUri
import dev.anilbeesetti.nextplayer.core.model.PlaybackQueueSnapshot
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.extensions.registerForSuspendActivityResult
import dev.anilbeesetti.nextplayer.feature.player.extensions.setExtras
import dev.anilbeesetti.nextplayer.feature.player.extensions.uriToSubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.model.QueueHydrationStatus
import dev.anilbeesetti.nextplayer.feature.player.service.PlayerService
import dev.anilbeesetti.nextplayer.feature.player.service.addSubtitleTrack
import dev.anilbeesetti.nextplayer.feature.player.service.stopPlayerSession
import dev.anilbeesetti.nextplayer.feature.player.utils.PlayerApi
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.anilbeesetti.nextplayer.core.ui.R as coreUiR

val LocalUseMaterialYouControls = compositionLocalOf { false }

@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    val playerPreferences get() = viewModel.uiState.value.playerPreferences

    private val onWindowAttributesChangedListener = CopyOnWriteArrayList<Consumer<WindowManager.LayoutParams?>>()

    private var isPlaybackFinished = false
    private var playInBackground: Boolean = false
    private var isIntentNew: Boolean = true

    /**
     * Player
     */
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private lateinit var playerApi: PlayerApi
    private var lastAppliedQueueSignature: String? = null

    /**
     * Listeners
     */
    private val playbackStateListener: Player.Listener = playbackStateListener()

    private val subtitleFileSuspendLauncher = registerForSuspendActivityResult(OpenDocument())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var player by remember { mutableStateOf<MediaController?>(null) }

            LifecycleStartEffect(Unit) {
                maybeInitControllerFuture()
                lifecycleScope.launch {
                    player = controllerFuture?.await()
                }

                onStopOrDispose {
                    player = null
                }
            }

            CompositionLocalProvider(LocalUseMaterialYouControls provides (uiState.playerPreferences?.useMaterialYouControls == true)) {
                NextPlayerTheme(darkTheme = true) {
                    MediaPlayerScreen(
                        player = player,
                        viewModel = viewModel,
                        playerPreferences = uiState.playerPreferences ?: return@NextPlayerTheme,
                        onSelectSubtitleClick = {
                            lifecycleScope.launch {
                                val uri = subtitleFileSuspendLauncher.launch(
                                    arrayOf(
                                        MimeTypes.APPLICATION_SUBRIP,
                                        MimeTypes.APPLICATION_TTML,
                                        MimeTypes.TEXT_VTT,
                                        MimeTypes.TEXT_SSA,
                                        MimeTypes.BASE_TYPE_APPLICATION + "/octet-stream",
                                        MimeTypes.BASE_TYPE_TEXT + "/*",
                                    ),
                                ) ?: return@launch
                                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                maybeInitControllerFuture()
                                controllerFuture?.await()?.addSubtitleTrack(uri)
                            }
                        },
                        onBackClick = { finishAndStopPlayerSession() },
                        onPlayInBackgroundClick = {
                            playInBackground = true
                            finish()
                        },
                        onNextUnavailableClick = {
                            viewModel.showPlaybackNotice(getString(coreUiR.string.no_next_video_in_queue))
                        },
                        onPreviousUnavailableClick = {
                            viewModel.showPlaybackNotice(getString(coreUiR.string.no_previous_video_in_queue))
                        },
                    )
                }
            }
        }

        playerApi = PlayerApi(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.queueHydrationState.collect { state ->
                    if (state.status != QueueHydrationStatus.READY) return@collect
                    val snapshot = state.snapshot ?: return@collect
                    cacheHydratedPlaybackContext(snapshot)
                    applyHydratedQueue(snapshot)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            maybeInitControllerFuture()
            mediaController = controllerFuture?.await()

            mediaController?.run {
                updateKeepScreenOnFlag()
                addListener(playbackStateListener)
                startPlayback()
            }
        }
    }

    override fun onStop() {
        mediaController?.run {
            viewModel.playWhenReady = playWhenReady
            removeListener(playbackStateListener)
        }
        val shouldPlayInBackground = playInBackground || playerPreferences?.autoBackgroundPlay == true
        if (subtitleFileSuspendLauncher.isAwaitingResult || !shouldPlayInBackground) {
            mediaController?.pause()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            finish()
            if (!shouldPlayInBackground) {
                mediaController?.stopPlayerSession()
            }
        }

        controllerFuture?.run {
            MediaController.releaseFuture(this)
            controllerFuture = null
        }
        super.onStop()
    }

    private fun maybeInitControllerFuture() {
        if (controllerFuture == null) {
            val sessionToken = SessionToken(applicationContext, ComponentName(applicationContext, PlayerService::class.java))
            controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
        }
    }

    private fun startPlayback() {
        val uri = intent.data ?: return
        val playbackUri = getMediaContentUri(uri) ?: uri
        val launchContext = playerApi.getPlaybackLaunchContext(playbackUri.toString())

        val returningFromBackground = !isIntentNew && mediaController?.currentMediaItem != null
        val isNewUriTheCurrentMediaItem = mediaController?.currentMediaItem?.localConfiguration?.uri.toString() == uri.toString()

        if (returningFromBackground || (isNewUriTheCurrentMediaItem && launchContext == null)) {
            mediaController?.prepare()
            mediaController?.playWhenReady = viewModel.playWhenReady
            return
        }

        isIntentNew = false
        lastAppliedQueueSignature = null

        lifecycleScope.launch {
            playVideo(uri)
        }
    }

    private suspend fun playVideo(uri: Uri) = withContext(Dispatchers.Default) {
        val playbackUri = getMediaContentUri(uri) ?: uri
        val currentUriString = playbackUri.toString()
        val currentMediaItem = buildCurrentPlaybackMediaItem(
            uriString = currentUriString,
        )
        val launchContext = playerApi.getPlaybackLaunchContext(currentUriString)

        withContext(Dispatchers.Main) {
            mediaController?.run {
                setMediaItem(currentMediaItem, playerApi.position?.toLong() ?: C.TIME_UNSET)
                playWhenReady = viewModel.playWhenReady
                prepare()
            }
        }

        viewModel.hydratePlaybackQueue(playbackUri, launchContext)
    }

    private suspend fun buildCurrentPlaybackMediaItem(
        uriString: String,
    ): MediaItem {
        return MediaItem.Builder().apply {
            setUri(uriString)
            setMediaId(uriString)
            setMediaMetadata(
                MediaMetadata.Builder().apply {
                    setTitle(playerApi.title)
                    setExtras(positionMs = playerApi.position?.toLong())
                }.build(),
            )
            val apiSubs = playerApi.getSubs().map { subtitle ->
                uriToSubtitleConfiguration(
                    uri = subtitle.uri,
                    subtitleEncoding = playerPreferences?.subtitleTextEncoding ?: "",
                    isSelected = subtitle.isSelected,
                )
            }
            setSubtitleConfigurations(apiSubs)
        }.build()
    }

    private fun buildQueueMediaItem(uriString: String): MediaItem {
        return MediaItem.Builder()
            .setUri(uriString)
            .setMediaId(uriString)
            .build()
    }

    private fun cacheHydratedPlaybackContext(snapshot: PlaybackQueueSnapshot) {
        if (snapshot.uriStrings.size <= 1) return
        if (playerApi.getPlaybackLaunchContext(snapshot.currentUriString) != null) return

        intent.putStringArrayListExtra(
            PlayerApi.API_PLAYBACK_CONTEXT_URIS,
            ArrayList(snapshot.uriStrings),
        )
        intent.putExtra(PlayerApi.API_PLAYBACK_SOURCE_TYPE, snapshot.sourceType.name)
    }

    private fun applyHydratedQueue(snapshot: PlaybackQueueSnapshot) {
        val controller = mediaController ?: return
        if (snapshot.uriStrings.size <= 1) return
        if (controller.currentMediaItem?.mediaId != snapshot.currentUriString) return

        val queueSignature = buildString {
            append(snapshot.sourceType.name)
            append(':')
            append(snapshot.uriStrings.joinToString("|"))
        }
        if (queueSignature == lastAppliedQueueSignature) return

        val currentQueue = buildList {
            repeat(controller.mediaItemCount) { index ->
                add(controller.getMediaItemAt(index).mediaId)
            }
        }
        if (currentQueue == snapshot.uriStrings) {
            lastAppliedQueueSignature = queueSignature
            return
        }

        if (controller.mediaItemCount == 1 && controller.currentMediaItem?.mediaId == snapshot.currentUriString) {
            val previousItems = snapshot.entries
                .take(snapshot.currentIndex)
                .map { buildQueueMediaItem(it.uriString) }
            val nextItems = snapshot.entries
                .drop(snapshot.currentIndex + 1)
                .map { buildQueueMediaItem(it.uriString) }
            if (previousItems.isNotEmpty()) {
                controller.addMediaItems(0, previousItems)
            }
            if (nextItems.isNotEmpty()) {
                controller.addMediaItems(controller.mediaItemCount, nextItems)
            }
            lastAppliedQueueSignature = queueSignature
            return
        }

        val currentPosition = controller.currentPosition
        val playWhenReady = controller.playWhenReady
        controller.setMediaItems(
            snapshot.entries.mapIndexed { index, entry ->
                if (index == snapshot.currentIndex) {
                    controller.currentMediaItem ?: buildQueueMediaItem(entry.uriString)
                } else {
                    buildQueueMediaItem(entry.uriString)
                }
            },
            snapshot.currentIndex,
            currentPosition,
        )
        controller.playWhenReady = playWhenReady
        controller.prepare()
        lastAppliedQueueSignature = queueSignature
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            intent.data = mediaItem?.localConfiguration?.uri
            isPlaybackFinished = false
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            updateKeepScreenOnFlag()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if (mediaController?.hasNextMediaItem() == true) return
                    isPlaybackFinished = mediaController?.playbackState == Player.STATE_ENDED
                    finishAndStopPlayerSession()
                }

                else -> {}
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)

            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                if (mediaController?.repeatMode != Player.REPEAT_MODE_OFF) return
                if (mediaController?.hasNextMediaItem() == true) return
                isPlaybackFinished = true
                finishAndStopPlayerSession()
            }
        }
    }

    override fun finish() {
        if (playerApi.shouldReturnResult) {
            val result = playerApi.getResult(
                isPlaybackFinished = isPlaybackFinished,
                duration = mediaController?.duration ?: C.TIME_UNSET,
                position = mediaController?.currentPosition ?: C.TIME_UNSET,
            )
            setResult(RESULT_OK, result)
        }
        super.finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) {
            setIntent(intent)
            playerApi = PlayerApi(this)
            isIntentNew = true
            lastAppliedQueueSignature = null
            if (mediaController != null) {
                startPlayback()
            }
        }
    }

    private fun updateKeepScreenOnFlag() {
        if (mediaController?.isPlaying == true) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun finishAndStopPlayerSession() {
        finish()
        mediaController?.stopPlayerSession()
    }

    override fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        super.onWindowAttributesChanged(params)
        for (listener in onWindowAttributesChangedListener) {
            listener.accept(params)
        }
    }

    fun addOnWindowAttributesChangedListener(listener: Consumer<WindowManager.LayoutParams?>) {
        onWindowAttributesChangedListener.add(listener)
    }

    fun removeOnWindowAttributesChangedListener(listener: Consumer<WindowManager.LayoutParams?>) {
        onWindowAttributesChangedListener.remove(listener)
    }
}
