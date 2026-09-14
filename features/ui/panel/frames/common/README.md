# Frames panel common

`space.kscience:frameswork.features.ui.panel.frames.common`

This Kotlin Multiplatform module contains the shared contracts used by frames-panel clients and
servers. It defines the frame metadata and streaming interface, the serializable stream selection,
and the relative path segments that both sides use. It does not contain a UI, a transport
implementation, or a server routing configurator.

The module currently publishes JVM and Kotlin/JS IR variants. The JS target supports browser and
Node.js environments. Its common API is shared by both targets.

## Dependency

Use the root multiplatform coordinate and let Gradle select the target-specific variant:

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.ui.panel.frames.common:<version>")
}
```

## Public API

### `FramesDataInfoFeature`

`FramesDataInfoFeature` is the implementation-independent contract used by a frames panel:

- `getAvailableProcessors()` returns a snapshot of processor names.
- `getAvailableFramesSources(processorName)` returns a snapshot of source identifiers, or `null`
  when the processor is unknown.
- `getFramesFlow(processorName, id)` returns a `Flow<ByteArrayFrameData>` for one processor and
  source. Flow sharing, reconnection, and unavailable-source behavior belong to the implementation.

This module supplies the interface, not an implementation.

### `FramesFlowFeatureId`

`FramesFlowFeatureId` is a serializable pair of `processorName` and `FramesSourceId`. Remote
implementations use it to identify the stream requested by a client. Construct it directly:

```kotlin
val streamId = FramesFlowFeatureId(
    processorName = "main",
    framesSourceId = FramesSourceId("camera-1"),
)
```

### `PanelCameraConstants`

`PanelCameraConstants` is a Kotlin object. Access it directly; it is not a DI service. All values
are relative path segments without leading or trailing slashes.

| Constant | Value | Composed relative path | Purpose |
| --- | --- | --- | --- |
| `rootPathPart` | `cameras` | `cameras` | Groups frames-panel operations. |
| `getAvailableProcessorsPathPart` | `getAvailableProcessors` | `cameras/getAvailableProcessors` | Lists processor names. |
| `getAvailableCamerasPathPart` | `getAvailableCameras` | `cameras/getAvailableCameras` | Lists a processor's frame sources. |
| `getFramesPathPart` | `getFrames` | `cameras/getFrames` | Identifies the frame-streaming connection. |

These constants describe a shared wire contract only. This module does not mount endpoints; the
server module is responsible for assigning HTTP/WebSocket methods, parameters, and any deployment
prefix.

## Dependency injection

The module's `Plugin.setupDI` is empty. Its JVM and JS startup adapters delegate to that empty
setup, so installing this module registers no Koin definitions and declares no qualifiers.
Consequently:

- construct `FramesFlowFeatureId` directly;
- access `PanelCameraConstants` directly;
- obtain a `FramesDataInfoFeature` only after an application or an implementation module has
  registered one.

For example, an application can register its own unqualified implementation and retrieve it by the
interface type:

```kotlin
val featureModule = module {
    single<FramesDataInfoFeature> { MyFramesDataInfoFeature(/* dependencies */) }
}

val feature: FramesDataInfoFeature = koin.get()
```

The registration in this example belongs to the application, not to this module. If an application
uses a qualifier, it must use that same application-defined qualifier when retrieving the service;
this module defines none.
