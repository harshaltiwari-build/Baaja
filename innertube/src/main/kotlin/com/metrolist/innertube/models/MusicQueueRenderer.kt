package com.metrolist.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicQueueRenderer(
    val content: Content?,
    val header: Header?,
    val subHeaderChipCloud: SubHeaderChipCloud? = null,
) {
    /** The radio's re-tuning chips (All, Popular, Discover, Deep cuts…) above the queue. */
    @Serializable
    data class SubHeaderChipCloud(
        val chipCloudRenderer: ChipCloudRenderer? = null,
    ) {
        @Serializable
        data class ChipCloudRenderer(
            val chips: List<Chip> = emptyList(),
        )

        @Serializable
        data class Chip(
            val chipCloudChipRenderer: ChipRenderer? = null,
        )

        @Serializable
        data class ChipRenderer(
            val text: Runs? = null,
            val uniqueId: String? = null,
            val isSelected: Boolean = false,
            val navigationEndpoint: ChipEndpoint? = null,
        )

        @Serializable
        data class ChipEndpoint(
            val queueUpdateCommand: QueueUpdateCommand? = null,
        )

        @Serializable
        data class QueueUpdateCommand(
            val fetchContentsCommand: FetchContentsCommand? = null,
        )

        @Serializable
        data class FetchContentsCommand(
            val watchEndpoint: WatchEndpoint? = null,
        )
    }

    @Serializable
    data class Content(
        val playlistPanelRenderer: PlaylistPanelRenderer,
    )

    @Serializable
    data class Header(
        val musicQueueHeaderRenderer: MusicQueueHeaderRenderer?,
    ) {
        @Serializable
        data class MusicQueueHeaderRenderer(
            val title: Runs?,
            val subtitle: Runs?,
        )
    }
}
