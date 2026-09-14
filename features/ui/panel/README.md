# Panel UI

[Russian version](README.ru.md)

Gradle module: `:frameswork.features.ui.panel`

Maven module: `space.kscience:frameswork.features.ui.panel`

This Kotlin Multiplatform module provides the browser UI for displaying and editing a panel of
child Frameswork navigation views. It defines the panel navigation configuration, model and view
model contracts, the Compose Web view, localized strings, and a built-in editor for sample items.

## Dependency and startup

Add the published artifact to a JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.ui.panel:<version>")
        }
    }
}
```

Add [`space.kscience.frameswork.features.ui.panel.JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) to the
browser application's `StartLauncherPlugin` plugin list. Its setup includes the common
registrations from [`Plugin`](src/commonMain/kotlin/Plugin.kt), so do not add both panel UI plugins
separately.

The same startup graph must also supply the shared `Json`, navigation infrastructure, and a
`PanelFeature`. The repository client does this with the common-common, common-web, and panel-web
JS plugins. It also starts the sample UI plugin: that plugin contributes the serializer and
navigation factory required by the built-in `SamplePanelViewConfigProvider` and by the fallback
sample layout. A Maven dependency makes these APIs available at compile time but does not start
their plugins.

## Main types

- [`PanelViewConfig`](src/commonMain/kotlin/ui/PanelViewConfig.kt) is the serializable, fieldless
  `ViewConfig` that identifies the panel screen. The layout itself is loaded by the model.
- [`PanelModel`](src/commonMain/kotlin/ui/PanelModel.kt) is the UI-facing layout contract. It exposes
  a `Flow<PanelInfo>`, accepts complete replacements, and supplies the `Json` used to decode each
  item's polymorphic view configuration.
- [`PanelViewModel`](src/commonMain/kotlin/ui/PanelViewModel.kt) loads the persisted layout and owns
  view/edit mode, the add-item workflow, provider selection, and JSON import/export. Saving launches
  `PanelModel.updatePanelInfo` and then exits edit mode.
- [`PanelViewConfigProvider`](src/commonMain/kotlin/ui/PanelViewConfigProvider.kt) is an extension
  point for Compose editors that create item-specific `ViewConfig` values. A provider reports
  `null` while its form is incomplete and a configuration when it is valid.
- [`PanelView`](src/jsMain/kotlin/ui/PanelView.kt) is the Compose Web navigation view. It renders
  each decoded panel item as a child navigation node and supports adding, moving, resizing,
  deleting, importing, exporting, and saving items.
- [`PanelStrings`](src/jsMain/kotlin/PanelStrings.kt) contains the panel's English string resources
  and Russian translations. It is accessed directly, for example with
  `PanelStrings.save.translation()`.
- [`SamplePanelViewConfigProvider`](src/jsMain/kotlin/ui/sample/SamplePanelViewConfigProvider.kt) is
  the built-in provider. Its form creates a `SampleViewConfig` only after a non-blank text and a
  background color have both been entered.

The default `PanelModel` registered by the plugin delegates storage to `PanelFeature`. Its flow
first tries the current layout, then the feature's default layout, accepting either only when every
item configuration can be decoded by the shared `Json`. If neither is usable, it emits a generated
three-by-three layout of sample items. The model's update method delegates the replacement to
`PanelFeature` and, if that call does not throw, signals the flow to reload; the
`PanelFeature.setPanelConfig` Boolean result is not exposed by `PanelModel`.

## Dependency injection

The startup plugins install these Koin definitions:

| Registered type | Scope and qualifier | How to obtain it | Purpose |
| --- | --- | --- | --- |
| `PanelModel` | singleton, no qualifier | `koin.get<PanelModel>()` | Default `PanelFeature`-backed model; requires unqualified `PanelFeature` and `Json` definitions. |
| `PanelViewModel` | factory, no qualifier | `koin.get<PanelViewModel> { parametersOf(node) }` | Creates a view model for the supplied `NavigationNode<PanelViewConfig, ViewConfig>`; resolves `PanelModel`, `Json`, and all provider contributions from Koin. |
| `SerializersModule` | singleton, generated random qualifier | `koin.getAllDistinct<SerializersModule>()` | Registers `PanelViewConfig` polymorphically under both `Any` and `ViewConfig`. |
| `NavigationNodeFactory<ViewConfig>` | singleton, generated random qualifier | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | JS-only typed factory that creates `PanelView(chain, config)` for `PanelViewConfig`. |
| `PanelViewConfigProvider` | singleton, generated random qualifier | `koin.getAllDistinct<PanelViewConfigProvider>()` | JS-only contribution backed by the `SamplePanelViewConfigProvider` object. |

`singleWithRandomQualifier` creates an unstable qualifier for every aggregate contribution. Do not
invent a named qualifier or use singular unqualified `get()` for `SerializersModule`,
`NavigationNodeFactory<ViewConfig>`, or `PanelViewConfigProvider`; import
`dev.inmo.micro_utils.koin.getAllDistinct` and retrieve their collections.

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.parameter.parametersOf
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.ui.PanelModel
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfigProvider
import space.kscience.frameswork.features.ui.panel.ui.PanelViewModel

val model: PanelModel = koin.get()
val viewModel: PanelViewModel = koin.get { parametersOf(node) }
val serializers: List<SerializersModule> = koin.getAllDistinct()
val viewFactories: List<NavigationNodeFactory<ViewConfig>> = koin.getAllDistinct()
val itemEditors: List<PanelViewConfigProvider> = koin.getAllDistinct()
```

`PanelViewConfig`, `PanelView`, and `PanelStrings` are not registered directly in Koin. Construct a
configuration with `PanelViewConfig()`. Normally the aggregated navigation factory constructs the
view; constructing `PanelView(chain, config)` directly is possible, but Koin must be running before
its lazy `viewModel` is accessed. Direct construction of
`PanelViewModel(node, model, providers, json)` is also supported. The plugin's default `PanelModel`
is anonymous; without DI, implement `PanelModel` yourself. The sample provider is an object and can
be referenced directly, although Koin exposes it only as one randomly qualified
`PanelViewConfigProvider` contribution.

To add another item editor, implement `PanelViewConfigProvider` and contribute it under the
interface type:

```kotlin
singleWithRandomQualifier<PanelViewConfigProvider> { MyPanelItemProvider }
```

The item configuration type must also have a polymorphic serializer contribution and a matching
`NavigationNodeFactory<ViewConfig>` so saved items can be decoded and rendered.

## Navigation usage

After `JSPlugin` has contributed its typed factory, open the panel from an existing navigation
composition with its fieldless configuration:

```kotlin
InjectNavigationNode(
    PanelViewConfig()
)
```

The application's aggregated `NavigationNodeFactory<ViewConfig>` selects the typed panel factory.
Inside `PanelView`, every `PanelItemInfo.config` is decoded with `PanelModel.decodingJson` and passed
to another `InjectNavigationNode`, allowing each grid cell to host any registered view type.

## Network paths and server endpoints

This module has no server source set, Ktor routing configurator, HTTP or WebSocket endpoint, direct
`HttpClient` request, or client path constant. Its default model calls the `PanelFeature` interface
and does not choose a URL. A concrete `PanelFeature` may perform persistence locally or remotely;
the repository's panel-web implementation documents its own relative client paths in the
[panel-web README](../../panel/web/README.md).
