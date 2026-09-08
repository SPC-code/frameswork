# Architecture

## Overview

`frameswork-srv` is a **Kotlin Multiplatform (KMP)** full-stack web application. The server runs on the JVM, and the client compiles to JavaScript. Both share common code across platform targets. The system is structured around a **plugin-based startup architecture** using the `microutils.startup` library.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.x (Multiplatform) |
| Build | Gradle with KMP plugin |
| Server framework | Ktor 3.x (Netty engine) |
| Database ORM | Exposed 1.x |
| Database | PostgreSQL |
| Frontend UI | Jetbrains Compose for Web |
| Navigation | dev.inmo:navigation.mvvm |
| DI | Koin 4.x |
| Serialization | Kotlinx Serialization JSON |
| Logging | KSLog + Logback |
| Code generation | KSP |

---

## Module Structure

```
frameswork-srv/
├── client/                          # JS target — browser SPA
├── server/                          # JVM target — HTTP server
└── features/
    ├── common/
    │   ├── common/                  # Shared multiplatform core (JVM + JS + Android)
    │   ├── server/                  # JVM-only server infrastructure
    │   └── web/                     # JS-only web UI base
    └── ui/
        └── sample/                  # Sample feature module (JS)
```

Gradle module names follow the pattern `:frameswork.<path>`, e.g.:

- `:frameswork.features.common.common`
- `:frameswork.features.common.server`
- `:frameswork.features.common.web`
- `:frameswork.features.ui.sample`
- `:frameswork.client`
- `:frameswork.server`

But in `settings.gradle`, they must appear with the `:` prefix:

- `features:common:common`
- `features:common:server`
- `features:common:web`

---

## Plugin System

The entire application (both client and server) is initialized via the `StartLauncherPlugin` from `microutils.startup`. Each plugin implements the `StartPlugin` interface with two lifecycle phases:

1. **`setupDI(config)`** — registers dependencies into the Koin DI container.
2. **`startPlugin(koin)`** — async startup work using the already-built DI container.

Plugins are composable: a parent plugin typically delegates to child plugins in order. The server reads its plugin list from `config.json`; the client hardcodes the chain in `Main.kt`.

---

## Server Startup Flow

```
MainKt (microutils entry point)
  └─ reads config.json
  └─ JVMPlugin
       ├─ CommonCommonPlugin    — JSON serializer, CoroutineScope
       ├─ CommonServerPlugin
       │    ├─ connect to PostgreSQL (Exposed)
       │    ├─ initialize versioning tables
       │    └─ start Ktor (Netty)
       │         ├─ WebSockets
       │         ├─ ContentNegotiation (JSON)
       │         ├─ GZip compression
       │         ├─ Authentication
       │         ├─ Sessions
       │         ├─ Static file serving
       │         └─ Call logging
       └─ <feature plugins>     — register routes, tables, etc.
```

Routes are not hardcoded. Each feature plugin can inject `ApplicationRoutingConfigurator` instances into Koin in server jvm module; the server collects all of them and installs their routes into Ktor's routing tree.

---

## Client Startup Flow

```
Main.kt (JS entry point)
  └─ ClientJSPlugin
       ├─ CommonCommonPlugin    — JSON serializer, CoroutineScope
       ├─ CommonWebPlugin       — registers navigation node factories
       ├─ SampleUIPlugin        — registers SampleView factory
       └─ startPlugin()
            ├─ build navigation system (NavigationChain<ViewConfig>)
            └─ render Compose tree into HTML element "content"
```

---

## Navigation / MVVM (Client)

The client uses a navigation stack from `dev.inmo.navigation.mvvm`.

Small note: `C` in the table is the `ViewConfig` inheritor classname

| Type | Role                                                                           |
|---|--------------------------------------------------------------------------------|
| `ViewConfig` | Serializable screen state / identifier                                         |
| `NavigationChain<ViewConfig>` | Navigation stack                                                               |
| `NavigationNode<C, ViewConfig>` | One screen instance                                                            |
| `ViewModel<C>` | Business logic, exposes StateFlows |
| `ComposeView<C, ViewConfig, VM>` | Compose UI, observes ViewModel                                                 |

### Koin registration

Each MVVM piece is registered in a specific way inside a plugin's `setupDI(config)`.

**1. `ViewConfig` — polymorphic serializer (common `Plugin.kt`)**

```kotlin
singleWithRandomQualifier {
    SerializersModule {
        polymorphic(Any::class, SampleViewConfig::class, SampleViewConfig.serializer())
        polymorphic(ViewConfig::class, SampleViewConfig::class, SampleViewConfig.serializer())
    }
}
```

The `singleWithRandomQualifier` helper (from microutils) registers the binding under a generated qualifier so that multiple `SerializersModule` instances can coexist in the DI container. The common infrastructure collects all of them and merges them into one combined module.

**2. `ViewModel` — Koin `factory` (common `Plugin.kt`)**

```kotlin
factory { SampleViewModel(it.get(), get()) }
```

`factory` means a new instance is created on each injection. The first argument (`it.get()`) resolves the `NavigationNode` that is passed as a parameter at injection time; `get()` resolves `SampleModel` from the container.

**3. `NavigationNodeFactory` — view factory (JS `JSPlugin.kt`)**

```kotlin
singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
    NavigationNodeFactory.Typed<SampleViewConfig, ViewConfig> { chain, config ->
        SampleView(chain, config)
    }
}
```

Again `singleWithRandomQualifier` is used so multiple factories (one per screen) can be registered for the same `NavigationNodeFactory<ViewConfig>` type. The navigation system retrieves all of them with `getAll<NavigationNodeFactory<ViewConfig>>()` and picks the one whose type matches the current `ViewConfig` at runtime.

**4. `ComposeView` — no explicit DI registration**

`ComposeView` subclasses are instantiated directly inside the `NavigationNodeFactory` lambda (see step 3). The view then injects its own `ViewModel` lazily:

```kotlin
override val viewModel: SampleViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    parametersOf(this@SampleView)
}
```

`parametersOf(this)` passes the view itself as a parameter, which is forwarded to the `factory { SampleViewModel(it.get(), get()) }` lambda as `it`.

### Adding a new screen

1. Create a `ViewConfig` subclass (serializable).
2. Create a `ViewModel` (extend `ViewModel<C>`) and a `ComposeView` (extend `ComposeView<C, ViewConfig, VM>`).
3. In the common `Plugin.kt`, register the `SerializersModule` and the `factory` for the `ViewModel`.
4. In the JS `JSPlugin.kt`, register a `NavigationNodeFactory.Typed` with `singleWithRandomQualifier`.

---

## Database

- **PostgreSQL** accessed via **Exposed** ORM (both SQL DSL and DAO).
- Connection config is read from `config.json` (`database.url`, `database.username`, `database.password`).
- A `VersionsRepo` (backed by a `tables_versions` table) tracks schema versions for migrations.
- For local development, a `docker-compose.yml` in `server/` starts a PostgreSQL container on port 8201.

---

## Configuration

Server configuration is a JSON file (`config.json`) passed as the first CLI argument. Sample of configuration file
can be found in `server/sample.config.json`. It can be used as is for launching server.

| Field | Purpose |
|---|---|
| `host` / `port` | Ktor bind address |
| `publicHost` | Public hostname (used for link generation) |
| `wss` | Use `wss://` scheme for WebSocket links |
| `staticFolders` | URL path → local directory mappings for static files |
| `database` | PostgreSQL connection details |
| `plugins` | Fully-qualified `StartPlugin` class names, loaded by reflection |

`DEBUG=true` environment variable switches Ktor into development mode and raises log verbosity.

---

## Build & Deployment

### Building

```bash
./gradlew build           # builds everything (server dist + client JS bundle)
./gradlew :frameswork.client:jsBrowserDevelopmentWebpack   # client only (dev)
./gradlew :frameswork.server:installDist                    # server only
```

The server build depends on the client web build; the resulting JS bundle is placed under `client/build/dist/js/` and served as static files by the Ktor server.

### Running locally

```bash
# start dev database
cd server && docker compose up
```

Will result in hanging and working database. For stopping database without dropping data just stop the process (with `ctrl+c`, for example)

```bash
# run server (after build)
./gradlew run --args="sample.config.json"
```

### Docker

The `server/Dockerfile` packages the JVM distribution:

```
ENTRYPOINT ["/frameswork.server/bin/frameswork.server", "/config.json"]
```

Mount points:
- `/config.json` — runtime configuration
- `/db` — database data directory
- `/static` — additional static assets

### CI

GitHub Actions (`.github/workflows/build.yml`) runs `./gradlew build` on every push.

---

## Adding a New Feature

1. **Create a feature module** under `features/` using the Gradle templates in `gradle/templates/`.
2. **Register it** in `settings.gradle`.
3. **Implement plugins:**
   - `Plugin.kt` (common) — serializers, DI bindings shared by all targets.
   - `JVMPlugin.kt` — server-side: Exposed tables, Ktor route configurators.
   - `JSPlugin.kt` — client-side: navigation node factories, ViewModels.
4. **Register ViewConfig** in the polymorphic serialization module (in the common plugin's `SerializersModule`).
5. **Add the JVM plugin** to `config.json` `plugins` list.
6. **Add the JS plugin** to the client `Main.kt` plugin chain.
