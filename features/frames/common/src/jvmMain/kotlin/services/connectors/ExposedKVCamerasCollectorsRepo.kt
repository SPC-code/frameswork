package space.kscience.frameswork.features.frames.common.services.connectors

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import space.kscience.frameswork.features.frames.common.models.FrameSourceConnectorConfig
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.services.FrameSourceConnector

private val frameSourceConnectorConfigSerializer = PolymorphicSerializer(FrameSourceConnectorConfig::class)

fun ExposedKVCamerasCollectorsRepo(
    database: Database,
    json: Json
): KeyValueRepo<FramesSourceId, FrameSourceConnector> = ExposedKeyValueRepo<String, String>(
    database,
    { text("id") },
    { text("connector") },
    "camera_connectors"
).withMapper<FramesSourceId, FrameSourceConnector, String, String>(
    { json.encodeToString(FramesSourceId.serializer(), this) },
    { json.encodeToString(frameSourceConnectorConfigSerializer, this.createConfig()) },
    { json.decodeFromString(FramesSourceId.serializer(), this) },
    { json.decodeFromString(frameSourceConnectorConfigSerializer, this).createFramesSourceConnector() },
)
