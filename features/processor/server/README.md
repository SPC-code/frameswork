# Processor server

[Русская версия](README.ru.md)

- Gradle module: `:frameswork.features.processor.server`
- Maven multiplatform module: `space.kscience:frameswork.features.processor.server`
- Maven JVM publication: `space.kscience:frameswork.features.processor.server-jvm`

This Kotlin Multiplatform module assembles named frame-processing pipelines for a JVM server. It
exports the [`processor/common`](../common/README.md) API and the
[`common/server`](../../common/server/README.md) infrastructure, provides a container for configured
processors, and adds a JVM image-cropping middleware.

## Classes

| Declaration | Purpose |
| --- | --- |
| `Plugin.Config` | Serializable root configuration containing the map of named processor definitions. |
| [`ProcessorsContainer`](src/commonMain/kotlin/ProcessorsContainer.kt) | Builds one `FramesProcessorService` for every entry in the `processors` configuration and exposes the initialized processors by identifier. |
| [`ProcessorsContainer.ProcessorConfig`](src/commonMain/kotlin/ProcessorsContainer.kt) | Describes an ordered middleware chain, concurrency, throttling, and stale-frame cancellation for one processor. |
| [`FrameCroppingMiddleware`](src/jvmMain/kotlin/processor_middleware/FrameCroppingMiddleware.kt) | Crops a frame to a configured rectangle, returns `BufferedImageFrameData`, and preserves frame metadata. |
| [`FrameCroppingMiddleware.CropData`](src/jvmMain/kotlin/processor_middleware/FrameCroppingMiddleware.kt) | Serializable crop rectangle (`x`, `y`, `width`, and `height`) measured in pixels from the image's top-left corner. |
| [`FrameCroppingMiddleware.Factory`](src/jvmMain/kotlin/processor_middleware/FrameCroppingMiddleware.kt) | Creates cropping middleware under the processor-facing identifier `crop_<suffix>`. |

`ProcessorsContainer` resolves every string in a processor's `middlewares` list as a registered
`FramesProcessorMiddleware.Factory.id` first. If there is no matching factory, it treats the string
as another configured processor identifier, allowing processors to be composed. Middleware
references are evaluated in list order.

The container initializes its processor map in the supplied `CoroutineScope`. Its
`availableProcessors()` and `getProcessor(id)` methods suspend until that map is ready.

## Startup and configuration

With the startup loader, load
`space.kscience.frameswork.features.processor.server.JVMPlugin`. It delegates to
`space.kscience.frameswork.features.processor.common.JVMPlugin` and then to this module's common
`Plugin` for both DI setup and startup. `Plugin` and `JVMPlugin` are Kotlin `object`s; reference them
directly rather than obtaining them from Koin.

The top-level `processors` object is required in practice because `ProcessorsContainer` is an eager
singleton. `middlewares.crops` is optional. Each entry below `crops` creates a factory whose ID is the
entry name prefixed with `crop_`:

```json
{
  "middlewares": {
    "crops": {
      "preview": {
        "x": 40,
        "y": 20,
        "width": 640,
        "height": 360
      }
    }
  },
  "processors": {
    "preview": {
      "middlewares": ["crop_preview"],
      "parallelProcessing": 8,
      "processingThrottlingMillis": 30,
      "rejectOldParallelHandling": false
    }
  }
}
```

`middlewares` is required for each processor, and `parallelProcessing` must be positive. The other
processor fields have the defaults shown in the example. Setting `processingThrottlingMillis` to
`null` disables time-based throttling. In the current `FramesProcessorService` implementation, that
`null` branch performs best-effort scheduling but does not emit completed frames to allocated source
flows; keep a non-null value for flow consumers, or call `process(frame)` directly when the result is
required. Crop coordinates and dimensions must describe a valid rectangle inside every input image.

The server must also supply the unqualified `Json`, `CoroutineScope`, and `FramesCollector` required
by these definitions. The usual application loads the `common/server` JVM plugin for `Json` and the
scope, and the `frames/common` JVM plugin with its `cameras` configuration for the collector, before
this plugin. Applications may provide equivalent bindings themselves. Merely depending on those
artifacts does not run their plugins.

## Dependency injection and direct construction

Calling `JVMPlugin.setupDI` produces the following local definitions:

| Type | Registration and qualifier | Retrieval |
| --- | --- | --- |
| `Plugin.Config` | Unqualified singleton, registered only when the top-level `processors` key exists. It is decoded from the complete root configuration. | `koin.get<Plugin.Config>()` |
| `ProcessorsContainer` | Unqualified singleton with `createdAtStart = true`; its definition is always added. | `koin.get<ProcessorsContainer>()` |
| `FramesProcessorMiddleware.Factory` | One singleton per `middlewares.crops` entry, each registered with a generated random qualifier and backed by `FrameCroppingMiddleware.Factory`. | `koin.getAllDistinct<FramesProcessorMiddleware.Factory>()`; select by `factory.id`, such as `crop_preview`. |

Import `dev.inmo.micro_utils.koin.getAllDistinct` for the collection lookup. Random qualifiers are
generated at registration time and are not stable application-facing names. Consequently, crop
factories should not be fetched by a copied qualifier or by
`koin.get<FrameCroppingMiddleware.Factory>()`.

The delegated `processor/common` JVM plugin also contributes its serializers module, the default
`BufferedImageSaverMiddleware`, and its `images_saver` factory. Those collection contributions are
documented in the [`processor/common` README](../common/README.md).

All locally declared classes can also be constructed without Koin:

```kotlin
val crop = FrameCroppingMiddleware(
    FrameCroppingMiddleware.CropData(x = 40, y = 20, width = 640, height = 360)
)
val factory = FrameCroppingMiddleware.Factory(
    FrameCroppingMiddleware.CropData(x = 40, y = 20, width = 640, height = 360),
    suffix = "preview"
)
val container = ProcessorsContainer(
    framesCollector = framesCollector,
    scope = scope,
    processorsConfigs = mapOf(
        "preview" to ProcessorsContainer.ProcessorConfig(
            middlewares = listOf("crop_preview")
        )
    ),
    middlewaresFactories = listOf(factory)
)
```

Directly constructed crop middleware and factories are independent of DI. A directly constructed
container still initializes asynchronously in the provided scope.

## Configurators and endpoints

This module provides no Ktor application configurator and no routing configurator. It installs:

- no HTTP endpoints;
- no WebSocket endpoints.

Therefore, there are no relative endpoint paths to list. The `server` name refers to server-side
processor assembly, not to an HTTP API. Other server feature modules may retrieve the unqualified
`ProcessorsContainer` and expose its processors through their own relative routes.
