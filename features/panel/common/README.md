# Panel common

Gradle module: `:frameswork.features.panel.common`  
Maven module: `space.kscience:frameswork.features.panel.common`

This Kotlin Multiplatform module defines the shared panel-layout contract, serializable panel model,
and relative path constants used by the panel server and web modules. Its configured publication
targets are JVM and JavaScript; Gradle selects the matching platform variant from the multiplatform
dependency.

Russian documentation: [README.ru.md](README.ru.md).

## Dependency

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.panel.common:<version>")
}
```

## Panel API

[`PanelFeature`](src/commonMain/kotlin/PanelFeature.kt) is the storage-independent contract for a
panel layout:

- `getPanelConfig()` returns the current layout, or `null` when one has not been stored.
- `getDefaultConfig()` returns the default layout, or `null` when none is available.
- `setPanelConfig(config)` asks the implementation to replace the current layout and reports whether
  the update was accepted.

The interface does not prescribe persistence, concurrency, or validation. Those policies belong to
the implementation.

## Model

[`PanelInfo`](src/commonMain/kotlin/models/PanelInfo.kt) is a serializable panel description. It stores
the horizontal and vertical slot counts and a list of `PanelItemInfo` values. It does not validate
positive dimensions, item overlap, or whether items fit inside the grid.

[`PanelItemInfo`](src/commonMain/kotlin/models/PanelItemInfo.kt) stores an item's origin, size, and
item-specific configuration as a `JsonElement`. Its containment helpers use half-open bounds:
`[x, x + width)` and `[y, y + height)`.

Create model values directly:

```kotlin
val item = PanelItemInfo(
    x = 0,
    y = 1,
    width = 2,
    height = 1,
    config = buildJsonObject { put("type", "camera") },
)
val panel = PanelInfo(horizontalSlots = 4, verticalSlots = 3, items = listOf(item))
```

The secondary `PanelItemInfo` constructor accepts an `Any` configuration and encodes it with
`PolymorphicSerializer<Any>`. `decodeConfig(json)` performs the inverse operation. In both cases the
supplied `Json` instance must contain a polymorphic serializer for the concrete configuration type.

## Koin dependency injection

The common [`Plugin`](src/commonMain/kotlin/Plugin.kt) has an empty `setupDI` implementation. The
[`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) and
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) delegate to it, so loading this module alone adds **no
Koin definitions**. There are no qualifiers or exact retrieval calls supplied by this module.

| Declaration | Built-in Koin definition | How to obtain it |
| --- | --- | --- |
| `PanelFeature` | none | Register or load an implementation, then use unqualified `koin.get<PanelFeature>()` |
| `PanelInfo` | none | Construct or deserialize it directly |
| `PanelItemInfo` | none | Construct or deserialize it directly |
| `PanelConstants` | none | Use the Kotlin object directly |

The sibling `panel.server` and `panel.web` plugins each provide an unqualified `PanelFeature`
binding for their own implementation. Alternatively, an application can provide one explicitly:

```kotlin
val panelModule = module {
    single<PanelFeature> { ApplicationPanelFeature(/* application dependencies */) }
}

val panelFeature = koin.get<PanelFeature>()
```

This common module neither defines `ApplicationPanelFeature` nor chooses the implementation's
dependencies; both belong to the application. Model instances normally represent request, response,
or stored data and are not DI services.

## Relative paths and platform scope

[`PanelConstants`](src/commonMain/kotlin/PanelConstants.kt) contains the relative strings shared by
the companion HTTP modules:

| Constant | Value |
| --- | --- |
| `rootPanelPath` | `panel` |
| `getPanelSubpath` | `get` |
| `getPanelDefaultSubpath` | `default` |
| `setPanelSubpath` | `set` |
| `getPanelFullPath` | `panel/get` |
| `getPanelDefaultPath` | `panel/default` |
| `setPanelFullPath` | `panel/set` |

All paths are relative and have no leading slash. This module contains no Ktor configurator, installs
no routes, and therefore exposes no HTTP endpoints by itself. The current build publishes JVM and
JavaScript targets. An Android plugin source is present for reuse, but the module does not currently
configure or publish an Android target.
