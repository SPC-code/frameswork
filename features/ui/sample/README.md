# Sample UI

[Russian version](README.ru.md)

Maven module: `space.kscience:frameswork.features.ui.sample`

This Kotlin Multiplatform module is a minimal Frameswork navigation feature for JavaScript. It
defines a serializable sample-view configuration, the corresponding model and view model, a
Compose Web view, localized strings, and the DI contributions that connect them to the shared
navigation and serialization infrastructure.

## Add and start the module

Add the published artifact to a JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.ui.sample:<version>")
        }
    }
}
```

Add [`space.kscience.frameswork.features.ui.sample.JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) to the
`StartLauncherPlugin` plugin list in a browser application. `JSPlugin` installs both the common
registrations from [`Plugin`](src/commonMain/kotlin/Plugin.kt) and the JavaScript navigation-view
factory. Run it in the same startup graph as the common Frameswork plugins that provide the shared
`Json`, Koin, and web navigation infrastructure. Installing `Plugin` separately alongside
`JSPlugin` is unnecessary because `JSPlugin.setupDI` delegates to it.

## Public types

- [`SampleViewConfig`](src/commonMain/kotlin/ui/SampleViewConfig.kt) is the serializable
  `ViewConfig`. `backgroundColor` defaults to `null`, while `text` defaults to `"Hello, world!"`.
- [`SampleModel`](src/commonMain/kotlin/ui/SampleModel.kt) is the model contract. It currently has no
  members; the plugin registers an empty placeholder implementation for sample-screen behaviour.
- [`SampleViewModel`](src/commonMain/kotlin/ui/SampleViewModel.kt) connects a
  `NavigationNode<SampleViewConfig, ViewConfig>` to the model. It currently adds no state or actions
  beyond the navigation `ViewModel` base class.
- [`SampleView`](src/jsMain/kotlin/ui/SampleView.kt) is the Compose Web navigation node. It lazily
  injects its view model and currently renders the parameterless `FlotViewDashboard` reference
  dashboard. The current drawing code does not use `SampleViewConfig.backgroundColor` or `text`.
- [`SampleStrings`](src/jsMain/kotlin/SampleStrings.kt) exposes the `sample` string resource with
  English `"Sample"` as its default value and Russian `"Пример"` as a translation. It is a
  directly accessed object, not a Koin service; in Compose code resolve the resource with
  `SampleStrings.sample.translation()`.

## Dependency injection

The startup plugins add these Koin definitions:

| Type | Scope and qualifier | How to obtain it | Purpose |
| --- | --- | --- | --- |
| `SampleModel` | singleton, no qualifier | `koin.get<SampleModel>()` | Empty default model implementation. |
| `SampleViewModel` | factory, no qualifier | `koin.get<SampleViewModel> { parametersOf(node) }` | A new view model for the supplied `NavigationNode<SampleViewConfig, ViewConfig>`; its `SampleModel` is resolved from Koin. |
| `SerializersModule` | singleton, generated random qualifier | `koin.getAllDistinct<SerializersModule>()` | Registers `SampleViewConfig` polymorphically as both `Any` and `ViewConfig`. The common JSON setup aggregates these contributions. |
| `NavigationNodeFactory<ViewConfig>` | singleton, generated random qualifier | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | Creates `SampleView` for a `SampleViewConfig`; installed only by `JSPlugin`. |

`singleWithRandomQualifier` deliberately gives each aggregate contribution an unstable qualifier.
Do not use an invented named qualifier or a singular unqualified `get()` for `SerializersModule` or
`NavigationNodeFactory<ViewConfig>`; aggregate them with `getAllDistinct` from
`dev.inmo.micro_utils.koin`.

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.parameter.parametersOf
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.sample.ui.SampleModel
import space.kscience.frameswork.features.ui.sample.ui.SampleViewModel

val model: SampleModel = koin.get()
val viewModel: SampleViewModel = koin.get { parametersOf(node) }
val serializerContributions: List<SerializersModule> = koin.getAllDistinct()
val viewFactories: List<NavigationNodeFactory<ViewConfig>> = koin.getAllDistinct()
```

`SampleViewConfig`, `SampleView`, and `SampleStrings` are not registered directly in Koin. Construct
a config yourself. Normally the aggregated navigation factory constructs the view, and the view
requests its view model with `parametersOf(this@SampleView)`. Direct `SampleViewModel(node, model)`
construction is also supported when both dependencies are already available. Constructing a
`SampleView(chain, config)` directly mirrors the factory, but Koin must be running before its lazy
`viewModel` is accessed.

## Navigation usage

Once `JSPlugin` has contributed its typed factory, a host can navigate with a
`SampleViewConfig`. For example, inside an existing navigation composition:

```kotlin
InjectNavigationNode(
    SampleViewConfig(text = "Diagnostic sample")
)
```

The host's aggregated `NavigationNodeFactory<ViewConfig>` selects the typed factory and creates
`SampleView(chain, config)`. The `features.ui.panel` module also uses `SampleViewConfig` in its
sample panel-item provider and default panel data.

## Network paths and server endpoints

This module has no server source set, Ktor configurators, HTTP routes, WebSocket routes, or client
requests. It therefore defines no endpoint or client path, relative or absolute.
