package dev.anilbeesetti.nextplayer.feature.player.utils

import android.content.Intent
import android.net.Uri
import androidx.media3.common.C
import dev.anilbeesetti.nextplayer.core.model.PlaybackLaunchContext
import dev.anilbeesetti.nextplayer.core.model.PlaybackSourceType
import dev.anilbeesetti.nextplayer.core.model.SiblingVideoEntry
import dev.anilbeesetti.nextplayer.feature.player.PlayerActivity
import dev.anilbeesetti.nextplayer.feature.player.extensions.getParcelableUriArray
import dev.anilbeesetti.nextplayer.feature.player.model.Subtitle

class PlayerApi(val activity: PlayerActivity) {

    private val extras get() = activity.intent.extras
    val isApiAccess: Boolean get() = extras != null
    val hasPosition: Boolean get() = extras?.containsKey(API_POSITION) == true
    val hasTitle: Boolean get() = extras?.containsKey(API_TITLE) == true
    val shouldReturnResult: Boolean get() = extras?.containsKey(API_RETURN_RESULT) == true
    val position: Int? get() = if (hasPosition) extras?.getInt(API_POSITION) else null
    val title: String? get() = if (hasTitle) extras?.getString(API_TITLE) else null

    fun getSubs(): List<Subtitle> {
        val extras = extras ?: return emptyList()
        if (!extras.containsKey(API_SUBS)) return emptyList()

        val subs = extras.getParcelableUriArray(API_SUBS) ?: return emptyList()
        val subsName = extras.getStringArray(API_SUBS_NAME)

        val subsEnable = extras.getParcelableUriArray(API_SUBS_ENABLE)
        val defaultSub = if (!subsEnable.isNullOrEmpty()) subsEnable[0] as Uri else null

        return subs.mapIndexed { index, parcelable ->
            val subtitleUri = parcelable as Uri
            val subtitleName = subsName?.let { if (it.size > index) it[index] else null }
            Subtitle(
                name = subtitleName,
                uri = subtitleUri,
                isSelected = subtitleUri == defaultSub,
            )
        }
    }

    fun getPlaylist(): List<String> {
        val extras = extras ?: return emptyList()
        if (!extras.containsKey(API_PLAYLIST)) return emptyList()
        val playlist = extras.getParcelableUriArray(API_PLAYLIST) ?: return emptyList()
        return playlist.map { (it as Uri).toString() }
    }

    fun getPlaybackLaunchContext(currentUriString: String): PlaybackLaunchContext? {
        val playlist = getPlaylist().normalizedUriStrings(currentUriString)
        if (playlist.isNotEmpty()) {
            return PlaybackLaunchContext(
                currentUriString = currentUriString,
                siblings = playlist.map(::SiblingVideoEntry),
                sourceType = PlaybackSourceType.API_PLAYLIST,
            )
        }

        val extras = extras ?: return null

        val sourceType = extras.getString(API_PLAYBACK_SOURCE_TYPE)
            ?.let { source -> runCatching { PlaybackSourceType.valueOf(source) }.getOrNull() }
            ?: return null
        val siblingUris = extras.getStringArrayList(API_PLAYBACK_CONTEXT_URIS)
            .orEmpty()
            .normalizedUriStrings(currentUriString)
        if (siblingUris.isEmpty()) return null

        return PlaybackLaunchContext(
            currentUriString = currentUriString,
            siblings = siblingUris.map(::SiblingVideoEntry),
            sourceType = sourceType,
        )
    }

    fun getResult(isPlaybackFinished: Boolean, duration: Long, position: Long): Intent {
        return Intent(API_RESULT_INTENT).apply {
            if (isPlaybackFinished) {
                putExtra(API_END_BY, API_END_BY_COMPLETION)
            } else {
                putExtra(API_END_BY, API_END_BY_USER)
                if (duration != C.TIME_UNSET) putExtra(API_DURATION, duration.toInt())
                if (position != C.TIME_UNSET) putExtra(API_POSITION, position.toInt())
            }
        }
    }

    companion object {
        const val API_TITLE = "title"
        const val API_POSITION = "position"
        const val API_DURATION = "duration"
        const val API_RETURN_RESULT = "return_result"
        const val API_END_BY = "end_by"
        const val API_SUBS = "subs"
        const val API_SUBS_ENABLE = "subs.enable"
        const val API_SUBS_NAME = "subs.name"
        const val API_PLAYLIST = "video_list"
        const val API_PLAYBACK_CONTEXT_URIS = "playback_context_uris"
        const val API_PLAYBACK_SOURCE_TYPE = "playback_source_type"

        const val API_RESULT_INTENT = "com.mxtech.intent.result.VIEW"

        private const val API_END_BY_USER = "user"
        private const val API_END_BY_COMPLETION = "playback_completion"
    }
}

private fun List<String>.normalizedUriStrings(currentUriString: String): List<String> {
    return buildList {
        add(currentUriString)
        addAll(this@normalizedUriStrings)
    }.map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
}
