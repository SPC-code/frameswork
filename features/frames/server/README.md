# Frames server integration

[Русская версия](README.ru.md)

- Gradle module: `:frameswork.features.frames.server`
- Maven multiplatform module: `space.kscience:frameswork.features.frames.server`
- Maven JVM publication: `space.kscience:frameswork.features.frames.server-jvm`

This Kotlin Multiplatform module is the JVM server-side integration point for the Frames feature. It
exports [`frames/common`](../common/README.md) and the
[`common/server`](../../common/server/README.md) infrastructure as API dependencies and provides a
startup entry point that delegates to the JVM Frames plugin. It currently contains no server-specific
domain class, service implementation, Ktor configurator, or route.

## Startup entry points

| Declaration | Purpose |
| --- | --- |
| [`Plugin`](src/commonMain/kotlin/Plugin.kt) | Common `StartPlugin` entry point. Its overrides add no local bindings or module-specific startup work. |
| [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) | JVM `StartPlugin` entry point. It runs `space.kscience.frameswork.features.frames.common.JVMPlugin` and then the local wiring-only `Plugin` during both DI setup and startup. |

The two declarations are Kotlin `object`s, so they are referenced directly as `Plugin` and
`JVMPlugin`; they are not constructed or obtained from Koin.

Merely adding this artifact as a dependency does not run either plugin. When using the startup
launcher, load `space.kscience.frameswork.features.frames.server.JVMPlugin`. Do not also load
`space.kscience.frameswork.features.frames.common.JVMPlugin`, because the server entry point already
delegates to it.

Despite the API dependency on `common/server`, this module does **not** invoke
`space.kscience.frameswork.features.common.server.JVMPlugin`. A complete server must load that plugin
separately (or otherwise supply its prerequisites) before starting the Frames plugin. A typical plugin
selection is therefore:

```json
{
  "plugins": [
    "space.kscience.frameswork.features.common.server.JVMPlugin",
    "space.kscience.frameswork.features.frames.server.JVMPlugin"
  ],
  "cameras": {
    "presets": []
  }
}
```

The delegated Frames JVM plugin expects the `cameras` object when it is started. Its `presets` array
contains polymorphic `FrameSourceConnectorConfig` values. For example, the bundled RTSP connector is
encoded as `["ffmpeg_rtsp", {"id": "camera-id", "url": "rtsp://..."}]` when using the shared
array-polymorphic `Json` configuration.

## Dependency injection

This module adds no Koin definitions of its own. Calling `JVMPlugin.setupDI` delegates the following
definitions to `frames/common`; they are listed here so consumers can distinguish DI-managed services
from directly constructed API types.

| Type | Exact qualifier | Availability | Koin retrieval |
| --- | --- | --- | --- |
| `DefaultFramesCollector` | None | Always registered | `koin.get<DefaultFramesCollector>()` |
| `FramesCollector` | None | Always registered; resolves to `DefaultFramesCollector` | `koin.get<FramesCollector>()` |
| `InMemoryFramesSourcesCollector` | None | Always registered | `koin.get<InMemoryFramesSourcesCollector>()` |
| Frames metadata `SerializersModule` contribution | Generated random qualifier | Always registered | `koin.getAllDistinct<SerializersModule>()` |
| `space.kscience.frameswork.features.frames.common.JVMPlugin.Config` | None | Registered when `cameras` is present | `koin.get<space.kscience.frameswork.features.frames.common.JVMPlugin.Config>()` |
| RTSP `SerializersModule` contribution | None | Registered when `cameras` is present | `koin.get<SerializersModule>()` (also included in `getAllDistinct`) |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `StringQualifier("exposed_cameras_collector")` | Registered when `cameras` is present | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cameras_collector"))` |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `StringQualifier("exposed_cached_cameras_collector")` | Registered when `cameras` is present | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cached_cameras_collector"))` |
| `KVBasedFramesSourcesCollector` | None | Registered when `cameras` is present | `koin.get<KVBasedFramesSourcesCollector>()` |
| `FramesSourcesCollector` | None | Registered when `cameras` is present; resolves to `KVBasedFramesSourcesCollector` | `koin.get<FramesSourcesCollector>()` |
| `MutableFramesSourcesCollector` | None | Registered when `cameras` is present; resolves to `KVBasedFramesSourcesCollector` | `koin.get<MutableFramesSourcesCollector>()` |

Collection lookup requires `dev.inmo.micro_utils.koin.getAllDistinct`; named lookups require
`org.koin.core.qualifier.StringQualifier`. The generated random qualifier is intentionally not a
stable name and must not be copied into application code.

The delegated definitions require an unqualified `Json`, `CoroutineScope`, and `Database`. The
recommended `common/server` JVM plugin supplies them. `DefaultFramesCollector` also requires a
`FramesSourcesCollector` when it is first resolved; the normal `cameras` setup supplies the
`KVBasedFramesSourcesCollector` implementation.

There are no locally declared constructible classes. Public frame models, collector interfaces, and
concrete collectors come from the exported `frames/common` module. They may be constructed directly
when DI is not wanted—for example, `InMemoryFramesSourcesCollector(connectors, scope)` and
`DefaultFramesCollector(sourceRegistry, scope)`—and their complete API is documented in the
[`frames/common` README](../common/README.md).

## Configurators and endpoints

This module provides no Ktor application configurator and no routing configurator. It installs:

- no HTTP endpoints;
- no WebSocket endpoints.

Consequently, there are no relative endpoint paths to list. Loading the separate `common/server`
plugin installs its infrastructure and configuration-driven routes; those configurators and their
relative paths are documented in the [`common/server` README](../../common/server/README.md).
