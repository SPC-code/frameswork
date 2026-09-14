# Frameswork common server

[Русская версия](README.ru.md)

Gradle module: `:frameswork.features.common.server`  
Maven multiplatform module: `space.kscience:frameswork.features.common.server`  
Maven JVM publication: `space.kscience:frameswork.features.common.server-jvm`

This module provides the JVM server foundation shared by Frameswork features. It creates an embedded
Netty server, installs the common Ktor infrastructure, collects authentication and routing extensions
from Koin, exposes configured static directories, and provides a PostgreSQL-backed global key-value
repository with an in-memory full cache.

The startup entry point is `space.kscience.frameswork.features.common.server.JVMPlugin`. Its DI setup
also invokes the common feature setup, so the unqualified `Json` and `CoroutineScope` used below come
from `frameswork.features.common.common`.

[`Plugin`](src/commonMain/kotlin/Plugin.kt) is the common multiplatform startup entry point and currently
adds no bindings or startup work. [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) supplies the JVM
bindings described here and starts the embedded server.

## Configuration models

Both configuration objects are decoded from the same startup JSON object. Unknown keys are accepted by
the shared `Json` instance, while the two exact, case-sensitive Koin qualifier names keep the models
separate.

| Model | Purpose | DI access |
| --- | --- | --- |
| [`Config`](src/jvmMain/kotlin/models/Config.kt) | Database settings, public addressing, and static-directory mappings. The JSON key for `databaseConfig` is `database`. | `koin.get<Config>(StringQualifier("ConfigJsonQualifier"))` |
| [`KtorConfig`](src/jvmMain/kotlin/models/KtorConfig.kt) | The actual Netty listening `host` and `port`, plus the optional `rootRoute` applied to contributed routes. | `koin.get<KtorConfig>(StringQualifier("KtorConfigJsonQualifier"))` |
| [`DatabaseConfig`](src/jvmMain/kotlin/models/DatabaseConfig.kt) | JDBC URL, PostgreSQL driver, credentials, and the resulting Exposed `Database` handle. | Nested in the qualified `Config`; its `Database` is also available unqualified as described below. |

All three data classes have public constructors and can be created directly when startup DI is not
needed. `DatabaseConfig` creates its `Database` property from its connection settings. The same source
file also provides `defaultDatabaseParamsName` (`"defaultDatabase"`) and the nullable
`Map<String, Any>.database` accessor for maps that store a `DatabaseConfig` under that key.

The server listener reads `host` and `port` from `KtorConfig`, not from `Config`. The `wss` fields are
configuration data only in this module; they do not install TLS. On the JVM, `isInDebugMode` is `true`
only when the `DEBUG` environment variable equals `true`, ignoring case. Debug mode enables Ktor
development mode and changes call logging to `TRACE`; otherwise call logging uses `WARN`.

## DI bindings

The following definitions are Koin singletons after `JVMPlugin.setupDI` has run:

| Binding | How to obtain it | Notes |
| --- | --- | --- |
| `Config` | `koin.get<Config>(StringQualifier("ConfigJsonQualifier"))` | Decoded from startup JSON. |
| `KtorConfig` | `koin.get<KtorConfig>(StringQualifier("KtorConfigJsonQualifier"))` | Decoded from the same startup JSON. |
| `Database` | `koin.get<Database>()` | The handle from the qualified `Config.databaseConfig`. |
| `VersionsRepo<Database>` | `koin.get<VersionsRepo<Database>>()` | Stores table versions in `tables_versions`. |
| `InternalApplicationRoutingConfigurator` | `koin.get<InternalApplicationRoutingConfigurator>()` | Unqualified concrete binding; run separately after the generic configurators. |
| `EmbeddedServer<*, *>` | `koin.get<EmbeddedServer<*, *>>()` | Configured Netty server. Starting it blocks in the startup plugin. |
| `ExposedGlobalKVRepo` | `koin.get<ExposedGlobalKVRepo>()` | Persistent repository implementation. |
| `FullyCachedGlobalKVRepo` | `koin.get<FullyCachedGlobalKVRepo>()` | Full in-memory cache over `ExposedGlobalKVRepo`. |
| `GlobalKVRepo` | `koin.get<GlobalKVRepo>()` | Resolves to the same cached repository. This is the preferred application-facing binding. |
| All generic Ktor configurators | `koin.getAllDistinct<KtorApplicationConfigurator>()` | Each is registered as `KtorApplicationConfigurator` with a generated random qualifier. |
| All route contributions | `koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()` | Includes this module's static-files contribution and contributions from feature modules. |

Relevant imports for the qualified and collection lookups are:

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import org.koin.core.qualifier.StringQualifier
```

The random qualifiers are deliberately not stable lookup names. Consequently,
`ApplicationAuthenticationConfigurator`, `ContentNegotiationKtorApplicationConfigurator`, and
`GZipConfigurator` are obtained from DI through the `KtorApplicationConfigurator` collection rather
than by their concrete types. `StatusPagesConfigurator`, `ApplicationCachingHeadersConfigurator`, and
`ApplicationSessionsConfigurator` from MicroUtils are registered in the same collection.

`WebSocketsConfiguration` is not a Koin definition. The server constructs it directly with the
unqualified `Json` and applies it before all DI-managed configurators. The local configurator classes
also have public constructors for explicit use:

```kotlin
val authentication = ApplicationAuthenticationConfigurator(authenticationElements)
val contentNegotiation = ContentNegotiationKtorApplicationConfigurator(json)
val gzip = GZipConfigurator()
val routing = InternalApplicationRoutingConfigurator(routeElements, rootPath = "api")
val webSockets = WebSocketsConfiguration(json)
```

Authentication extensions are collected as
`ApplicationAuthenticationConfigurator.Element` instances. This module does not register an element;
feature modules may contribute several with `singleWithRandomQualifier`.

## Repositories

[`GlobalKVRepo`](src/commonMain/kotlin/repos/GlobalKVRepo.kt) is a string-to-string application-wide
key-value repository. [`ExposedGlobalKVRepo`](src/jvmMain/kotlin/repos/ExposedGlobalKVRepo.kt) persists
it in the `globals` table, using text columns named `key` and `value`.

[`FullyCachedGlobalKVRepo`](src/commonMain/kotlin/repos/FullyCachedGlobalKVRepo.kt) wraps that persistent
repository in `FullKeyValueCacheRepo` with a `MapKeyValueRepo` cache. The supplied application
`CoroutineScope` performs initial cache population. Writes go through the persistent repository and
update the cache. Application code should normally request `GlobalKVRepo`, while maintenance code that
specifically needs the persistent layer can request `ExposedGlobalKVRepo`.

## Configurators and endpoints

Every path below is **relative**: it has no scheme, host, or leading slash. Values read from
configuration may contain slashes, but the notation here is normalized. `rootRoute` is optional.

| Configurator or route installer | HTTP/WebSocket endpoints installed | Purpose |
| --- | --- | --- |
| [`ApplicationAuthenticationConfigurator`](src/jvmMain/kotlin/configurators/ApplicationAuthenticationConfigurator.kt) | None. | Installs authentication providers contributed as `ApplicationAuthenticationConfigurator.Element`; routes choose whether to use them. |
| [`ContentNegotiationKtorApplicationConfigurator`](src/jvmMain/kotlin/configurators/ContentNegotiationKtorApplicationConfigurator.kt) | None. | Installs JSON request/response conversion with the shared `Json`. |
| [`GZipConfigurator`](src/jvmMain/kotlin/configurators/GZipConfigurator.kt) | None. | Enables gzip for eligible responses of at least 1,024 bytes. |
| [`WebSocketsConfiguration`](src/jvmMain/kotlin/configurators/WebSocketsConfiguration.kt) | No WebSocket endpoint. | Installs WebSocket protocol support and JSON frame conversion; feature route elements define actual sockets. |
| [`InternalApplicationRoutingConfigurator`](src/jvmMain/kotlin/configurators/InternalApplicationRoutingConfigurator.kt) | No fixed endpoint of its own. It installs whatever each `ApplicationRoutingConfigurator.Element` declares, below `rootRoute` when configured. | Aggregates all DI route contributions into Ktor routing. |
| Static-files `ApplicationRoutingConfigurator.Element` in `JVMPlugin` | `GET [rootRoute/]<static-path>/{file...}` for every `Config.staticFolders` entry. If the path represents the route root, the relative form is `[rootRoute/]{file...}`. | Serves files recursively from the mapped local directory. Directory requests use `index.html`; missing content returns `404`. No endpoint is installed when `staticFolders` is empty. |
| MicroUtils `StatusPagesConfigurator` | None. | Applies contributed status-page handlers. |
| MicroUtils `ApplicationCachingHeadersConfigurator` | None. | Applies contributed cache-header rules. |
| MicroUtils `ApplicationSessionsConfigurator` | None. | Applies contributed session configuration. |

Thus, this module defines no fixed REST endpoint and no WebSocket endpoint. Its only concrete HTTP
route is the configuration-driven static-files `GET`; all other application endpoints come from route
elements supplied by feature modules. With `rootRoute = "api"` and a mapping
`"assets" -> "./public"`, for example, a file is exposed at the relative path
`api/assets/{file...}`.
