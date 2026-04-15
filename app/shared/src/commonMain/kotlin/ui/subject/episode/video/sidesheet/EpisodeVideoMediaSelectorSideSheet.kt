/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.video.sidesheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.ProvideCompositionLocalsForPreview
import me.him188.ani.app.ui.foundation.rememberAsyncHandler
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.app.ui.mediafetch.MediaSelectorState
import me.him188.ani.app.ui.mediafetch.MediaSelectorView
import me.him188.ani.app.ui.mediafetch.MediaSourceResultListPresentation
import me.him188.ani.app.ui.mediafetch.TestMediaSourceResultListPresentation
import me.him188.ani.app.ui.mediafetch.ViewKind
import me.him188.ani.app.ui.mediafetch.rememberTestMediaSelectorState
import me.him188.ani.app.ui.mediafetch.request.TestMediaFetchRequest
import me.him188.ani.app.ui.subject.episode.TAG_MEDIA_SELECTOR_SHEET
import me.him188.ani.app.ui.subject.episode.video.components.EpisodeVideoSideSheets
import me.him188.ani.app.ui.subject.episode.video.settings.SideSheetLayout
import me.him188.ani.datasources.api.source.MediaFetchRequest
import me.him188.ani.utils.platform.annotations.TestOnly
import me.him188.ani.utils.platform.isDesktop

@Composable
internal fun LocalVideoAvailabilityHint(
    hasLocalFileBinding: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = if (LocalPlatform.current.isDesktop()) {
        if (hasLocalFileBinding) {
            "本地视频入口已启用，当前剧集已绑定本地文件。"
        } else {
            "本地视频入口已启用，可使用上方按钮手动选择本地视频。"
        }
    } else {
        "本地视频入口未启用：当前平台不是桌面端。"
    }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun RowScope.LocalVideoActionButtons(
    hasLocalFileBinding: Boolean,
    onBindLocalFile: suspend (String) -> Boolean,
    onClearLocalFileBinding: suspend () -> Boolean,
    onBindSucceeded: () -> Unit = {},
) {
    if (!LocalPlatform.current.isDesktop()) return

    val asyncHandler = rememberAsyncHandler()
    val toaster = LocalToaster.current
    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.Video,
        title = "选择本地视频",
    ) { file ->
        if (file == null) return@rememberFilePickerLauncher
        asyncHandler.launch {
            if (onBindLocalFile(file.absolutePath())) {
                toaster.toast("已绑定本地视频")
                onBindSucceeded()
            } else {
                toaster.toast("绑定失败，请确认文件存在且当前剧集已加载完成")
            }
        }
    }

    FilledTonalButton(onClick = { filePicker.launch() }) {
        Text(if (hasLocalFileBinding) "重新绑定" else "使用本地视频")
    }
    if (hasLocalFileBinding) {
        TextButton(
            onClick = {
                asyncHandler.launch {
                    if (onClearLocalFileBinding()) {
                        toaster.toast("已清除本地绑定")
                    } else {
                        toaster.toast("当前剧集没有本地绑定")
                    }
                }
            },
        ) {
            Text("清除绑定")
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
fun EpisodeVideoSideSheets.MediaSelectorSheet(
    mediaSelectorState: MediaSelectorState,
    mediaSourceResultListPresentation: MediaSourceResultListPresentation,
    viewKind: ViewKind,
    onViewKindChange: (ViewKind) -> Unit,
    fetchRequest: MediaFetchRequest?,
    onFetchRequestChange: (MediaFetchRequest) -> Unit,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit,
    onRestartSource: (instanceId: String) -> Unit,
    hasLocalFileBinding: Boolean,
    onBindLocalFile: suspend (String) -> Boolean,
    onClearLocalFileBinding: suspend () -> Boolean,
    modifier: Modifier = Modifier,
) {
    SideSheetLayout(
        title = { Text(text = "选择数据源") },
        onDismissRequest = onDismissRequest,
        Modifier.testTag(TAG_MEDIA_SELECTOR_SHEET),
        closeButton = {
            IconButton(onClick = onDismissRequest) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭")
            }
        },
    ) {
        LocalVideoAvailabilityHint(
            hasLocalFileBinding = hasLocalFileBinding,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .fillMaxWidth(),
        )

        MediaSelectorView(
            mediaSelectorState,
            viewKind,
            onViewKindChange,
            fetchRequest,
            onFetchRequestChange,
            mediaSourceResultListPresentation,
            onRestartSource = onRestartSource,
            onRefresh,
            modifier.padding(horizontal = 16.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            stickyHeaderBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            topActions = {
                LocalVideoActionButtons(
                    hasLocalFileBinding = hasLocalFileBinding,
                    onBindLocalFile = onBindLocalFile,
                    onClearLocalFileBinding = onClearLocalFileBinding,
                    onBindSucceeded = onDismissRequest,
                )
            },
            onClickItem = {
                mediaSelectorState.select(it)
                onDismissRequest()
            },
            singleLineFilter = true,
        )
    }
}


@OptIn(TestOnly::class)
@Composable
@Preview
@PreviewLightDark
private fun PreviewEpisodeVideoMediaSelectorSideSheet() {
    ProvideCompositionLocalsForPreview {
        val (viewKind, onViewKindChange) = rememberSaveable { mutableStateOf(ViewKind.WEB) }
        EpisodeVideoSideSheets.MediaSelectorSheet(
            mediaSelectorState = rememberTestMediaSelectorState(),
            mediaSourceResultListPresentation = TestMediaSourceResultListPresentation,
            viewKind = viewKind,
            onViewKindChange = onViewKindChange,
            fetchRequest = TestMediaFetchRequest,
            onFetchRequestChange = {},
            onDismissRequest = {},
            onRefresh = {},
            onRestartSource = {},
            hasLocalFileBinding = false,
            onBindLocalFile = { false },
            onClearLocalFileBinding = { false },
        )
    }
}
