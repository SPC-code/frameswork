# Processor common

Gradle module: `:frameswork.features.processor.common`  
Maven module: `space.kscience:frameswork.features.processor.common`

This Kotlin Multiplatform module defines the shared frame-processing contracts and their default
service implementation. A processor can transform one frame directly or expose transformed flows
for every source supplied by a `FramesCollector`. The JVM variant also provides a middleware that
saves buffered images as JPEG files.

The configured publication targets are JVM and JavaScript. Gradle selects the appropriate variant
from the multiplatform dependency.

Russian documentation: [README.ru.md](README.ru.md).

## Dependency

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.processor.common:<version>")
}
```

## Processing API

- [`FramesProcessorMiddleware`](src/commonMain/kotlin/services/FramesProcessorMiddleware.kt)
  transforms one `FrameData` value. Its result becomes the input to the next middleware.
- `FramesProcessorMiddleware.Factory` creates middleware selected by its serializable `Id`.
  `Factory.Simple` wraps an existing middleware and returns that same instance from
  `createMiddleware()`.
- [`FramesProcessor`](src/commonMain/kotlin/services/FramesProcessor.kt) combines the middleware
  contract with `FramesCollector`. It therefore supports direct `process(frame)` calls, current
  per-source flow lookup, and persistent flow lookup.
- [`FramesProcessorService`](src/commonMain/kotlin/services/FramesProcessorService.kt) is the default
  implementation. It applies middleware in list order. If a middleware throws, the service logs the
  failure, retains the last successful frame, and continues with the next middleware.

`allocateFramesFlow(id)` returns the currently known subscription-aware processed flow, or `null`
when that source is unavailable. `allocatePersistentFramesFlow(id)` never returns `null`: it emits
nothing while the source is absent and automatically follows the flow when the source appears or is
replaced. The processor's `sourcesListUpdatesFlow` contains only source IDs for which the upstream
collector returned a flow.

## Service construction and execution

`FramesProcessorService` is constructed with an upstream `FramesCollector`, an ordered middleware
list, a positive parallel-work limit, an optional throttling interval, an old-work rejection flag,
and the owning `CoroutineScope`:

```kotlin
val processor = FramesProcessorService(
    framesCollector = collector,
    middlewares = listOf(firstMiddleware, secondMiddleware),
    parallelProcessorWorks = 4,
    processingThrottlingMillis = 30,
    rejectOldParallelHandling = true,
    scope = applicationScope,
)
```

The service observes source-list changes, asks the upstream collector for each current source flow,
and builds a processing channel for every successful allocation. Those channels are half-cold:
subscribers to the same source share collection, and the upstream source is collected only while
that processed flow has subscribers. A source-list update closes the old channel and rebuilds the
map.

When `processingThrottlingMillis` is non-null, the service keeps the latest pending frame between
scheduling opportunities, limits active jobs with a semaphore, and sends completed results to the
processed source flow. Busy or superseded frames can be dropped. A value of `0` keeps this output
path without adding an intentional delay. The current `null` branch uses immediate best-effort
scheduling but does not emit scheduled results into the allocated source flow; use direct `process`
calls for that mode when a result is required.

When `rejectOldParallelHandling` is enabled and frames carry `FrameReceiveTimestamp`, observing a
newer completed result cancels still-running older work. The supplied scope owns source observation
and shared-flow jobs; cancel that scope to stop the processor's background work.

## Koin dependency injection

Load [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) or
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) through the application's `StartPlugin` mechanism. Both
delegate shared setup to [`Plugin`](src/commonMain/kotlin/Plugin.kt).

The built-in registrations are exactly:

| Registered type | Target | Qualifier | Exact retrieval | Value |
| --- | --- | --- | --- | --- |
| `SerializersModule` | JVM and JS | generated random qualifier | `koin.getAllDistinct<SerializersModule>()` | Polymorphic serializer contribution for `FramesProcessorMiddleware.Factory.Simple` under both `Any` and `FramesProcessorMiddleware.Factory` |
| `FramesProcessorMiddleware` | JVM only | generated random qualifier | `koin.getAllDistinct<FramesProcessorMiddleware>()` | Singleton `BufferedImageSaverMiddleware("./local/")` |
| `FramesProcessorMiddleware.Factory` | JVM only | generated random qualifier | `koin.getAllDistinct<FramesProcessorMiddleware.Factory>()` | Singleton saver factory with ID `images_saver` and folder `./local/` |

`singleWithRandomQualifier` does not expose a stable qualifier name. Import
`dev.inmo.micro_utils.koin.getAllDistinct` and use collection lookup for these contributions; an
unqualified single-value lookup is not the contract of these registrations. The JVM middleware and
factory entries are separate singletons. Calling the factory creates a new saver rather than
returning the middleware singleton.

The module does **not** register `FramesProcessorService` or bind `FramesProcessor`: the corresponding
code in the common plugin is commented out. `FramesProcessorMiddleware.Factory.Id` and
`Factory.Simple` are value/helper types and are not standalone Koin definitions either. Construct a
processor directly, or add application-owned bindings with the policy values appropriate to the
application:

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import org.koin.dsl.module

val processorModule = module {
    single {
        FramesProcessorService(
            framesCollector = get(),
            middlewares = getAllDistinct<FramesProcessorMiddleware>(),
            parallelProcessorWorks = 4,
            processingThrottlingMillis = 30,
            rejectOldParallelHandling = true,
            scope = get(),
        )
    }
    single<FramesProcessor> { get<FramesProcessorService>() }
}

val processor = koin.get<FramesProcessor>()
```

This custom definition requires the application to provide unqualified `FramesCollector` and
`CoroutineScope` dependencies. On JS there are no built-in middleware contributions, so the
collection is empty unless the application or another feature registers some.

## JVM image saver

[`BufferedImageSaverMiddleware`](src/jvmMain/kotlin/services/middlewares/BufferedImageSaverMiddleware.kt)
creates its destination directory when constructed. For each `BufferedImageFrameData`, it writes a
JPEG named `HH-mm-ss_dd-MM-yyyy.jpg`, then returns the original frame. Other `FrameData`
implementations pass through without file output. The default JVM plugin path, `./local/`, is
relative to the process working directory. Because filenames have one-second resolution, multiple
frames handled in the same second can target the same file.

The JS target contains only the common processing contracts and service. An Android plugin source
also delegates to common setup, but the module's current Gradle configuration does not build or
publish an Android target.

This module provides no Ktor configurators, server routes, or HTTP endpoints.
