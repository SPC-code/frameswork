# Panel web client

Gradle module: `:frameswork.features.panel.web`  
Maven module: `space.kscience:frameswork.features.panel.web`

This Kotlin Multiplatform module provides an HTTP-backed implementation of the shared
[`PanelFeature`](../common/src/commonMain/kotlin/PanelFeature.kt) contract for web clients. It uses a
Ktor `HttpClient` to read the current and default panel layouts and to submit a replacement layout.

Russian documentation: [README.ru.md](README.ru.md).

## Dependency

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.panel.web:<version>")
}
```

The module exposes the panel-common and common-web APIs transitively.

## Main class and behavior

[`KtorPanelFeature`](src/commonMain/kotlin/KtorPanelFeature.kt) implements `PanelFeature` with three
HTTP requests:

- `getPanelConfig()` reads the current layout.
- `getDefaultConfig()` reads the default layout.
- `setPanelConfig(config)` submits a complete replacement and returns the Boolean response from the
  server.

For both reads, a response whose body is exactly the JSON literal `null` becomes Kotlin `null`.
Every other response is decoded as `PanelInfo`. The write serializes `PanelInfo` as the request body
and decodes the response as `Boolean`. The class does not catch transport, HTTP-client, or
serialization failures.

The supplied `HttpClient` therefore needs an appropriate base URL, JSON content negotiation, and
JSON as the request content type. The common-web `SerializationConfigurator` and
`DefaultUrlHttpClientConfigurator` provide those pieces when the common-web plugin is loaded.

## Koin dependency injection

The common [`Plugin`](src/commonMain/kotlin/Plugin.kt) registers exactly these unqualified singleton
definitions:

```kotlin
single { KtorPanelFeature(get()) }
single<PanelFeature> { get<KtorPanelFeature>() }
```

Consequently, Koin must already contain an **unqualified** `HttpClient`; no named or other qualifier
is used when the constructor dependency is resolved. The plugin creates no `HttpClient` itself.
After the plugin's `setupDI` has been loaded, retrieve either type without a qualifier:

```kotlin
val ktorPanelFeature: KtorPanelFeature = koin.get()
val panelFeature: PanelFeature = koin.get()

check(panelFeature === ktorPanelFeature)
```

The `PanelFeature` definition resolves the `KtorPanelFeature` singleton, so both lookups return the
same object. Loading [`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) delegates setup and startup to the
panel-common plugin and installs these panel-web definitions, but it still does not install the
common-web `HttpClient` definition. Load the common-web startup plugin as part of the application,
or provide a configured client yourself:

```kotlin
val dependencies = module {
    single<HttpClient> { configuredHttpClient }
}
```

If dependency injection is not wanted, construct the implementation directly; this bypasses all
Koin registrations and qualifiers:

```kotlin
val panelFeature: PanelFeature = KtorPanelFeature(configuredHttpClient)
```

Do not load another unqualified `PanelFeature` implementation into the same Koin scope unless the
application deliberately defines how duplicate definitions are handled.

## Relative HTTP paths

All requests use path constants from the panel-common module. The strings below are relative: they
have no scheme, host, or leading slash and are resolved by the configured `HttpClient`.

| Method | Relative path | Client operation | Purpose and response |
| --- | --- | --- | --- |
| `GET` | `panel/get` | `getPanelConfig()` | Read the current `PanelInfo`, or the literal `null` when absent. |
| `GET` | `panel/default` | `getDefaultConfig()` | Read the default `PanelInfo`, or the literal `null` when absent. |
| `POST` | `panel/set` | `setPanelConfig(config)` | Send a `PanelInfo` body and decode a Boolean result. |

This client module installs no server routing configurator and exposes no server endpoints of its
own. It configures no WebSocket paths.
