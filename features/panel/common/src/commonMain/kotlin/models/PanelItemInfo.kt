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

/**
 * A serializable item placed in a rectangular, half-open region of a panel grid.
 *
 * The occupied coordinates are `[x, x + width)` horizontally and `[y, y + height)` vertically.
 * This value does not validate its coordinates or dimensions.
 *
 * @property x horizontal coordinate of the item's left edge.
 * @property y vertical coordinate of the item's top edge.
 * @property width width of the occupied region in slots.
 * @property height height of the occupied region in slots.
 * @property config JSON representation of the item-specific configuration.
 */
@Serializable
data class PanelItemInfo(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val config: JsonElement
) {
    /**
     * Creates an item by encoding [config] polymorphically under the [Any] base class.
     *
     * The supplied [json] instance must contain a polymorphic serializer for the runtime type of
     * [config].
     *
     * @param x horizontal coordinate of the item's left edge.
     * @param y vertical coordinate of the item's top edge.
     * @param width width of the occupied region in slots.
     * @param height height of the occupied region in slots.
     * @param config item-specific configuration to encode.
     * @param json JSON instance used for encoding.
     */
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

    /**
     * Decodes [config] polymorphically under the [Any] base class.
     *
     * The supplied [json] instance must contain the serializer for the encoded concrete type.
     */
    fun decodeConfig(json: Json): Any = json.decodeFromJsonElement(configDecoder, config)

    /** Returns whether [x] lies within this item's half-open horizontal interval. */
    fun containsX(x: Int): Boolean = this.x <= x && this.x + width > x

    /** Returns whether [y] lies within this item's half-open vertical interval. */
    fun containsY(y: Int): Boolean = this.y <= y && this.y + height > y

    /** Returns whether the point at [x], [y] lies within this item's half-open region. */
    fun contains(x: Int, y: Int): Boolean = containsX(x) && containsY(y)

    /** Holds the serializer used for polymorphic item configuration values. */
    companion object {
        /** A polymorphic serializer whose base class is [Any]. */
        val configDecoder = PolymorphicSerializer<Any>(Any::class)
    }
}
