# Common Web

[Russian version](README.ru.md)

Maven module: `space.kscience:frameswork.features.common.web`

This Kotlin Multiplatform module provides the shared web-client foundation for Frameswork. It
assembles a Ktor `HttpClient`, contributes JSON and WebSocket configuration, supplies navigation
configuration primitives, and contains the Compose Web FlotView operator-panel UI kit.

## Add and start the module

Use the published artifact from a JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.common.web:<version>")
        }
    }
}
```

Add `space.kscience.frameswork.features.common.web.JSPlugin` to the `StartLauncherPlugin` plugin
list. Its DI setup includes the common web registrations and a Ktor JavaScript engine. The module's
common plugin depends on the shared `Json` instance supplied by
`frameswork.features.common.common`, so that common plugin must be part of the same startup graph.

## Dependency injection

The startup plugins register the following Koin definitions:

| Type | Qualifier | Obtain from Koin | Purpose |
| --- | --- | --- | --- |
| `HttpClient` | none | `koin.get<HttpClient>()` | Shared, fully configured client. |
| `HttpClientEngineFactory<*>` | none | `koin.get<HttpClientEngineFactory<*>>()` | JavaScript engine (`Js`) supplied by `JSPlugin`; the common plugin can also use another engine registered by a host. |
| `HttpClientConfigurator` | random qualifier per contribution | `koin.getAllDistinct<HttpClientConfigurator>()` | The serialization, default-URL, and WebSocket configuration contributions. |
| `NavigationNodeFactory<ViewConfig>` | random qualifier | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | Typed factory that creates an empty navigation node for `EmptyConfig`; it is intended to be aggregated with factories from feature modules. |

`getAllDistinct` is the extension from `dev.inmo.micro_utils.koin`. Random qualifiers are generated
by `singleWithRandomQualifier`; they have no stable name. Consequently, the concrete
`SerializationConfigurator`, `DefaultUrlHttpClientConfigurator`, and `WebSocketsConfigurator`
classes are registered only as `HttpClientConfigurator` contributions and cannot be retrieved with
`koin.get<SerializationConfigurator>()` (and similarly for the other concrete classes).

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import io.ktor.client.HttpClient
import space.kscience.frameswork.features.common.web.configurators.HttpClientConfigurator

val client: HttpClient = koin.get()
val contributions: List<HttpClientConfigurator> = koin.getAllDistinct()
```

Resolve `HttpClient` only after all feature modules have installed their DI definitions: the client
collects all `HttpClientConfigurator` contributions when its singleton is first created.

## HTTP and WebSocket configuration

- [`HttpClientConfigurator`](src/commonMain/kotlin/configurators/HttpClientConfigurator.kt) is the
  contribution interface used while building the shared client.
- [`SerializationConfigurator`](src/commonMain/kotlin/configurators/SerializationConfigurator.kt)
  installs Ktor content negotiation with the DI-managed `Json` instance and selects
  `application/json` as the default request content type.
- [`DefaultUrlHttpClientConfigurator`](src/commonMain/kotlin/configurators/DefaultUrlHttpClientConfigurator.kt)
  resolves requests against a URL obtained immediately before each request. The default provider is
  [`getUrl`](src/commonMain/kotlin/UrlGetter.kt), whose JavaScript implementation returns
  `window.location.href`. It appends the request path to the base path, retains query parameters,
  assumes `http` when the base has no scheme, and preserves the appropriate `ws`/`wss` request
  scheme for an HTTP/HTTPS base.
- [`WebSocketsConfigurator`](src/commonMain/kotlin/configurators/WebSocketsConfigurator.kt) installs
  Ktor WebSockets with `KotlinxWebsocketSerializationConverter` backed by the same DI-managed
  `Json` instance.

This module declares **no fixed HTTP endpoints or WebSocket paths**. Callers provide their own
relative paths to the DI-managed client; the default-URL configurator combines those paths with the
runtime base URL. In particular, it does not insert an `/api` segment.

The configurators can also be constructed directly when a standalone client needs a different
composition:

```kotlin
val serialization = SerializationConfigurator(json)
val defaultUrl = DefaultUrlHttpClientConfigurator(
    urlGetter = { "https://example.test/application/" },
    useDefaultUrlPrefix = true,
)
val webSockets = WebSocketsConfigurator(json)
```

## Navigation models

- [`ViewConfig`](src/commonMain/kotlin/models/ViewConfig.kt) is the marker type shared by
  navigation-view configuration models.
- [`EmptyConfig`](src/commonMain/kotlin/models/EmptyConfig.kt) is a serializable configuration for a
  view with no parameters. Construct it directly as `EmptyConfig()`; Koin contains a
  `NavigationNodeFactory<ViewConfig>` that knows how to turn it into `NavigationNode.Empty`.

## FlotView Compose Web UI

The UI API is stateless and is not registered in Koin. Construct models directly and call the
composables from Compose Web:

- [`FlotViewComponents.kt`](src/jsMain/kotlin/ui/FlotViewComponents.kt) contains reusable cards,
  metrics, tabs, mode controls, setpoint steppers, send states, loading/error states, and their
  models (`FlotViewMetric`, `FlotViewTab`, `FlotViewSetpoint`, `FlotViewState`, and
  `FlotViewSendState`).
- [`FlotViewDashboard.kt`](src/jsMain/kotlin/ui/FlotViewDashboard.kt) contains the complete operator
  dashboard model, event callbacks, localized strings, video/recommendation/session states, and
  composables. `FlotViewDashboard(model, actions, strings, videoContent)` renders caller-owned live
  state. The parameterless overload uses `FlotViewDashboardModel.referencePreview()` and is only a
  static design preview.
- [`FlotViewStyleSheet.kt`](src/jsMain/kotlin/ui/FlotViewStyleSheet.kt) exposes ISA-101-inspired
  colours and dimensions through `FlotViewTokens`, semantic `FlotViewTone` values, and the shared
  `FlotViewStyleSheet`. Render `InstallFlotViewStyles()` in the document, or use the application's
  `StyleSheetsAggregator` integration.

```kotlin
FlotViewDashboard(
    model = dashboardModel,
    actions = FlotViewDashboardActions(
        onModeSelected = ::selectMode,
        onSend = ::sendPendingSetpoints,
    ),
    videoContent = { ProcessVideo() },
)
```

## Other utilities

- [`CommonResources`](src/commonMain/kotlin/CommonResources.kt) provides Russian-first localized
  strings with English translations for common UI actions. Access it directly, for example
  `CommonResources.cancel`.
- [`DefaultDateFormat`](src/commonMain/kotlin/DefaultDateFormat.kt) is the shared
  `dd/MM/YYYY, HH:mm:ss` formatter.
- [`MergeUrlBuilders.kt`](src/commonMain/kotlin/utils/MergeUrlBuilders.kt) contains `URLBuilder`
  merge/copy helpers used by default URL resolution.
- [`HEXAColor.cssRGBA`](src/commonMain/kotlin/utils/HEXAColorToCSSColorValue.kt) adapts a
  `HEXAColor` RGBA value for Compose Web CSS. These utilities are direct APIs, not Koin services.
