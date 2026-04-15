/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.media.fetch

import kotlinx.coroutines.flow.asFlow
import kotlinx.io.files.Path
import me.him188.ani.app.data.repository.player.EpisodeLocalFileBinding
import me.him188.ani.app.data.repository.player.EpisodeLocalFileBindingRepository
import me.him188.ani.datasources.api.DefaultMedia
import me.him188.ani.datasources.api.MediaProperties
import me.him188.ani.datasources.api.paging.SinglePagePagedSource
import me.him188.ani.datasources.api.paging.SizedSource
import me.him188.ani.datasources.api.source.ConnectionStatus
import me.him188.ani.datasources.api.source.MatchKind
import me.him188.ani.datasources.api.source.MediaFetchRequest
import me.him188.ani.datasources.api.source.MediaMatch
import me.him188.ani.datasources.api.source.MediaSource
import me.him188.ani.datasources.api.source.MediaSourceInfo
import me.him188.ani.datasources.api.source.MediaSourceKind
import me.him188.ani.datasources.api.source.MediaSourceLocation
import me.him188.ani.datasources.api.topic.EpisodeRange
import me.him188.ani.datasources.api.topic.FileSize.Companion.bytes
import me.him188.ani.datasources.api.topic.ResourceLocation
import me.him188.ani.utils.io.SystemPath
import me.him188.ani.utils.io.exists
import me.him188.ani.utils.io.extension
import me.him188.ani.utils.io.inSystem
import me.him188.ani.utils.io.isRegularFile
import me.him188.ani.utils.io.length
import me.him188.ani.utils.io.name

class LocalEpisodeFileBindingMediaSource(
    private val repository: EpisodeLocalFileBindingRepository,
) : MediaSource {
    override val mediaSourceId: String = ID
    override val location: MediaSourceLocation = MediaSourceLocation.Local
    override val kind: MediaSourceKind = MediaSourceKind.LocalCache
    override val info: MediaSourceInfo = MediaSourceInfo(
        displayName = "本地绑定",
        description = "手动绑定到剧集的本地视频文件",
        isSpecial = true,
    )

    override suspend fun checkConnection(): ConnectionStatus = ConnectionStatus.SUCCESS

    override suspend fun fetch(query: MediaFetchRequest): SizedSource<MediaMatch> {
        return SinglePagePagedSource {
            val subjectId = query.subjectId.toIntOrNull()
            val episodeId = query.episodeId.toIntOrNull()
            if (subjectId == null || episodeId == null) {
                return@SinglePagePagedSource emptyList<MediaMatch>().asFlow()
            }

            val binding = repository.get(subjectId, episodeId)
                ?: return@SinglePagePagedSource emptyList<MediaMatch>().asFlow()
            val file = Path(binding.filePath).inSystem
            if (!file.exists() || !file.isRegularFile()) {
                return@SinglePagePagedSource emptyList<MediaMatch>().asFlow()
            }

            listOf(
                MediaMatch(
                    createMedia(binding, query, file),
                    MatchKind.EXACT,
                ),
            ).asFlow()
        }
    }

    private fun createMedia(
        binding: EpisodeLocalFileBinding,
        query: MediaFetchRequest,
        file: SystemPath,
    ): DefaultMedia {
        val download = ResourceLocation.LocalFile(
            filePath = binding.filePath,
            fileType = inferFileType(file),
        )
        return DefaultMedia(
            mediaId = "$ID:${query.subjectId}:${query.episodeId}:${binding.filePath}",
            mediaSourceId = ID,
            originalUrl = download.uri,
            download = download,
            originalTitle = binding.displayName.ifBlank { file.name },
            publishedTime = 0L,
            properties = MediaProperties(
                subjectName = query.subjectNameCN ?: query.subjectNames.firstOrNull(),
                episodeName = query.episodeName,
                subtitleLanguageIds = binding.subtitleLanguageIds,
                resolution = binding.resolution,
                alliance = binding.alliance.ifBlank { DEFAULT_ALLIANCE },
                size = file.length().bytes,
                subtitleKind = binding.subtitleKind,
            ),
            episodeRange = EpisodeRange.single(query.episodeEp ?: query.episodeSort),
            location = MediaSourceLocation.Local,
            kind = MediaSourceKind.LocalCache,
        )
    }

    private fun inferFileType(file: SystemPath): ResourceLocation.LocalFile.FileType {
        return when (file.extension.lowercase()) {
            "ts", "m2ts", "mts" -> ResourceLocation.LocalFile.FileType.MPTS
            else -> ResourceLocation.LocalFile.FileType.CONTAINED
        }
    }

    companion object {
        const val ID: String = "local-episode-binding"
        const val DEFAULT_ALLIANCE: String = "本地文件"
    }
}
