package dev.anilbeesetti.nextplayer.core.data.repository.fake

import dev.anilbeesetti.nextplayer.core.data.repository.LanFolderRepository
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanResult

class FakeLanFolderRepository : LanFolderRepository {
    var folder: LanFolder? = null
    var errorType: LanErrorType? = null

    override suspend fun listFolder(serverId: String, folderPath: String): LanResult<LanFolder> {
        errorType?.let { return LanResult.Failure(LanError(it)) }
        return folder?.let { LanResult.Success(it.copy(serverId = serverId, path = folderPath)) }
            ?: LanResult.Failure(LanError(LanErrorType.PathNotFound))
    }
}
