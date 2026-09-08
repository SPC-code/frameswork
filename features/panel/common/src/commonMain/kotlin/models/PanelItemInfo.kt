package space.kscience.frameswork.features.panel.common.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer

@Serializable
data class PanelItemInfo(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val config: JsonElement
) {
    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        config: Any,
        json: Json
    ) : this(
        x = x,
        y = y,
        width = width,
        height = height,
        config = json.encodeToJsonElement(configDecoder, config)
    )

    fun decodeConfig(json: Json): Any = json.decodeFromJsonElement(configDecoder, config)

    fun containsX(x: Int): Boolean = this.x <= x && this.x + width > x
    fun containsY(y: Int): Boolean = this.y <= y && this.y + height > y
    fun contains(x: Int, y: Int): Boolean = containsX(x) && containsY(y)

    companion object {
        val configDecoder = PolymorphicSerializer<Any>(Any::class)
    }
}
