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

object DateTimeSerializer : KSerializer<DateTime> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("date_time", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): DateTime {
        return DateTime(decoder.decodeDouble())
    }

    override fun serialize(encoder: Encoder, value: DateTime) {
        encoder.encodeDouble(value.unixMillis)
    }
}
