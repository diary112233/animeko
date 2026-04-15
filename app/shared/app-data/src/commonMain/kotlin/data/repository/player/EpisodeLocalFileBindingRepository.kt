/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.repository.player

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import me.him188.ani.app.data.repository.Repository
import me.him188.ani.datasources.api.SubtitleKind

@Serializable
data class EpisodeLocalFileBinding(
    val subjectId: Int,
    val episodeId: Int,
    val filePath: String,
    val displayName: String,
    val subtitleLanguageIds: List<String> = emptyList(),
    val resolution: String = "",
    val alliance: String = "",
    val subtitleKind: SubtitleKind? = null,
)

@Serializable
data class EpisodeLocalFileBindings(
    val bindings: List<EpisodeLocalFileBinding> = emptyList(),
) {
    companion object {
        val Empty = EpisodeLocalFileBindings(emptyList())
    }
}

interface EpisodeLocalFileBindingRepository {
    val flow: Flow<List<EpisodeLocalFileBinding>>

    fun bindingFlow(subjectId: Int, episodeId: Int): Flow<EpisodeLocalFileBinding?>

    suspend fun save(binding: EpisodeLocalFileBinding)

    suspend fun remove(subjectId: Int, episodeId: Int): Boolean

    suspend fun get(subjectId: Int, episodeId: Int): EpisodeLocalFileBinding?
}

class EpisodeLocalFileBindingRepositoryImpl(
    private val dataStore: DataStore<EpisodeLocalFileBindings>,
) : Repository(), EpisodeLocalFileBindingRepository {
    override val flow: Flow<List<EpisodeLocalFileBinding>> = dataStore.data.map { it.bindings }

    override fun bindingFlow(subjectId: Int, episodeId: Int): Flow<EpisodeLocalFileBinding?> {
        return flow.map { bindings ->
            bindings.firstOrNull { it.subjectId == subjectId && it.episodeId == episodeId }
        }
    }

    override suspend fun save(binding: EpisodeLocalFileBinding) {
        dataStore.updateData { current ->
            current.copy(
                bindings = current.bindings
                    .filterNot { it.subjectId == binding.subjectId && it.episodeId == binding.episodeId }
                    .plus(binding),
            )
        }
    }

    override suspend fun remove(subjectId: Int, episodeId: Int): Boolean {
        var removed = false
        dataStore.updateData { current ->
            val filtered = current.bindings.filterNot {
                val shouldRemove = it.subjectId == subjectId && it.episodeId == episodeId
                removed = removed || shouldRemove
                shouldRemove
            }
            current.copy(bindings = filtered)
        }
        return removed
    }

    override suspend fun get(subjectId: Int, episodeId: Int): EpisodeLocalFileBinding? {
        return bindingFlow(subjectId, episodeId).firstOrNull()
    }
}
