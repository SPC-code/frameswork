# Frames common

Gradle module: `:frameswork.features.frames.common`  
Maven module: `space.kscience:frameswork.features.frames.common`

This Kotlin Multiplatform module defines Frameswork's frame payload, source, connector, and collector
abstractions. It also provides subscription-aware source sharing, a binary frame envelope, and JVM
adapters for JavaCV/FFmpeg and Exposed. The configured publication targets are JVM and JavaScript;
Gradle selects the corresponding platform variant from the multiplatform dependency.

Russian documentation: [README.ru.md](README.ru.md).

## Dependency

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.frames.common:<version>")
}
```

## Frame model and metadata

- [`FrameData`](src/commonMain/kotlin/models/FrameData.kt) represents a payload plus a
  `MetaContainer`. `toByteArray()` converts the payload, while `copyWithModifiedMeta` keeps the
  payload semantics of the implementation and copies its metadata.
- [`ByteArrayFrameData`](src/commonMain/kotlin/models/ByteArrayFrameData.kt) holds an existing byte
  array and returns that same array without copying.
- [`FramesSource`](src/commonMain/kotlin/models/FramesSource.kt) allocates frame flows.
  [`OneConnectionFramesSource`](src/commonMain/kotlin/models/OneConnectionFramesSource.kt) shares one
  selected upstream source among its subscribers and switches when the selected source changes.
- [`FramesSourceId`](src/commonMain/kotlin/models/FramesSourceId.kt) is the serializable source ID.
- [`FramesSourceIdMeta`, `FrameSourceWidth`, `FrameSourceHeight`, `FrameReceiveTimestamp`, and
  `FrameSourceTimestampMicroseconds`](src/commonMain/kotlin/models/FrameMeta.kt) are serializable
  metadata keys for source identity, dimensions, receive time, and the source timestamp.
- [`FrameSourceConnectorConfig`](src/commonMain/kotlin/models/FrameSourceConnectorConfig.kt) recreates
  a [`FrameSourceConnector`](src/commonMain/kotlin/services/FrameSourceConnector.kt), which combines a
  source ID, a frame flow, and reproducible configuration.

These model types are not individual Koin definitions. Construct values directly, use the metadata
key objects directly, and obtain connectors from a `FramesSourcesCollector`.

## Collectors

- [`FramesSourcesCollector`](src/commonMain/kotlin/services/FramesSourcesCollector.kt) lists source IDs
  and exposes a live connector flow for each ID.
- [`MutableFramesSourcesCollector`](src/commonMain/kotlin/services/MutableFramesSourcesCollector.kt)
  adds connector insertion and removal.
- [`InMemoryFramesSourcesCollector`](src/commonMain/kotlin/services/InMemoryFramesSourcesCollector.kt)
  serializes mutations in a coroutine actor and loses its contents when the process exits.
- [`KVBasedFramesSourcesCollector`](src/commonMain/kotlin/services/KVBasedFramesSourcesCollector.kt)
  persists connectors in a `KeyValueRepo` and follows repository updates.
- [`FramesCollector`](src/commonMain/kotlin/services/FramesCollector.kt) is the consumer-facing frame
  stream registry. [`DefaultFramesCollector`](src/commonMain/kotlin/services/DefaultFramesCollector.kt)
  shares the upstream connection for each available source among downstream subscribers.

## Startup and Koin DI

Load [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) or
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) through the application's `StartPlugin` mechanism. Both
delegate their common registrations to [`Plugin`](src/commonMain/kotlin/Plugin.kt).

The common registrations are:

| Registered type | Qualifier | Exact retrieval | Notes |
| --- | --- | --- | --- |
| `DefaultFramesCollector` | none | `koin.get<DefaultFramesCollector>()` | Requires an unqualified `FramesSourcesCollector` and `CoroutineScope` |
| `FramesCollector` | none | `koin.get<FramesCollector>()` | Resolves the same `DefaultFramesCollector` singleton |
| `InMemoryFramesSourcesCollector` | none | `koin.get<InMemoryFramesSourcesCollector>()` | Initial connectors come from `getAllDistinct<FrameSourceConnector>()`; requires `CoroutineScope` |
| `SerializersModule` | random, generated internally | `koin.getAllDistinct<SerializersModule>()` | Contributes serializers for `FramesSourceId` and all metadata keys |

The common plugin deliberately does **not** expose `InMemoryFramesSourcesCollector` as
`FramesSourcesCollector` or `MutableFramesSourcesCollector`. On JS, or in a custom common setup, add
those interface bindings when the in-memory registry should back `DefaultFramesCollector`:

```kotlin
single<FramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }
single<MutableFramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }
```

The JVM plugin additionally requires a top-level `cameras` configuration object containing a
`presets` list. When that block is present, it registers:

| Registered type | Qualifier | Exact retrieval |
| --- | --- | --- |
| `JVMPlugin.Config` | none | `koin.get<JVMPlugin.Config>()` |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `exposed_cameras_collector` | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cameras_collector"))` |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `exposed_cached_cameras_collector` | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cached_cameras_collector"))` |
| `KVBasedFramesSourcesCollector` | none | `koin.get<KVBasedFramesSourcesCollector>()` |
| `FramesSourcesCollector` | none | `koin.get<FramesSourcesCollector>()` |
| `MutableFramesSourcesCollector` | none | `koin.get<MutableFramesSourcesCollector>()` |
| `SerializersModule` | none | `koin.get<SerializersModule>()` |

The two source-collector interfaces resolve the same `KVBasedFramesSourcesCollector`. Its cached
repository wraps the raw Exposed repository. The JVM module's unqualified `SerializersModule`
contains the `FFMPEGRTSPConfig` registrations; `getAllDistinct<SerializersModule>()` returns it
together with the randomly qualified common contribution. JVM setup expects `Json`, `Database`, and
`CoroutineScope` to be supplied by the application or prerequisite plugins. During startup, preset
configs are converted to connectors and inserted only when their IDs are not already persisted.

## JVM integrations

- [`FFMPEGRTSPConfig`](src/jvmMain/kotlin/models/FFMPEGRTSPConfig.kt) is the polymorphic
  `ffmpeg_rtsp` configuration and creates an
  [`FFMPEGRTSPFrameSourceConnector`](src/jvmMain/kotlin/services/connectors/FFMPEGRTSPFrameSourceConnector.kt).
  The connector reads RTSP over TCP with JavaCV, converts images to `BufferedImageFrameData`, and
  retries failed starts.
- [`BufferedImageFrameData`](src/jvmMain/kotlin/models/javacv/BufferedImageFrameData.kt) wraps an AWT
  `BufferedImage` and encodes it as JPEG.
- [`JavaCVFrameData`](src/jvmMain/kotlin/models/javacv/JavaCVFrameData.kt) wraps a JavaCV `Frame` and
  exposes the backing array of its data buffer.
- [`ExposedKVCamerasCollectorsRepo`](src/jvmMain/kotlin/services/connectors/ExposedKVCamerasCollectorsRepo.kt)
  constructs the raw Exposed repository used by the JVM plugin. Call it directly only when building
  a custom DI graph.

The JVM frame/config/connector classes are not registered as standalone Koin instances. Connectors
are created from persisted or preset configurations. The JavaScript target uses only the common
model and collector APIs. An Android plugin source exists for reuse, but this module's current Gradle
configuration does not publish an Android target.

## Binary frame envelope

[`FrameData.encodeToByteArray`](src/commonMain/kotlin/utils/ByteArrayFrameMetaToByteArray.kt) writes a
four-byte big-endian metadata length, JSON-encoded `MetaContainer`, and frame payload.
`ByteArray.decodeFrameData` reverses that layout into `ByteArrayFrameData`. Pass compatible `Json`
instances on both sides, including serializers for every metadata key and value. These extensions are
called directly and are not registered in Koin.

This module does not install server routes or expose HTTP endpoints.
