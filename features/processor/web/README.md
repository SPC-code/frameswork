# Processor Web

[Russian version](README.ru.md)

Gradle module: `:frameswork.features.processor.web`  
Maven module: `space.kscience:frameswork.features.processor.web`

This Kotlin Multiplatform module is the JavaScript startup bundle for frame processing. It exports
the APIs of [Processor Common](../common/README.md) and
[Common Web](../../common/web/README.md), and its JavaScript plugin activates the Processor Common
serialization contribution. The module does not declare a web-specific processor, middleware,
user interface, or transport protocol of its own.

## Dependency and platform scope

The current Gradle configuration builds and publishes only a JavaScript target. Add the root
multiplatform coordinate to a JavaScript source set; Gradle selects its `-js` variant:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.processor.web:<version>")
        }
    }
}
```

The repository contains `JVMPlugin.kt` and `AndroidPlugin.kt` source files, but this module does not
declare JVM or Android targets. Those files are therefore not compiled or included in the currently
published module. Do not use this coordinate as a JVM or Android dependency.

## Module API and startup

This module declares no processing classes. Its only compiled declarations are Kotlin singleton
startup plugins:

- [`Plugin`](src/commonMain/kotlin/Plugin.kt) is the common startup hook; its DI and startup methods
  are intentionally empty.
- [`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) delegates DI setup and startup first to
  `space.kscience.frameswork.features.processor.common.JSPlugin` and then to the empty local
  `Plugin`.

Plugin objects are not Koin definitions and have no constructors. Refer to the JavaScript plugin
directly, normally by adding `space.kscience.frameswork.features.processor.web.JSPlugin` to the
application's `StartLauncherPlugin` plugin list:

```kotlin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import space.kscience.frameswork.features.processor.web.JSPlugin

val processorWebPlugin: StartPlugin = JSPlugin
```

Loading this plugin does **not** load
`space.kscience.frameswork.features.common.web.JSPlugin`. An application that needs the shared Ktor
`HttpClient`, web configurators, or navigation registration must add that plugin to its startup list
separately.

## Dependency injection

The local `Plugin` contributes no Koin definitions. Through delegation to Processor Common,
`JSPlugin` contributes exactly one definition:

| Registered type | Qualifier | Exact retrieval | Purpose |
| --- | --- | --- | --- |
| `SerializersModule` | generated random qualifier | `koin.getAllDistinct<SerializersModule>()` | Adds polymorphic serializers for `FramesProcessorMiddleware.Factory.Simple` under both `Any` and `FramesProcessorMiddleware.Factory`. |

`singleWithRandomQualifier` does not expose a stable qualifier, so a singular unqualified
`koin.get<SerializersModule>()` is not the correct way to obtain this contribution. Aggregate it
with the other serializer-module contributions:

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import kotlinx.serialization.modules.SerializersModule

val processorSerializers: List<SerializersModule> = koin.getAllDistinct()
```

No `FramesProcessor`, `FramesProcessorService`, `FramesProcessorMiddleware`, or middleware factory
is registered by the JavaScript plugin. In particular, `koin.get<FramesProcessor>()` is unavailable
unless the host application supplies that definition. The service exported from Processor Common
can instead be constructed directly with all of its required collaborators and settings:

```kotlin
val processor = FramesProcessorService(
    framesCollector = framesCollector,
    middlewares = middlewares,
    parallelProcessorWorks = 1,
    processingThrottlingMillis = null,
    rejectOldParallelHandling = false,
    scope = scope,
)
```

Alternatively, register that instance and its interface explicitly in the host's Koin module:

```kotlin
single {
    FramesProcessorService(
        framesCollector = get(),
        middlewares = getAllDistinct(),
        parallelProcessorWorks = 1,
        processingThrottlingMillis = null,
        rejectOldParallelHandling = false,
        scope = get(),
    )
}
single<FramesProcessor> { get<FramesProcessorService>() }
```

The middleware collection in this host-defined example is an application convention; this web
module itself contributes no JavaScript middleware definitions. See
[Processor Common](../common/README.md) for the processor interfaces, service behavior, constructor
requirements, and serializer details.

## Configurators, routes, and paths

This is not a server module and installs no routes or endpoints. It also declares no HTTP or
WebSocket configurators and no fixed client paths. The API dependency on Common Web only makes that
module's types available at compile time; it does not activate its startup plugin. If the
application separately activates Common Web, supply relative request paths to its shared client as
described in the [Common Web documentation](../../common/web/README.md).
