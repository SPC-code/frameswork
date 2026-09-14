package space.kscience.frameswork.features.common.common.utils

import korlibs.time.DateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.Double.Companion

/**
 * Serializes [DateTime] values as their Unix timestamp in milliseconds represented by a [Double].
 */
object DateTimeSerializer : KSerializer<DateTime> {
    /** Describes the serialized timestamp as a primitive double named `date_time`. */
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("date_time", PrimitiveKind.DOUBLE)

    /**
     * Decodes a Unix timestamp in milliseconds.
     *
     * @param decoder Decoder supplying the timestamp.
     * @return A date-time value initialized from the decoded timestamp.
     */
    override fun deserialize(decoder: Decoder): DateTime {
        return DateTime(decoder.decodeDouble())
    }

    /**
     * Encodes [value] as its Unix timestamp in milliseconds.
     *
     * @param encoder Encoder receiving the timestamp.
     * @param value Date-time value to encode.
     */
    override fun serialize(encoder: Encoder, value: DateTime) {
        encoder.encodeDouble(value.unixMillis)
    }
}
