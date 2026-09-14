# Frames Web

[Russian version](README.ru.md)

Gradle module: `:frameswork.features.frames.web`  
Maven module: `space.kscience:frameswork.features.frames.web`

This Kotlin Multiplatform module is the JavaScript startup bundle for the frame-domain APIs. It
exports [`frameswork.features.frames.common`](../common/README.md) and
[`frameswork.features.common.web`](../../common/web/README.md) as API dependencies and supplies a
JavaScript `StartPlugin` that installs the frame-common registrations. It does not add a web-specific
frame model, service, UI, HTTP client configuration, or transport protocol of its own.

## Dependency and platform scope

The current Gradle configuration builds and publishes only a JavaScript target. Add the root
multiplatform coordinate to a JavaScript source set; Gradle selects its `-js` variant:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.frames.web:<version>")
        }
    }
}
```

The repository contains `JVMPlugin.kt` and `AndroidPlugin.kt` source files, but this module does not
declare JVM or Android targets. Those files are therefore not compiled or included in the currently
published module. Do not use this coordinate as a JVM or Android dependency.

## Module API and startup

This module declares no domain classes. Its only compiled declarations are Kotlin singleton plugin
objects:

- [`Plugin`](src/commonMain/kotlin/Plugin.kt) is the common startup hook. Its DI and startup methods
  are intentionally empty.
- [`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) delegates DI setup and startup to
  `space.kscience.frameswork.features.frames.common.JSPlugin`, then to the empty local `Plugin`.

Plugin objects are not Koin definitions and have no constructors. Refer to them directly, normally
by adding `space.kscience.frameswork.features.frames.web.JSPlugin` to the application's
`StartLauncherPlugin` plugin list.

```kotlin
val frameWebPlugin: StartPlugin =
    space.kscience.frameswork.features.frames.web.JSPlugin
```

Loading this plugin does **not** load
`space.kscience.frameswork.features.common.web.JSPlugin`. An application that needs the shared Ktor
`HttpClient` must add that plugin to the startup list separately. Likewise, the host must supply the
common application registrations, including an unqualified `CoroutineScope`, before resolving the
frame collectors; the standard choice is
`space.kscience.frameswork.features.common.common.JSPlugin`.

## Koin registrations

The local common plugin contributes no Koin definitions. Through its delegation to the frame-common
JavaScript plugin, `JSPlugin` contributes the following definitions:

| Registered type | Qualifier | Exact retrieval | Requirement or result |
| --- | --- | --- | --- |
| `DefaultFramesCollector` | none | `koin.get<DefaultFramesCollector>()` | Requires unqualified `FramesSourcesCollector` and `CoroutineScope` definitions. |
| `FramesCollector` | none | `koin.get<FramesCollector>()` | Resolves the same `DefaultFramesCollector` singleton and has the same requirements. |
| `InMemoryFramesSourcesCollector` | none | `koin.get<InMemoryFramesSourcesCollector>()` | Requires an unqualified `CoroutineScope`; initial connectors come from all distinct `FrameSourceConnector` contributions. |
| `SerializersModule` | generated random qualifier | `koin.getAllDistinct<SerializersModule>()` | Contributes serializers for frame source IDs and frame metadata keys; there is no stable qualifier for singular retrieval. |

The delegated plugin deliberately does not bind `InMemoryFramesSourcesCollector` to
`FramesSourcesCollector` or `MutableFramesSourcesCollector`. If the in-memory implementation should
back the consumer-facing `FramesCollector`, add the interface bindings before retrieving it:

```kotlin
single<FramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }
single<MutableFramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }

val sources: FramesSourcesCollector = koin.get()
val mutableSources: MutableFramesSourcesCollector = koin.get()
val frames: FramesCollector = koin.get()
```

The exported frame implementations can also be constructed without Koin:

```kotlin
val sources = InMemoryFramesSourcesCollector(
    tmpPreset = connectors,
    scope = scope,
)
val frames = DefaultFramesCollector(
    framesCollector = sources,
    scope = scope,
)
```

See the [Frames Common documentation](../common/README.md) for the frame model, connector and
collector APIs, and all construction requirements. See [Common Web](../../common/web/README.md) for
the separately activated `HttpClient` and navigation registrations.

## Routes and client paths

This is not a server module and it installs no routes or endpoints. It also defines no HTTP or
WebSocket configurators and no fixed client paths. The API dependency on Common Web only makes that
module's types available at compile time; it does not activate its client plugin.
