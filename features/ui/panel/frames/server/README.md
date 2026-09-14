# Frames panel server

[Русская версия](README.ru.md)

- Gradle module: `:frameswork.features.ui.panel.frames.server`
- Maven multiplatform module: `space.kscience:frameswork.features.ui.panel.frames.server`
- Maven JVM publication: `space.kscience:frameswork.features.ui.panel.frames.server-jvm`

This Kotlin Multiplatform module implements the server side of the
[`FramesDataInfoFeature`](../common/src/commonMain/kotlin/features/FramesDataInfoFeature.kt) contract.
It reads processor and frame-source state locally and contributes a JVM Ktor API used by the frames
panel client. The module exports the [`frames panel common`](../common/README.md),
[`common/server`](../../../../common/server/README.md),
[`frames/server`](../../../../frames/server/README.md), and
[`processor/server`](../../../../processor/server/README.md) APIs.

## Classes and behavior

| Declaration | Purpose |
| --- | --- |
| [`ServerFramesDataInfoFeature`](src/commonMain/kotlin/features/ServerFramesDataInfoFeature.kt) | Implements `FramesDataInfoFeature` from a local `ProcessorsContainer`. It lists processors, reads each processor's current source snapshot, converts non-byte-backed frames, and filters frames by receive timestamp. |
| [`FramesDataInfoFeatureRoutingsConfigurator`](src/jvmMain/kotlin/configurators/FramesDataInfoFeatureRoutingsConfigurator.kt) | Implements `ApplicationRoutingConfigurator.Element` and installs the two HTTP endpoints and one WebSocket endpoint listed below. |

`ServerFramesDataInfoFeature.getFramesFlow` creates a cold flow. When collection begins, it resolves
the requested processor and persistent source flow. An unknown processor produces no values and the
flow completes. For a known processor, the persistent flow remains quiet while the source is absent
and follows later source additions or replacements. Each non-`ByteArrayFrameData` value is converted
with `toByteArray()`. Frames without `FrameReceiveTimestamp` are discarded, as are timestamps older
than the last emitted timestamp; equal timestamps are accepted. The initial comparison value is
`DateTime(0)`.

The constructor also requires a `FramesSourcesCollector`. The current implementation retains that
dependency but obtains source snapshots and streams through `ProcessorsContainer`.

## Startup and dependency injection

When using the startup loader on a JVM, load
[`space.kscience.frameswork.features.ui.panel.frames.server.JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt).
Its `setupDI` invokes the `frames panel common` JVM setup, invokes this module's common `Plugin`
setup, and then adds the routing contribution. Neither local plugin reads configuration keys.

After `JVMPlugin.setupDI` runs, this module contributes these Koin singletons:

| Type | Registration and qualifier | Exact retrieval |
| --- | --- | --- |
| `ServerFramesDataInfoFeature` | Unqualified concrete singleton. | `koin.get<ServerFramesDataInfoFeature>()` |
| `FramesDataInfoFeature` | Unqualified interface singleton resolving to the same `ServerFramesDataInfoFeature` instance. | `koin.get<FramesDataInfoFeature>()` |
| `ApplicationRoutingConfigurator.Element` | A `FramesDataInfoFeatureRoutingsConfigurator` registered with a generated random qualifier. | `koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()`; select the `FramesDataInfoFeatureRoutingsConfigurator` element from the contributed collection. |

The generated routing qualifier is intentionally unstable and has no name that application code can
use. The configurator is not registered under its concrete type, so
`koin.get<FramesDataInfoFeatureRoutingsConfigurator>()` is not a valid lookup. Import
`dev.inmo.micro_utils.koin.getAllDistinct` when retrieving the contributed collection.

Resolving `ServerFramesDataInfoFeature` requires unqualified `ProcessorsContainer` and
`FramesSourcesCollector` definitions. Resolving the route contribution also requires the unqualified
`FramesDataInfoFeature` and `Json`. These dependencies are not registered by this module's plugins;
the application must install the appropriate processor, frames, and common server/common plugins or
provide equivalent definitions. Declaring the Maven dependency alone does not execute a startup
plugin.

For example, after all prerequisites and this module's definitions have been loaded:

```kotlin
val implementation = koin.get<ServerFramesDataInfoFeature>()
val feature = koin.get<FramesDataInfoFeature>()
check(feature === implementation)

val routes = koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()
val framesRoutes = routes.filterIsInstance<FramesDataInfoFeatureRoutingsConfigurator>().single()
```

`Plugin` and `JVMPlugin` are startup objects, not Koin definitions. Both local classes have
public constructors and can also be created without Koin:

```kotlin
val feature: FramesDataInfoFeature = ServerFramesDataInfoFeature(
    processorsContainer = processorsContainer,
    framesSourcesCollector = framesSourcesCollector,
)
val routes = FramesDataInfoFeatureRoutingsConfigurator(feature, json)
```

## Configurator and endpoints

`FramesDataInfoFeatureRoutingsConfigurator` is the module's only configurator. It installs all three
operations beneath `PanelCameraConstants.rootPathPart = "cameras"`:

| Constant | Value |
| --- | --- |
| `PanelCameraConstants.getAvailableProcessorsPathPart` | `getAvailableProcessors` |
| `PanelCameraConstants.getAvailableCamerasPathPart` | `getAvailableCameras` |
| `PanelCameraConstants.getFramesPathPart` | `getFrames` |

Every path below is **relative**: it has no scheme, host, or leading slash. A parent Ktor route may
add another prefix; in the standard server assembly, `KtorConfig.rootRoute` is such an optional
external prefix.

| Method | Relative path | Input | Purpose |
| --- | --- | --- | --- |
| `GET` | `cameras/getAvailableProcessors` | None. | Returns the current set of processor names from `FramesDataInfoFeature.getAvailableProcessors()`. |
| `GET` | `cameras/getAvailableCameras` | Required query parameter `id={processorName}`; `id` is the value of `idParameterName`. | Returns that processor's current `Set<FramesSourceId>`, or a nullable response when the processor is unknown. Despite the path name, the values are frame-source identifiers. |
| `WebSocket` | `cameras/getFrames` | Text selection messages described below. | Streams encoded frames for the latest valid processor/source selection. |

The required processor identifier is a query parameter, not a dynamic path segment. Thus a complete
relative request target can look like `cameras/getAvailableCameras?id=main`. If `id` is missing,
`getOrFail` raises an error whose HTTP representation is determined by the application's global Ktor
configuration.

### WebSocket protocol

The WebSocket sends no frames until the client submits a text message containing a serialized
`FramesFlowFeatureId`:

```json
{
  "processorName": "main",
  "framesSourceId": "camera-left"
}
```

A later valid selection switches the stream via `flatMapLatest`, cancelling collection of the
previous selection. Invalid JSON is logged and ignored, leaving the previous selection unchanged.
The exact text message `ping` receives the text response `pong`. Incoming binary, Close, Ping, and
Pong frames have no application-level action in this configurator.

Each emitted frame is sent as one final binary WebSocket frame. Its payload is produced by
`FrameData.encodeToByteArray(json)`: a four-byte big-endian metadata length, the UTF-8 JSON metadata,
and then the raw frame bytes. The client must use compatible serializers.

The configurator adds no authentication or authorization. HTTP serialization requires Ktor content
negotiation, and the WebSocket route requires the Ktor WebSockets plugin. There are no other
configurators, HTTP endpoints, WebSocket endpoints, or dynamic path segments in this module.
