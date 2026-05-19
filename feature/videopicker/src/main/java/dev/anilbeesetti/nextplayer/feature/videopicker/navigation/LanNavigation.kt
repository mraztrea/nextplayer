package dev.anilbeesetti.nextplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.lan.LanBookmarkRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.lan.LanFolderBrowserRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.lan.LanServerManagerRoute
import kotlinx.serialization.Serializable

@Serializable
data object LanServerManagerNavRoute

@Serializable
data object LanBookmarksNavRoute

@Serializable
data class LanFolderBrowserNavRoute(
    val serverId: String,
    val folderPath: String = "",
)

fun NavController.navigateToLanServerManager(navOptions: NavOptions? = null) {
    this.navigate(LanServerManagerNavRoute, navOptions)
}

fun NavController.navigateToLanBookmarks(navOptions: NavOptions? = null) {
    this.navigate(LanBookmarksNavRoute, navOptions)
}

fun NavController.navigateToLanFolderBrowser(
    serverId: String,
    folderPath: String = "",
    navOptions: NavOptions? = null,
) {
    this.navigate(LanFolderBrowserNavRoute(serverId, folderPath), navOptions)
}

fun NavGraphBuilder.lanServerManagerScreen(
    onNavigateUp: () -> Unit,
    onOpenServer: (serverId: String) -> Unit,
    onBookmarksClick: () -> Unit,
) {
    composable<LanServerManagerNavRoute> {
        LanServerManagerRoute(
            onNavigateUp = onNavigateUp,
            onOpenServer = onOpenServer,
            onBookmarksClick = onBookmarksClick,
        )
    }
}

fun NavGraphBuilder.lanBookmarkScreen(
    onNavigateUp: () -> Unit,
    onOpenBookmark: (serverId: String, folderPath: String) -> Unit,
) {
    composable<LanBookmarksNavRoute> {
        LanBookmarkRoute(
            onNavigateUp = onNavigateUp,
            onOpenBookmark = onOpenBookmark,
        )
    }
}

fun NavGraphBuilder.lanFolderBrowserScreen(
    onNavigateUp: () -> Unit,
    onFolderClick: (serverId: String, folderPath: String) -> Unit,
    onPlayVideo: (uri: Uri, siblingUris: List<Uri>) -> Unit,
) {
    composable<LanFolderBrowserNavRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LanFolderBrowserNavRoute>()
        LanFolderBrowserRoute(
            serverId = route.serverId,
            folderPath = route.folderPath,
            onNavigateUp = onNavigateUp,
            onFolderClick = onFolderClick,
            onPlayVideo = onPlayVideo,
        )
    }
}
