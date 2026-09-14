# Frameswork

Frameswork is a Kotlin Multiplatform toolkit for collecting, processing, transporting, and
displaying video frames. Its feature modules share Koin-based startup wiring and provide JVM server
integrations, Kotlin/JS clients, and Compose Web user interfaces.

The project group is `space.kscience`; the current version is defined in
[`gradle.properties`](gradle.properties). The Maven names below are base multiplatform coordinates:
append `:<version>` when declaring a dependency. Gradle selects target publications such as `-jvm`
or `-js` where applicable.

## Feature modules

| Gradle module | Main purpose | Maven module | Documentation |
| --- | --- | --- | --- |
| `:frameswork.features.common.common` | Shared serialization, coroutine, flow, metadata, byte-array, and weak-reference utilities. | `space.kscience:frameswork.features.common.common` | [English](features/common/common/README.md) · [Русский](features/common/common/README.ru.md) |
| `:frameswork.features.common.server` | Common JVM server bootstrap: Ktor configuration, authentication hooks, compression, WebSockets, static files, and global key-value persistence. | `space.kscience:frameswork.features.common.server` | [English](features/common/server/README.md) · [Русский](features/common/server/README.ru.md) |
| `:frameswork.features.common.web` | Shared web-client infrastructure: configured Ktor client, navigation primitives, resources, and Compose Web FlotView components. | `space.kscience:frameswork.features.common.web` | [English](features/common/web/README.md) · [Русский](features/common/web/README.ru.md) |
| `:frameswork.features.frames.common` | Frame models, source and collector contracts, binary envelopes, and JVM JavaCV/FFmpeg and Exposed adapters. | `space.kscience:frameswork.features.frames.common` | [English](features/frames/common/README.md) · [Русский](features/frames/common/README.ru.md) |
| `:frameswork.features.frames.server` | JVM startup integration that exposes the common frame APIs to server applications. | `space.kscience:frameswork.features.frames.server` | [English](features/frames/server/README.md) · [Русский](features/frames/server/README.ru.md) |
| `:frameswork.features.frames.web` | JavaScript startup integration that exposes the common frame APIs to web clients. | `space.kscience:frameswork.features.frames.web` | [English](features/frames/web/README.md) · [Русский](features/frames/web/README.ru.md) |
| `:frameswork.features.processor.common` | Frame-processing contracts, middleware chains, processor services, and JVM buffered-image saving. | `space.kscience:frameswork.features.processor.common` | [English](features/processor/common/README.md) · [Русский](features/processor/common/README.ru.md) |
| `:frameswork.features.processor.server` | Configured server-side processor pipelines and JVM image-cropping middleware. | `space.kscience:frameswork.features.processor.server` | [English](features/processor/server/README.md) · [Русский](features/processor/server/README.ru.md) |
| `:frameswork.features.processor.web` | JavaScript startup bundle for the common frame-processing APIs and serializers. | `space.kscience:frameswork.features.processor.web` | [English](features/processor/web/README.md) · [Русский](features/processor/web/README.ru.md) |
| `:frameswork.features.panel.common` | Shared panel layout contract, serializable models, and client/server path constants. | `space.kscience:frameswork.features.panel.common` | [English](features/panel/common/README.md) · [Русский](features/panel/common/README.ru.md) |
| `:frameswork.features.panel.server` | In-memory and persistent panel implementations plus the panel HTTP API. | `space.kscience:frameswork.features.panel.server` | [English](features/panel/server/README.md) · [Русский](features/panel/server/README.ru.md) |
| `:frameswork.features.panel.web` | Ktor HTTP client implementation of the shared panel contract. | `space.kscience:frameswork.features.panel.web` | [English](features/panel/web/README.md) · [Русский](features/panel/web/README.ru.md) |
| `:frameswork.features.ui.sample` | Minimal serializable navigation feature and sample Compose Web view. | `space.kscience:frameswork.features.ui.sample` | [English](features/ui/sample/README.md) · [Русский](features/ui/sample/README.ru.md) |
| `:frameswork.features.ui.panel` | Compose Web panel viewer/editor with navigation-backed child views. | `space.kscience:frameswork.features.ui.panel` | [English](features/ui/panel/README.md) · [Русский](features/ui/panel/README.ru.md) |
| `:frameswork.features.ui.panel.frames.common` | Shared camera-panel metadata and streaming contracts plus transport path constants. | `space.kscience:frameswork.features.ui.panel.frames.common` | [English](features/ui/panel/frames/common/README.md) · [Русский](features/ui/panel/frames/common/README.ru.md) |
| `:frameswork.features.ui.panel.frames.server` | Server implementation of the camera-panel contract with HTTP metadata and WebSocket frame routes. | `space.kscience:frameswork.features.ui.panel.frames.server` | [English](features/ui/panel/frames/server/README.md) · [Русский](features/ui/panel/frames/server/README.ru.md) |
| `:frameswork.features.ui.panel.frames.web` | Browser camera-panel transport, caching, view models, Compose UI, and Plotly facades. | `space.kscience:frameswork.features.ui.panel.frames.web` | [English](features/ui/panel/frames/web/README.md) · [Русский](features/ui/panel/frames/web/README.ru.md) |

## Application and aggregation modules

| Gradle module | Main purpose | Maven module | Documentation |
| --- | --- | --- | --- |
| `:frameswork.client` | Kotlin/JS browser bootstrap that assembles the default UI plugins, initializes navigation, and mounts Compose into the `content` element. | `space.kscience:frameswork.client` | [English](client/README.md) · [Русский](client/README.ru.md) |
| `:frameswork.server` | Published JVM dependency bundle for the server-side features. It currently has no local `main` function or Gradle `run` task. | `space.kscience:frameswork.server` | [English](server/README.md) · [Русский](server/README.ru.md) |

## Build and publication

JDK 17 or newer is required. From the repository root, build all modules with:

```bash
./gradlew build
```

To list every Maven-publishable Gradle module:

```bash
./gradlew getPublishableModules
```

Use a module's root multiplatform coordinate in dependencies and let Gradle resolve its target
variant. For example:

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.frames.common:<version>")
}
```

Loading a Maven dependency does not run its startup plugin or install its Koin definitions. Each
module README lists the required plugin, registered types and qualifiers, direct-construction
alternatives, and—in server modules—the relative routes contributed to Ktor.

The `server` module is currently an aggregation library rather than a runnable application. See its
[module documentation](server/README.md#build-publish-and-run) for the verified build command and
instructions for creating a launcher application. Its Docker files expect distribution archives
that the current Gradle configuration does not produce.

## Local RTSP source for development

1. Download a MediaMTX release from <https://github.com/bluenviron/mediamtx/releases>.
2. Start `mediamtx` from the extracted directory.
3. Publish a looping video with FFmpeg, replacing `video.mp4` with the source path:

```bash
ffmpeg -re -stream_loop -1 -i "video.mp4" -c copy -f rtsp rtsp://localhost:8554/mystream
```
