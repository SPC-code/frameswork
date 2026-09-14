# Panel server

[Русская версия](README.ru.md)

- Gradle module: `:frameswork.features.panel.server`
- Maven multiplatform module: `space.kscience:frameswork.features.panel.server`
- Maven JVM publication: `space.kscience:frameswork.features.panel.server-jvm`

This Kotlin Multiplatform module supplies server-side implementations of the
[`PanelFeature`](../common/src/commonMain/kotlin/PanelFeature.kt) API and, on the JVM, a Ktor route
contribution for reading and updating panel layouts. It exports both the
[`panel/common`](../common/README.md) API and the
[`common/server`](../../common/server/README.md) infrastructure.

## Classes and behavior

| Declaration | Purpose |
| --- | --- |
| [`InMemoryPanelFeature`](src/commonMain/kotlin/InMemoryPanelFeature.kt) | Keeps the current `PanelInfo` inside one process. The current value starts as `null`; the separately supplied default is not used as an automatic fallback. A coroutine mutex serializes current-value reads and writes. |
| [`GlobalKVBindedPanelFeature`](src/commonMain/kotlin/GlobalKVBindedPanelFeature.kt) | Encodes the current `PanelInfo` with the shared `Json` and stores it in `GlobalKVRepo` under the fixed key `panel_config`. The default remains separate. A write is reported successful only when the value read back equals the submitted value. |
| [`PanelRoutingsConfigurator`](src/jvmMain/kotlin/configurators/PanelRoutingsConfigurator.kt) | Implements `ApplicationRoutingConfigurator.Element` and installs the three panel HTTP routes listed below. |
| [`Plugin.DefaultPanelInfoConfig`](src/commonMain/kotlin/Plugin.kt) | Serializable wrapper for the optional configured default panel. Its JSON property is named `default`. |

Both feature implementations return `null` from `getPanelConfig()` until a current value exists and
return the constructor's `defaultPanel` only from `getDefaultConfig()`. They do not copy the default
into current storage. `GlobalKVBindedPanelFeature` propagates repository and serialization failures;
malformed JSON stored under `panel_config` is not converted to `null`.

## Startup and configuration

When using the startup loader on a JVM, load
[`space.kscience.frameswork.features.panel.server.JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt). It runs
the `panel/common` JVM setup and then this module's common `Plugin` setup before registering the route
contribution. The delegated `panel/common` plugins currently add no Koin definitions.

The optional top-level `panel_info` object is decoded as `Plugin.DefaultPanelInfoConfig`. For example:

```json
{
  "panel_info": {
    "default": {
      "horizontalSlots": 3,
      "verticalSlots": 2,
      "items": []
    }
  }
}
```

When `panel_info` is absent, both feature implementations receive a `null` default and there is no
`Plugin.DefaultPanelInfoConfig` definition in Koin. Supplying `panel_info` without a valid `default`
`PanelInfo` fails when that configuration singleton is resolved.

This module does **not** run `space.kscience.frameswork.features.common.server.JVMPlugin`. A complete
server normally loads that plugin separately to provide the unqualified `Json` and `GlobalKVRepo`,
collect the routing element, optionally apply `KtorConfig.rootRoute`, and start Ktor. Merely declaring
the Maven dependency does not execute either startup plugin.

## Dependency injection

After `JVMPlugin.setupDI` runs, this module contributes these Koin singletons:

| Type | Registration and qualifier | Exact retrieval |
| --- | --- | --- |
| `Plugin.DefaultPanelInfoConfig` | Unqualified; registered only when the top-level `panel_info` key exists. | `koin.get<Plugin.DefaultPanelInfoConfig>()` or `koin.getOrNull<Plugin.DefaultPanelInfoConfig>()` |
| `InMemoryPanelFeature` | Unqualified concrete singleton; always defined. | `koin.get<InMemoryPanelFeature>()` |
| `GlobalKVBindedPanelFeature` | Unqualified concrete singleton; always defined. | `koin.get<GlobalKVBindedPanelFeature>()` |
| `PanelFeature` | Unqualified interface singleton that resolves to the same `GlobalKVBindedPanelFeature` instance. | `koin.get<PanelFeature>()` |
| `ApplicationRoutingConfigurator.Element` | One singleton backed by `PanelRoutingsConfigurator`, registered with a generated random qualifier. | `koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()`; select the element from the contributed collection. |

The generated route qualifier is intentionally unstable and has no application-facing name. The
route configurator is registered only as `ApplicationRoutingConfigurator.Element`, not under its
concrete class, so `koin.get<PanelRoutingsConfigurator>()` is not a valid lookup. Import
`dev.inmo.micro_utils.koin.getAllDistinct` for the collection retrieval.

The feature definitions require an unqualified `Json`; `GlobalKVBindedPanelFeature` additionally
requires an unqualified `GlobalKVRepo`. The normal `common/server` JVM plugin provides both. Although
the definitions are added unconditionally, Koin singletons are created when resolved; asking for
`PanelFeature` selects the repository-backed implementation and therefore requires the repository.
Ask explicitly for `InMemoryPanelFeature` when process-local storage is desired.

`Plugin` and `JVMPlugin` are startup singleton objects, not Koin definitions. All three local classes
also have public constructors and can be used without Koin:

```kotlin
val inMemory: PanelFeature = InMemoryPanelFeature(
    defaultPanel = defaultPanel,
    json = json,
)

val persistent: PanelFeature = GlobalKVBindedPanelFeature(
    globalKVRepo = globalKVRepo,
    json = json,
    defaultPanel = defaultPanel,
)

val routes = PanelRoutingsConfigurator(persistent)
```

The `json` argument is currently retained but not used by `InMemoryPanelFeature`; it is still required
by the public constructor. Two repository-backed instances that share a `GlobalKVRepo` also share the
single `panel_config` value.

## Configurator and endpoints

[`PanelRoutingsConfigurator`](src/jvmMain/kotlin/configurators/PanelRoutingsConfigurator.kt) is the
module's only configurator. It installs HTTP routes and no WebSocket endpoint. The route element itself
uses the fixed values in `PanelConstants`: `rootPanelPath = "panel"`, `getPanelSubpath = "get"`,
`getPanelDefaultSubpath = "default"`, and `setPanelSubpath = "set"`.

Every path in this table is **relative**—there is no scheme, host, or leading slash:

| Method | Relative path | Purpose and response |
| --- | --- | --- |
| `GET` | `panel/get` | Returns the current `PanelInfo`. When no current configuration exists, responds with the string `null`. |
| `GET` | `panel/default` | Returns the configured default `PanelInfo`. When no default exists, responds with the string `null`. |
| `POST` | `panel/set` | Receives a `PanelInfo`, passes it to `PanelFeature.setPanelConfig`, and returns that method's Boolean result. |

When the route element is installed by `common/server`, the optional `KtorConfig.rootRoute` prefixes
all three paths. For example, `rootRoute = "api"` makes the first relative path `api/panel/get`.
`rootRoute` is external to this module; without it, the paths are exactly those in the table.

The configurator performs no authentication or authorization itself. JSON request decoding and typed
response encoding require the server's content-negotiation setup. There are no non-routing
configurators in this module, and therefore no additional endpoint-free configurators to list.
