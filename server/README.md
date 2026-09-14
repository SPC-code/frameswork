# Frameswork server bundle

[Русская версия](README.ru.md)

- Gradle module: `:frameswork.server`
- Maven coordinate: `space.kscience:frameswork.server`
- JVM target: Java 17

This module is the published JVM dependency bundle for a Frameswork server. Its `api` dependencies
put the server-side feature modules, Ktor server/client support, Exposed JDBC, and the PostgreSQL and
H2 drivers on a consumer's classpath.

The module has no `server/src` directory. Its JAR therefore contains no local Kotlin declaration,
startup plugin, dependency-injection definition, configurator, route, resource, or `main` function.
Adding the dependency makes the exported APIs available, but does not start Koin, load any feature
plugin, or start Ktor.

## Aggregated feature modules

All five project dependencies are exported with Gradle's `api` configuration:

| Feature | Main purpose | Module documentation |
| --- | --- | --- |
| `:frameswork.features.common.server` | Koin/Ktor bootstrap, common server configuration, persistence, static files, and shared server configurators. | [`features/common/server`](../features/common/server/README.md) |
| `:frameswork.features.panel.server` | Persistent and in-memory panel configuration plus its HTTP API. | [`features/panel/server`](../features/panel/server/README.md) |
| `:frameswork.features.frames.server` | Server integration for frame sources and collectors. | [`features/frames/server`](../features/frames/server/README.md) |
| `:frameswork.features.processor.server` | Configured frame-processing pipelines and JVM crop middleware. | [`features/processor/server`](../features/processor/server/README.md) |
| `:frameswork.features.ui.panel.frames.server` | Server implementation and routes used by the frames panel UI. | [`features/ui/panel/frames/server`](../features/ui/panel/frames/server/README.md) |

The bundle also exports Ktor Tomcat, WebSockets, Apache client, and kotlinx-serialization integration;
Exposed JDBC; and the PostgreSQL and H2 JDBC drivers. The common server feature currently constructs
a Netty embedded server, despite this bundle also exporting the Tomcat artifact.

To consume the published bundle from another JVM project, use the repository version required by the
application (the version in this checkout is `0.0.1`):

```groovy
dependencies {
    implementation "space.kscience:frameswork.server:0.0.1"
}
```

Within this build, use `implementation project(":frameswork.server")` instead.

## Bootstrap and configuration

There is no `space.kscience.frameswork.server.JVMPlugin`. A launcher application must use the
MicroUtils startup launcher available transitively from `common/server` and list every required
Frameswork startup plugin in the root JSON configuration. For the five aggregated features, a minimal
shape is:

```json
{
  "host": "127.0.0.1",
  "port": 8196,
  "publicHost": "127.0.0.1",
  "rootRoute": null,
  "staticFolders": {},
  "database": {
    "url": "jdbc:h2:./server/local.test",
    "driver": "org.h2.Driver",
    "username": "",
    "password": ""
  },
  "plugins": [
    "space.kscience.frameswork.features.common.server.JVMPlugin",
    "space.kscience.frameswork.features.frames.server.JVMPlugin",
    "space.kscience.frameswork.features.processor.server.JVMPlugin",
    "space.kscience.frameswork.features.panel.server.JVMPlugin",
    "space.kscience.frameswork.features.ui.panel.frames.server.JVMPlugin"
  ],
  "cameras": {
    "presets": []
  },
  "processors": {}
}
```

`host`, `port`, and `rootRoute` configure the Netty listener and route prefix through
`common/server`. The `database` object configures its Exposed connection. `cameras` is consumed by the
Frames JVM plugin, and `processors` is required by the processor server plugin even when no processor
is configured. Feature-specific keys and constraints are documented in the linked module READMEs.

Relative filesystem values are resolved from the launcher's working directory. In the example above,
`jdbc:h2:./server/local.test` is appropriate when the process starts in the repository root. A static
mapping such as `"": "../client/build/dist/js/developmentExecutable"` instead assumes that the
process starts in `server/`, as in [`sample.config.dev.json`](sample.config.dev.json). The container
template [`sample.config.docker-compose.json`](sample.config.docker-compose.json) maps static files
from `/static/`.

The two checked-in sample configurations are historical deployment templates, not minimal verified
configurations: they reference `space.kscience.frameswork.features.markup.*` plugins, while no markup
module is included by the current [`settings.gradle`](../settings.gradle), and their H2 database
objects omit the required `org.h2.Driver` override. Remove unavailable plugins and set the driver (or
supply a PostgreSQL URL and credentials) before using either template.

## Dependency injection

This aggregation module defines no classes and registers no Koin definitions, so there is no local
type to retrieve with `get()` or `inject()`. Dependency declarations alone do not register the
bindings from the aggregated modules; their `JVMPlugin.setupDI` methods must be run by the startup
launcher (or invoked by equivalent application bootstrap code).

Once the corresponding feature plugins have completed DI setup, representative unqualified lookups
are:

```kotlin
val globals = koin.get<GlobalKVRepo>()
val frames = koin.get<FramesCollector>()
val frameSources = koin.get<FramesSourcesCollector>()
val panel = koin.get<PanelFeature>()
val processors = koin.get<ProcessorsContainer>()
val framesData = koin.get<FramesDataInfoFeature>()
```

These are transitive bindings, not bindings owned by `server`. Some feature definitions are
conditional on configuration, and configurator contributions use collection bindings or qualifiers.
Use the linked feature README for the complete binding list, exact qualifier, availability condition,
and direct-construction alternative for each type.

## Configurators and endpoints

This module has no local startup plugin or source code, so it installs no configurator and no HTTP or
WebSocket endpoint. In particular, adding `space.kscience:frameswork.server` does not install the
routes listed below.

The following route contributions are only available transitively and are installed only when the
originating feature plugin and the common server plugin are loaded. Every path is **relative**: it has
no scheme, host, or leading slash. A non-null `rootRoute` prefixes all contributed paths.

| Origin | Configurator | Method and relative path | Purpose |
| --- | --- | --- | --- |
| [`common/server`](../features/common/server/README.md#configurators-and-endpoints) | `InternalApplicationRoutingConfigurator` plus its static-files element | `GET [rootRoute/]<configured-static-path>/{file...}` | Serves each configured local static directory. The other common configurators install infrastructure but no fixed endpoint. |
| [`panel/server`](../features/panel/server/README.md#configurator-and-endpoints) | `PanelRoutingsConfigurator` | `GET panel/get` | Returns the current panel configuration. |
| [`panel/server`](../features/panel/server/README.md#configurator-and-endpoints) | `PanelRoutingsConfigurator` | `GET panel/default` | Returns the configured default panel configuration. |
| [`panel/server`](../features/panel/server/README.md#configurator-and-endpoints) | `PanelRoutingsConfigurator` | `POST panel/set` | Stores a submitted panel configuration and returns the operation result. |
| [`frames/server`](../features/frames/server/README.md#configurators-and-endpoints) | None | None | This feature contributes no route. |
| [`processor/server`](../features/processor/server/README.md#configurators-and-endpoints) | None | None | This feature contributes no route. |
| [`frames panel/server`](../features/ui/panel/frames/server/README.md#configurator-and-endpoints) | `FramesDataInfoFeatureRoutingsConfigurator` | `GET cameras/getAvailableProcessors` | Returns the available processor identifiers. |
| [`frames panel/server`](../features/ui/panel/frames/server/README.md#configurator-and-endpoints) | `FramesDataInfoFeatureRoutingsConfigurator` | `GET cameras/getAvailableCameras?id={processorName}` | Returns the current frame-source identifiers for one processor. |
| [`frames panel/server`](../features/ui/panel/frames/server/README.md#configurator-and-endpoints) | `FramesDataInfoFeatureRoutingsConfigurator` | `WebSocket cameras/getFrames` | Accepts processor/source selections and streams encoded frames. |

See the origin README for exact request bodies, nullable responses, WebSocket messages, binary frame
format, DI registration, and infrastructure requirements.

## Build, publish, and run

From the repository root, build and verify this bundle with:

```bash
./gradlew :frameswork.server:build
```

The plain aggregation JAR is written to `server/build/libs/frameswork.server-<version>.jar`. To test
the Maven publication locally, run:

```bash
./gradlew :frameswork.server:publishToMavenLocal
```

The current `server/build.gradle` does not apply Gradle's `application` plugin. Consequently this
module has no `run`, `startScripts`, `assembleDist`, `distTar`, or `distZip` task and cannot be launched
directly. A runnable consumer must apply `application`, depend on this bundle, and set:

```groovy
application {
    mainClass = "dev.inmo.micro_utils.startup.launcher.MainKt"
}
```

That consumer can then pass one absolute configuration path to the launcher, for example:

```bash
./gradlew :your.runner:run --args="/absolute/path/to/frameswork/server/config.json"
```

[`Dockerfile`](Dockerfile) and the two deploy scripts currently expect
`server/build/distributions/frameswork.server.tar`, which the present Gradle module does not produce,
as well as `server/build/productionExecutable.tar`. They become usable only when a runnable
application build supplies those archives. [`docker-compose.yml`](docker-compose.yml) starts only the
development PostgreSQL service; [`sample.docker-compose.yml`](sample.docker-compose.yml) illustrates
the intended application/container layout but does not create the missing distribution.
