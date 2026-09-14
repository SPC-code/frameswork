# Frameswork client

[Russian version](README.ru.md)

The `:frameswork.client` Gradle module is the Kotlin/JS browser bootstrap for the Frameswork UI. It
assembles the default feature plugins, creates the startup configuration after the browser has
loaded, initializes navigation, and renders Compose for Web content into a DOM element named
`content`.

- Gradle module: `:frameswork.client`
- Kotlin package: `space.kscience.frameswork.client`
- Kotlin Multiplatform Maven coordinate: `space.kscience:frameswork.client:<version>`
- JavaScript target Maven coordinate: `space.kscience:frameswork.client-js:<version>`

The Maven coordinates follow the module's generated publications; the project version is defined
in the root `gradle.properties` file.

## Browser bootstrap

[`initClient`](src/jsMain/kotlin/Main.kt) is the module's public bootstrap function. It registers a
browser `load` listener and then calls `StartLauncherPlugin.start` in a coroutine with this
configured plugin sequence:

1. Every plugin supplied through `plugins`, in caller-provided order.
2. Every plugin supplied through `defaultPlugins`, in the order shown below.

The launcher performs DI setup by iterating over this sequence, but launches plugin startup
callbacks asynchronously and waits for all of them. Sequence position does not mean that one
plugin's startup callback completes before the next callback.

The default list is:

1. `ClientJSPlugin`
2. `features.common.common.JSPlugin`
3. `features.common.web.JSPlugin`
4. `features.frames.web.JSPlugin`
5. `features.panel.web.JSPlugin`
6. `features.processor.web.JSPlugin`
7. `features.ui.sample.JSPlugin`
8. `features.ui.panel.JSPlugin`
9. `features.ui.panel.frames.JSPlugin`

Call `initClient` directly from a Kotlin/JS executable entry point while the document is still
loading:

```kotlin
import space.kscience.frameswork.client.initClient

fun main() {
    initClient(
        plugins = listOf(MyApplicationPlugin),
    )
}
```

The host HTML must contain the Compose mount point:

```html
<div id="content"></div>
```

`initClient` is not a `main` function and is not registered in Koin. It must be invoked directly.
Supplying `defaultPlugins` replaces the complete default list, so a replacement list must include
`ClientJSPlugin` and every feature the application needs.

## Plugins and dependency injection

[`ClientPlugin`](src/commonMain/kotlin/ClientPlugin.kt) and
[`ClientJSPlugin`](src/jsMain/kotlin/ClientJSPlugin.kt) are Kotlin `object` declarations implementing
`StartPlugin`. They are not Koin bindings and have no Koin qualifier: refer to the singleton objects
directly or put them in the launcher plugin list. `ClientJSPlugin` delegates its common lifecycle to
`ClientPlugin`.

`ClientJSPlugin.setupDI` contributes exactly two unqualified singleton bindings:

| Binding | Registration | Retrieval |
|---|---|---|
| Compose drawing-block setter, `(@Composable () -> Unit) -> Unit` | Unqualified `single`; assigning a block updates `ClientJSPlugin.currentDrawingBlock` | `koin.get<(@Composable () -> Unit) -> Unit>()` |
| `NavigationConfigsRepo<ViewConfig>` | Unqualified `single`; implemented by `NavigationConfigsRepo.InMemory<ViewConfig>` | `koin.get<NavigationConfigsRepo<ViewConfig>>()` |

There are no named or random qualifiers on these two bindings. `ClientPlugin.setupDI` itself adds no
bindings. `ClientJSPlugin.currentDrawingBlock` is a property of the plugin singleton, not a DI
binding, and is therefore accessed as `ClientJSPlugin.currentDrawingBlock` when direct access is
needed.

During startup, `ClientPlugin` retrieves the drawing-block setter and navigation repository from
Koin. It obtains the navigation-node factory assembled from the feature plugins, starts navigation
with `EmptyConfig`, and injects an initial `PanelViewConfig`. `ClientJSPlugin` then adds
`FlotViewStyleSheet`, mounts Compose in `content`, and renders the current drawing block.

## Module dependencies

The module exposes these Frameswork modules as `commonMain` API dependencies:

- `:frameswork.features.common.web`
- `:frameswork.features.panel.web`
- `:frameswork.features.processor.web`
- `:frameswork.features.ui.sample`
- `:frameswork.features.ui.panel`
- `:frameswork.features.ui.panel.frames.web`

It also exposes the startup launcher, navigation MVVM, and Compose Runtime libraries. Compose Web
Core is a `jsMain` implementation dependency. The shared Gradle templates also add kotlinx
serialization as an API dependency and the Kotlin standard library as an implementation dependency.
The default bootstrap list additionally names the common-core and Frames-web plugins, which are
available through the feature dependency graph rather than direct declarations in
`client/build.gradle`.

## Runtime paths and endpoints

This client module defines no HTTP endpoint, Ktor route, or server routing configurator.

Its only hard-coded runtime target is the DOM element id `content`. The following browser resources
are packaged under relative paths; this module does not generate host HTML or insert tags for them:

- `css/bootstrap.min.css`
- `css/internal.css`
- `js/bootstrap.bundle.min.js`
- `js/plotly.min.js`

## Build and verification

From the repository root:

```bash
./gradlew :frameswork.client:compileKotlinJs
./gradlew :frameswork.client:jsNodeTest
./gradlew :frameswork.client:build
```

There are currently no Kotlin test sources in this module. The module also does not declare a
Kotlin/JS executable binary or provide a host HTML page, so it has no standalone browser run or
Webpack task. A consuming application supplies the executable entry point and calls `initClient`.
