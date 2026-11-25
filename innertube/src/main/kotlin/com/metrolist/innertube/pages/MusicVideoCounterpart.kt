package com.metrolist.innertube.pages

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * The music video that YouTube Music pairs with a song (or the song paired with a video), and how
 * positions line up between the two, since music videos often have an intro the song doesn't.
 */
data class MusicVideoCounterpart(
    val sourceVideoId: String,
    val targetVideoId: String,
    private val segments: List<Segment>,
    private val forward: Boolean,
) {
    data class Segment(
        val primaryStartMs: Long,
        val counterpartStartMs: Long,
        val durationMs: Long,
    )

    /** Whether YouTube said how the two line up; without it positions are assumed to match. */
    val hasTiming: Boolean get() = segments.isNotEmpty()

    /** Position in the target for a position in the source. */
    fun toTarget(positionMs: Long): Long = map(positionMs, fromPrimary = forward)

    /** Position in the source for a position in the target. */
    fun toSource(positionMs: Long): Long = map(positionMs, fromPrimary = !forward)

    private fun map(
        positionMs: Long,
        fromPrimary: Boolean,
    ): Long {
        if (segments.isEmpty()) return positionMs
        fun from(segment: Segment) = if (fromPrimary) segment.primaryStartMs else segment.counterpartStartMs
        fun to(segment: Segment) = if (fromPrimary) segment.counterpartStartMs else segment.primaryStartMs
        // Between or after segments, keep the offset of the nearest one before instead of jumping back.
        val segment = segments.sortedBy(::from).lastOrNull { from(it) <= positionMs } ?: segments.minBy(::from)
        return (to(segment) + positionMs - from(segment)).coerceAtLeast(0)
    }

    companion object {
        /** Reads the counterpart from a raw /next response. */
        fun parse(
            response: JsonElement,
            sourceVideoId: String,
        ): MusicVideoCounterpart? {
            for (wrapper in response.findObjects("playlistPanelVideoWrapperRenderer")) {
                val primaryId = wrapper.rendererVideoId("primaryRenderer") ?: continue
                val counterparts = wrapper["counterpart"] as? JsonArray ?: continue
                for (entry in counterparts) {
                    val counterpart = entry as? JsonObject ?: continue
                    val counterpartId = counterpart.rendererVideoId("counterpartRenderer") ?: continue
                    val forward = sourceVideoId == primaryId
                    if (!forward && sourceVideoId != counterpartId) continue
                    val targetId = if (forward) counterpartId else primaryId
                    if (targetId == sourceVideoId) continue
                    return MusicVideoCounterpart(sourceVideoId, targetId, counterpart.segments(), forward)
                }
            }
            return null
        }

        private fun JsonElement.findObjects(key: String): List<JsonObject> {
            val found = mutableListOf<JsonObject>()
            fun walk(element: JsonElement) {
                when (element) {
                    is JsonObject -> {
                        (element[key] as? JsonObject)?.let(found::add)
                        element.values.forEach(::walk)
                    }
                    is JsonArray -> element.forEach(::walk)
                    else -> Unit
                }
            }
            walk(this)
            return found
        }

        private fun JsonObject.rendererVideoId(key: String): String? =
            ((this[key] as? JsonObject)?.get("playlistPanelVideoRenderer") as? JsonObject)
                ?.get("videoId")
                ?.let { (it as? JsonPrimitive)?.contentOrNull }
                ?.takeIf { it.isNotBlank() }

        private fun JsonObject.segments(): List<Segment> =
            ((this["segmentMap"] as? JsonObject)?.get("segment") as? JsonArray)
                .orEmpty()
                .mapNotNull { element ->
                    val segment = element as? JsonObject ?: return@mapNotNull null
                    fun long(name: String) = (segment[name] as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }
                    Segment(
                        primaryStartMs = long("primaryVideoStartTimeMilliseconds") ?: return@mapNotNull null,
                        counterpartStartMs = long("counterpartVideoStartTimeMilliseconds") ?: return@mapNotNull null,
                        durationMs = long("durationMilliseconds")?.takeIf { it >= 0 } ?: return@mapNotNull null,
                    )
                }
    }
}
