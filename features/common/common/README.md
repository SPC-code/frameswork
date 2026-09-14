# Common core

Gradle module: `:frameswork.features.common.common`  
Maven module: `space.kscience:frameswork.features.common.common`

This Kotlin Multiplatform module supplies the shared infrastructure used by the other Frameswork
features: baseline serialization and coroutine bindings, subscription-aware flow adapters, metadata
copying, byte conversion, array filtering, and a common weak-reference abstraction. The published
targets are JVM and JavaScript; Gradle selects the corresponding `-jvm` or `-js` variant through
multiplatform metadata.

Russian documentation: [README.ru.md](README.ru.md).

## Startup and dependency injection

For the published targets, load [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) or
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) through the application's `StartPlugin` startup mechanism.
Both platform entry points delegate their DI setup to the shared
[`Plugin`](src/commonMain/kotlin/Plugin.kt). The repository also contains a
[`CommonAndroidPlugin`](src/androidMain/kotlin/CommonAndroidPlugin.kt) entry point, although the
module's current Gradle configuration builds only JVM and JavaScript targets.

After plugin setup, the following singleton definitions can be obtained from Koin:

| Type | Qualifier | Retrieval | What is provided |
| --- | --- | --- | --- |
| `ContentType` | none | `koin.get<ContentType>()` | `ContentType.Application.Json` |
| `CoroutineScope` | none | `koin.get<CoroutineScope>()` | A `Dispatchers.Default` linked supervisor scope with uncaught exceptions logged through KSLog |
| `Json` | none | `koin.get<Json>()` | The module's configured JSON serializer |
| `StringFormat` | none | `koin.get<StringFormat>()` | The same singleton instance as `Json`, exposed through its format interface |
| `SerialFormat` | none | `koin.get<SerialFormat>()` | The same singleton instance as `Json`, exposed through its base format interface |
| `SerializersModule` | random, generated internally | `koin.getAllDistinct<SerializersModule>()` | Serializer-module contributions; use collection lookup because an unqualified single-value lookup does not address these definitions |

For example:

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin

fun dependencies(koin: Koin) {
    val contentType = koin.get<ContentType>()
    val scope = koin.get<CoroutineScope>()
    val json = koin.get<Json>()
    val stringFormat = koin.get<StringFormat>()
    val serialFormat = koin.get<SerialFormat>()
    val serializerContributions = koin.getAllDistinct<SerializersModule>()
}
```

The `Json` singleton ignores unknown keys, uses array polymorphism, supports structured map keys and
special floating-point values, and combines every `SerializersModule` contribution visible to Koin
when that singleton is first created. This module's own randomly qualified contribution registers
polymorphic serializers under `Any` for `DateTime`, `String`, and the supported numeric primitives.

## Flow utilities

The flow APIs are regular extension functions; they are **not registered in Koin**.

- [`toSharedFlow`](src/commonMain/kotlin/utils/FlowToSharedFlow.kt) shares one source collection while
  the output has subscribers and returns the output together with its lifecycle `Job`.
- [`toHalfColdFlow`](src/commonMain/kotlin/utils/HalfColdFlow.kt) observes a flow of inner flows and
  collects the selected inner flow only while the shared output has subscribers.
- [`halfColdFlows`](src/commonMain/kotlin/utils/HalfColdFlows.kt) creates the same subscription-aware
  behavior independently for each key in a changing map of flows.
- [`StateFlowMap` and `mapAsStateFlow`](src/commonMain/kotlin/utils/StateFlowMap.kt) provide a mapped
  `StateFlow` view without launching a collector or storing a second state value.

Call these APIs directly and retain the returned `Job` when the caller needs explicit cancellation:

```kotlin
val (shared, lifecycle) = source.toSharedFlow(scope, replay = 1)
// lifecycle.cancel() when explicit early shutdown is required
```

## Serialization and data utilities

- [`DateTimeSerializer`](src/commonMain/kotlin/utils/DateTimeSerializer.kt) encodes `DateTime.unixMillis`
  as a `Double`. It is used directly as an object and is contributed to the configured
  `SerializersModule`; it is not registered as its own Koin definition.
- [`MetaContainer.modified`](src/commonMain/kotlin/utils/BuildMetaContainer.kt) copies a metadata
  container and applies builder changes to the copy.
- [`Int.toByteArray` and `ByteArray.toInt`](src/commonMain/kotlin/utils/ByteArrayToInt.kt) convert
  four-byte integers in big-endian order.
- [`Array.filterNotNullInArray`](src/commonMain/kotlin/utils/ArrayFilterNotNull.kt) returns a typed array
  containing only the non-null elements.

These functions are called directly and none of them is registered in Koin.

## Weak references

[`WeakRef`](src/commonMain/kotlin/utils/WeakRef.kt) is an expected multiplatform type backed by
`java.lang.ref.WeakReference` on JVM and the native JavaScript `WeakRef` on JS. Construct it directly
with `WeakRef(value)` and inspect it with `reference.get()`. It is not registered in Koin.

This module does not install server routes or expose HTTP endpoints.
