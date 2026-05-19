package dev.anilbeesetti.nextplayer.core.domain.lan

import dev.anilbeesetti.nextplayer.core.data.repository.LanFolderRepository
import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import dev.anilbeesetti.nextplayer.core.model.LanResult
import javax.inject.Inject

class BrowseLanFolderUseCase @Inject constructor(
    private val lanFolderRepository: LanFolderRepository,
) {
    suspend operator fun invoke(serverId: String, folderPath: String = ""): LanResult<LanFolder> {
        return when (val result = lanFolderRepository.listFolder(serverId, folderPath)) {
            is LanResult.Success -> LanResult.Success(
                result.value.copy(
                    items = result.value.items
                        .filter { it.type == LanMediaItemType.Folder || it.type == LanMediaItemType.Video }
                        .sortedWith(compareBy { it.type != LanMediaItemType.Folder }),
                ),
            )
            is LanResult.Failure -> LanResult.Failure(result.error)
        }
    }
}
