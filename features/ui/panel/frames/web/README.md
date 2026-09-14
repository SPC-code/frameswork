# Frames panel web client

[Russian version](README.ru.md)

Gradle module: `:frameswork.features.ui.panel.frames.web`  
Maven module: `space.kscience:frameswork.features.ui.panel.frames.web`

This Kotlin Multiplatform module provides the browser client and Compose Web UI for displaying
byte-backed camera frames in an editable panel. It combines an HTTP/WebSocket implementation of the
[shared frames-panel contract](../common/README.md), metadata caching and frame-rate limiting,
camera navigation models, a camera selector and live view, plus small Plotly.js external facades.

The current Gradle configuration publishes only a Kotlin/JS target. The `jvmMain` and `androidMain`
plugin source files are not compiled by this module.

## Dependency

Add the multiplatform coordinate to a JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.ui.panel.frames.web:<version>")
        }
    }
}
```

The module exposes `frameswork.features.ui.panel.frames.common`, `frameswork.features.ui.panel`, and
`frameswork.features.frames.web` as API dependencies.

## Transport and frame-flow features

[`KtorFramesDataInfoFeature`](src/commonMain/kotlin/ktor/KtorFramesDataInfoFeature.kt) implements
`FramesDataInfoFeature` with a supplied Ktor `HttpClient` and `Json`:

- metadata snapshots are requested over HTTP;
- each `getFramesFlow` call creates a flow backed by a reconnecting WebSocket and shared in the
  supplied `CoroutineScope`;
- after one second without a frame by default, the client sends the application text message
  `ping` and allows five seconds for a response; and
- after a session finishes or fails, it waits three seconds by default before reconnecting.

[`GetFramesProxyFramesDataInfoFeature`](src/commonMain/kotlin/features/GetFramesProxyFramesDataInfoFeature.kt)
delegates metadata and applies a caller-provided transformation to frame flows.
`GetFramesBackPressurePanelCameraInfoFeature` creates this decorator with `Flow.sample`, which drops
intermediate frames when a producer is faster than the chosen interval.

[`CacheFramesDataInfoFeature`](src/commonMain/kotlin/features/CacheFramesDataInfoFeature.kt) caches
processor and per-processor source snapshots independently for five seconds by default, including a
nullable source result. It also memoizes the fallback flow object for each processor/source pair.
It does not itself make a cold fallback flow hot; the default Koin graph places it around the
already shared Ktor flow.

The default plugin pipeline is:

```text
KtorFramesDataInfoFeature
  -> GetFramesProxyFramesDataInfoFeature (sample every 30 ms, qualifier "back_pressure")
  -> CacheFramesDataInfoFeature
  -> unqualified FramesDataInfoFeature
  -> CameraModel
```

All three feature implementations can also be composed directly:

```kotlin
val remote = KtorFramesDataInfoFeature(
    client = client,
    json = json,
    scope = scope,
)
val sampled = GetFramesBackPressurePanelCameraInfoFeature(remote, 30.milliseconds)
val frames: FramesDataInfoFeature = CacheFramesDataInfoFeature(
    fallback = sampled,
    scope = scope,
)
```

## Camera model and UI

- [`CameraModel`](src/commonMain/kotlin/ui/camera/CameraModel.kt) is the UI-facing contract for
  processor discovery, camera-source discovery, and frame streams. The plugin supplies an adapter
  backed by the unqualified `FramesDataInfoFeature`; applications may implement it directly.
- [`CameraViewConfig`](src/commonMain/kotlin/ui/camera/CameraViewConfig.kt) is the serializable
  `ViewConfig` containing a `FramesFlowFeatureId`. Construct it directly when selecting a stream.
- [`CameraViewModel`](src/commonMain/kotlin/ui/camera/CameraViewModel.kt) switches its `frames` flow
  whenever the navigation configuration selects another processor/source pair.
- [`CameraPanelViewConfigProvider`](src/jsMain/kotlin/ui/camera/CameraPanelViewConfigProvider.kt)
  draws processor and camera dropdowns, refreshes their snapshots once per second, and reports a
  complete `CameraViewConfig` to the panel editor.
- [`CameraView`](src/jsMain/kotlin/ui/camera/CameraView.kt) base64-encodes each payload as a JPEG data
  URL. Its dashboard overlay shows the selected stream, live/offline state, measured frame rate,
  and dimensions when `FrameSourceWidth` and `FrameSourceHeight` metadata are present. The last
  image remains visible when the stream becomes offline.
- [`CommonPanelStrings`](src/commonMain/kotlin/CommonPanelStrings.kt) and
  [`CameraStrings`](src/jsMain/kotlin/CameraStrings.kt) contain English/Russian string resources.
  They are Kotlin objects and are accessed directly, not through Koin.

## Koin registrations

Add `space.kscience.frameswork.features.ui.panel.frames.JSPlugin` to the application's
`StartLauncherPlugin` list. It installs the common definitions below and the two JS-only
contributions. It does not activate startup plugins from API dependencies, so the host must also
install the common application/web and panel plugins it needs. In particular, the graph must supply
unqualified `HttpClient`, `Json`, and `CoroutineScope` definitions.

| Registered type | Qualifier | Retrieval | Notes |
| --- | --- | --- | --- |
| `KtorFramesDataInfoFeature` | none | `koin.get<KtorFramesDataInfoFeature>()` | Singleton; requires unqualified `HttpClient`, `Json`, and `CoroutineScope`. |
| `FramesDataInfoFeature` | `StringQualifier("back_pressure")` | `koin.get<FramesDataInfoFeature>(named("back_pressure"))` | Singleton sampling proxy with a 30 ms interval. |
| `CacheFramesDataInfoFeature` | none | `koin.get<CacheFramesDataInfoFeature>()` | Singleton around the qualified sampling proxy. |
| `FramesDataInfoFeature` | none | `koin.get<FramesDataInfoFeature>()` | Resolves the same cache singleton. |
| `CameraModel` | none | `koin.get<CameraModel>()` | Singleton adapter around the unqualified frame feature. |
| `CameraViewModel` | none | `koin.get<CameraViewModel> { parametersOf(node) }` | Factory; requires a `NavigationNode<CameraViewConfig, ViewConfig>` parameter. `CameraView` supplies itself as that parameter. |
| `SerializersModule` | generated random qualifier | `koin.getAllDistinct<SerializersModule>()` | Adds `CameraViewConfig` polymorphic serializers for both `Any` and `ViewConfig`. |
| `NavigationNodeFactory<ViewConfig>` | generated random qualifier | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | JS-only typed factory that constructs `CameraView`. |
| `PanelViewConfigProvider` | generated random qualifier | `koin.getAllDistinct<PanelViewConfigProvider>()` | JS-only contribution that constructs `CameraPanelViewConfigProvider`. |

`named("back_pressure")` creates the same string qualifier as the plugin's explicit
`StringQualifier("back_pressure")`. The random qualifiers generated by `singleWithRandomQualifier`
have no stable value; retrieve those contributions as collections with
`dev.inmo.micro_utils.koin.getAllDistinct`, not with an invented name. The concrete
`CameraPanelViewConfigProvider` and `CameraView` types are not Koin definitions.

Resolve the aggregated `Json`, navigation factories, and panel providers only after all startup
modules have contributed their definitions. Plugin singleton objects themselves are direct Kotlin
objects, not Koin services.

## Relative client paths and server ownership

All paths are relative, contain no leading slash, and are resolved against the supplied
`HttpClient` base URL.

| Transport | Relative path | Request | Purpose |
| --- | --- | --- | --- |
| `GET` | `cameras/getAvailableProcessors` | No parameters | Return the available processor names; the client uses an empty set when the response status is not `200 OK`. |
| `GET` | `cameras/getAvailableCameras` | Query `id={processorName}` | Return that processor's `Set<FramesSourceId>?`. `id` is the shared `idParameterName`. |
| WebSocket | `cameras/getFrames` | First text message is a serialized `FramesFlowFeatureId` | Receive encoded `ByteArrayFrameData` values as binary frames; text `ping`/`pong` messages provide the application heartbeat. |

This client module installs no routes or server endpoints. The matching endpoints are mounted by
`FramesDataInfoFeatureRoutingsConfigurator` from the separate
[frames-panel server module](../server/README.md). A host application may mount that configurator
under an additional prefix, which must also be reflected in the client's base URL.

## Plotly.js facades

[`Plotly`](src/jsMain/kotlin/utils/plotly/Plotly.kt) exposes `newPlot`, `react`, and the nested
[`Plots.resize`](src/jsMain/kotlin/utils/plotly/Plots.kt) operation to Kotlin/JS. These are external
declarations, not implementations or Koin services. This artifact does not bundle Plotly.js; the
host page must load a compatible script that exposes the global `Plotly` object before calling
them.
