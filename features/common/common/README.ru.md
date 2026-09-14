# Общее ядро

Модуль Gradle: `:frameswork.features.common.common`  
Модуль Maven: `space.kscience:frameswork.features.common.common`

Этот модуль Kotlin Multiplatform предоставляет общую инфраструктуру, которую используют остальные
возможности Frameswork: базовые зависимости сериализации и корутин, учитывающие подписчиков адаптеры
потоков, копирование метаданных, преобразование байтов, фильтрацию массивов и общий интерфейс слабых
ссылок. Публикуемые платформы — JVM и JavaScript; Gradle выбирает соответствующий вариант `-jvm` или
`-js` с помощью метаданных мультиплатформенного проекта.

Документация на английском: [README.md](README.md).

## Запуск и внедрение зависимостей

Для публикуемых платформ подключите [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) или
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) через механизм запуска `StartPlugin` приложения. Обе
платформенные точки входа передают настройку DI общему
[`Plugin`](src/commonMain/kotlin/Plugin.kt). В репозитории также есть точка входа
[`CommonAndroidPlugin`](src/androidMain/kotlin/CommonAndroidPlugin.kt), хотя текущая конфигурация
Gradle этого модуля собирает только цели JVM и JavaScript.

После настройки плагина из Koin можно получить следующие синглтоны:

| Тип | Квалификатор | Получение | Что предоставляется |
| --- | --- | --- | --- |
| `ContentType` | отсутствует | `koin.get<ContentType>()` | `ContentType.Application.Json` |
| `CoroutineScope` | отсутствует | `koin.get<CoroutineScope>()` | Связанная надзорная область на `Dispatchers.Default`, которая записывает необработанные исключения через KSLog |
| `Json` | отсутствует | `koin.get<Json>()` | Настроенный модулем JSON-сериализатор |
| `StringFormat` | отсутствует | `koin.get<StringFormat>()` | Тот же синглтон, что и `Json`, предоставленный через интерфейс строкового формата |
| `SerialFormat` | отсутствует | `koin.get<SerialFormat>()` | Тот же синглтон, что и `Json`, предоставленный через базовый интерфейс формата |
| `SerializersModule` | случайный, генерируется внутри | `koin.getAllDistinct<SerializersModule>()` | Вклады модулей сериализаторов; используйте получение коллекции, потому что обращение без квалификатора за одним значением не адресует эти определения |

Пример:

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

Синглтон `Json` игнорирует неизвестные ключи, использует полиморфизм на основе массивов,
поддерживает структурированные ключи карт и специальные значения с плавающей точкой, а также
объединяет все вклады `SerializersModule`, доступные Koin в момент первого создания синглтона.
Собственный вклад этого модуля со случайным квалификатором регистрирует полиморфные сериализаторы
под `Any` для `DateTime`, `String` и поддерживаемых числовых примитивов.

## Утилиты потоков

API потоков — обычные функции-расширения; они **не зарегистрированы в Koin**.

- [`toSharedFlow`](src/commonMain/kotlin/utils/FlowToSharedFlow.kt) совместно использует одну выборку
  исходного потока, пока у результата есть подписчики, и возвращает результат вместе с управляющим
  его жизненным циклом `Job`.
- [`toHalfColdFlow`](src/commonMain/kotlin/utils/HalfColdFlow.kt) наблюдает за потоком вложенных потоков
  и собирает выбранный вложенный поток, только пока у общего результата есть подписчики.
- [`halfColdFlows`](src/commonMain/kotlin/utils/HalfColdFlows.kt) независимо создаёт такое же
  учитывающее подписчиков поведение для каждого ключа в изменяемой карте потоков.
- [`StateFlowMap` и `mapAsStateFlow`](src/commonMain/kotlin/utils/StateFlowMap.kt) предоставляют
  отображённое представление `StateFlow`, не запуская сборщик и не сохраняя второе значение состояния.

Вызывайте эти API напрямую и сохраняйте возвращённый `Job`, если вызывающему коду требуется явная отмена:

```kotlin
val (shared, lifecycle) = source.toSharedFlow(scope, replay = 1)
// lifecycle.cancel(), если требуется явная досрочная остановка
```

## Сериализация и утилиты данных

- [`DateTimeSerializer`](src/commonMain/kotlin/utils/DateTimeSerializer.kt) кодирует
  `DateTime.unixMillis` как `Double`. Он используется напрямую как объект и добавляется в настроенный
  `SerializersModule`; отдельное определение Koin для него не регистрируется.
- [`MetaContainer.modified`](src/commonMain/kotlin/utils/BuildMetaContainer.kt) копирует контейнер
  метаданных и применяет изменения построителя к копии.
- [`Int.toByteArray` и `ByteArray.toInt`](src/commonMain/kotlin/utils/ByteArrayToInt.kt) преобразуют
  четырёхбайтовые целые числа в порядке от старшего байта к младшему.
- [`Array.filterNotNullInArray`](src/commonMain/kotlin/utils/ArrayFilterNotNull.kt) возвращает
  типизированный массив, содержащий только элементы, не равные `null`.

Эти функции вызываются напрямую, и ни одна из них не зарегистрирована в Koin.

## Слабые ссылки

[`WeakRef`](src/commonMain/kotlin/utils/WeakRef.kt) — ожидаемый мультиплатформенный тип, который на JVM
основан на `java.lang.ref.WeakReference`, а в JS — на нативном `WeakRef` JavaScript. Создавайте его
напрямую с помощью `WeakRef(value)` и проверяйте через `reference.get()`. Тип не зарегистрирован в Koin.

Этот модуль не устанавливает серверные маршруты и не предоставляет HTTP-эндпоинты.
