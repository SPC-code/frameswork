package space.kscience.frameswork.features.frames.common.utils

import dev.inmo.micro_utils.meta.MetaContainer
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.common.common.utils.toByteArray
import space.kscience.frameswork.features.common.common.utils.toInt
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FrameData

/**
 * Encodes this [FrameData] into a self-contained [ByteArray] that bundles both the [FrameData.meta]
 * and the frame payload produced by [FrameData.toByteArray].
 *
 * The resulting layout is:
 * - bytes `[0, 4)`: big-endian length of the JSON-encoded meta block
 * - bytes `[4, 4 + metaLength)`: UTF-8 bytes of the JSON-encoded [MetaContainer]
 * - bytes `[4 + metaLength, size)`: raw frame payload
 *
 * Use [decodeFrameData] to restore the data back into a [ByteArrayFrameData].
 *
 * @param json the [Json] instance used to serialize the [MetaContainer]; must be symmetric with the
 * one passed to [decodeFrameData] on the receiving side.
 */
suspend fun FrameData.encodeToByteArray(json: Json): ByteArray {
    val meta = meta
    val metaEncoded = json.encodeToString(MetaContainer.serializer(), meta)
    val metaInBytes = metaEncoded.encodeToByteArray()
    val metaBytes = metaInBytes.size
    val metaBytesByteArray = metaBytes.toByteArray()

    return metaBytesByteArray + metaInBytes + toByteArray()
}

/**
 * Decodes a [ByteArray] previously produced by [FrameData.encodeToByteArray] into a
 * [ByteArrayFrameData] containing the original meta and frame payload.
 *
 * Expects the layout described in [FrameData.encodeToByteArray]: a 4-byte big-endian meta length
 * prefix, followed by the JSON-encoded [MetaContainer] bytes, followed by the raw frame payload.
 *
 * @param json the [Json] instance used to deserialize the [MetaContainer]; must be compatible with
 * the one used during encoding.
 */
fun ByteArray.decodeFrameData(json: Json): ByteArrayFrameData {
    val dataBytes = ByteArray(4) { get(it) }.toInt()
    val metaBytes = ByteArray(dataBytes) { get(it + 4) }
    val meta = json.decodeFromString(MetaContainer.serializer(), metaBytes.decodeToString())
    val frameBytes = ByteArray(size - 4 - dataBytes) { get(it + 4 + dataBytes) }

    return ByteArrayFrameData(frameBytes, meta)
}
