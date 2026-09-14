# Common Web

[English version](README.md)

Maven-модуль: `space.kscience:frameswork.features.common.web`

Этот Kotlin Multiplatform-модуль содержит общую основу веб-клиента Frameswork. Он
собирает Ktor `HttpClient`, добавляет конфигурацию JSON и WebSocket, предоставляет базовые
типы конфигурации навигации и набор UI-компонентов операторской панели FlotView для Compose Web.

## Подключение и запуск модуля

Добавьте опубликованный артефакт в JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.common.web:<version>")
        }
    }
}
```

Добавьте `space.kscience.frameswork.features.common.web.JSPlugin` в список плагинов
`StartLauncherPlugin`. Его DI-настройка включает общие регистрации веб-модуля и JavaScript-движок
Ktor. Общий плагин модуля зависит от экземпляра `Json`, который предоставляет
`frameswork.features.common.common`, поэтому этот общий плагин также должен входить в тот же граф запуска.

## Внедрение зависимостей

Плагины запуска регистрируют следующие определения Koin:

| Тип | Qualifier | Получение из Koin | Назначение |
| --- | --- | --- | --- |
| `HttpClient` | нет | `koin.get<HttpClient>()` | Общий полностью настроенный клиент. |
| `HttpClientEngineFactory<*>` | нет | `koin.get<HttpClientEngineFactory<*>>()` | JavaScript-движок (`Js`) от `JSPlugin`; общий плагин может также использовать другой движок, зарегистрированный приложением. |
| `HttpClientConfigurator` | случайный qualifier для каждого вклада | `koin.getAllDistinct<HttpClientConfigurator>()` | Вклады в настройку сериализации, URL по умолчанию и WebSocket. |
| `NavigationNodeFactory<ViewConfig>` | случайный qualifier | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | Типизированная фабрика, создающая пустой узел навигации для `EmptyConfig`; её предполагается агрегировать с фабриками модулей функций. |

`getAllDistinct` — extension-функция из `dev.inmo.micro_utils.koin`. Случайные qualifier-ы создаются
`singleWithRandomQualifier` и не имеют стабильного имени. Поэтому конкретные классы
`SerializationConfigurator`, `DefaultUrlHttpClientConfigurator` и `WebSocketsConfigurator`
зарегистрированы только как вклады `HttpClientConfigurator`: получить их через
`koin.get<SerializationConfigurator>()` (и аналогично для других конкретных классов) нельзя.

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import io.ktor.client.HttpClient
import space.kscience.frameswork.features.common.web.configurators.HttpClientConfigurator

val client: HttpClient = koin.get()
val contributions: List<HttpClientConfigurator> = koin.getAllDistinct()
```

Получайте `HttpClient` только после установки DI-определений всех модулей функций: клиент
собирает все вклады `HttpClientConfigurator` при первом создании singleton-экземпля.

## Конфигурация HTTP и WebSocket

- [`HttpClientConfigurator`](src/commonMain/kotlin/configurators/HttpClientConfigurator.kt) —
  интерфейс вклада, используемого при сборке общего клиента.
- [`SerializationConfigurator`](src/commonMain/kotlin/configurators/SerializationConfigurator.kt)
  устанавливает Ktor Content Negotiation с DI-экземпляром `Json` и выбирает
  `application/json` как тип содержимого запроса по умолчанию.
- [`DefaultUrlHttpClientConfigurator`](src/commonMain/kotlin/configurators/DefaultUrlHttpClientConfigurator.kt)
  разрешает запросы относительно URL, получаемого непосредственно перед каждым запросом.
  Провайдер по умолчанию — [`getUrl`](src/commonMain/kotlin/UrlGetter.kt), JavaScript-реализация
  которого возвращает `window.location.href`. Он добавляет путь запроса к базовому пути, сохраняет
  query-параметры, считает базовую схему без явного значения схемой `http` и сохраняет подходящую схему
  `ws`/`wss` запроса для базы HTTP/HTTPS.
- [`WebSocketsConfigurator`](src/commonMain/kotlin/configurators/WebSocketsConfigurator.kt) устанавливает
  Ktor WebSockets с `KotlinxWebsocketSerializationConverter`, использующим тот же DI-экземпляр `Json`.

Этот модуль **не задаёт фиксированные HTTP endpoints или WebSocket-пути**. Вызывающий код
передаёт в DI-клиент свои относительные пути; конфигуратор URL по умолчанию объединяет их с базовым URL
во время выполнения. В частности, он не добавляет сегмент `/api`.

Конфигураторы также можно создать напрямую, если для автономного клиента нужна другая композиция:

```kotlin
val serialization = SerializationConfigurator(json)
val defaultUrl = DefaultUrlHttpClientConfigurator(
    urlGetter = { "https://example.test/application/" },
    useDefaultUrlPrefix = true,
)
val webSockets = WebSocketsConfigurator(json)
```

## Модели навигации

- [`ViewConfig`](src/commonMain/kotlin/models/ViewConfig.kt) — маркерный тип, общий для моделей
  конфигурации navigation-view.
- [`EmptyConfig`](src/commonMain/kotlin/models/EmptyConfig.kt) — сериализуемая конфигурация view без
  параметров. Создавайте её напрямую как `EmptyConfig()`; Koin содержит
  `NavigationNodeFactory<ViewConfig>`, которая умеет преобразовать её в `NavigationNode.Empty`.

## FlotView UI для Compose Web

UI API не хранит состояние и не регистрируется в Koin. Создавайте модели напрямую и вызывайте
composable-функции из Compose Web:

- [`FlotViewComponents.kt`](src/jsMain/kotlin/ui/FlotViewComponents.kt) содержит переиспользуемые
  карточки, метрики, вкладки, элементы выбора режима, степперы уставок, состояния отправки,
  загрузки/ошибки и их модели (`FlotViewMetric`, `FlotViewTab`, `FlotViewSetpoint`,
  `FlotViewState` и `FlotViewSendState`).
- [`FlotViewDashboard.kt`](src/jsMain/kotlin/ui/FlotViewDashboard.kt) содержит полную модель
  операторской панели, callbacks событий, локализованные строки, состояния видео/рекомендации/сессии и
  composable-функции. `FlotViewDashboard(model, actions, strings, videoContent)` отображает живое
  состояние, которым владеет вызывающий код. Перегрузка без параметров использует
  `FlotViewDashboardModel.referencePreview()` и предназначена только для статического превью дизайна.
- [`FlotViewStyleSheet.kt`](src/jsMain/kotlin/ui/FlotViewStyleSheet.kt) предоставляет вдохновлённые
  ISA-101 цвета и размеры через `FlotViewTokens`, семантические значения `FlotViewTone` и общую
  `FlotViewStyleSheet`. Вызовите `InstallFlotViewStyles()` в документе или используйте интеграцию приложения с
  `StyleSheetsAggregator`.

```kotlin
FlotViewDashboard(
    model = dashboardModel,
    actions = FlotViewDashboardActions(
        onModeSelected = ::selectMode,
        onSend = ::sendPendingSetpoints,
    ),
    videoContent = { ProcessVideo() },
)
```

## Другие утилиты

- [`CommonResources`](src/commonMain/kotlin/CommonResources.kt) предоставляет строки с русским языком по
  умолчанию и английскими переводами для общих UI-действий. Обращайтесь к нему напрямую, например
  `CommonResources.cancel`.
- [`DefaultDateFormat`](src/commonMain/kotlin/DefaultDateFormat.kt) — общий форматтер
  `dd/MM/YYYY, HH:mm:ss`.
- [`MergeUrlBuilders.kt`](src/commonMain/kotlin/utils/MergeUrlBuilders.kt) содержит функции слияния/копирования
  `URLBuilder`, используемые при разрешении URL по умолчанию.
- [`HEXAColor.cssRGBA`](src/commonMain/kotlin/utils/HEXAColorToCSSColorValue.kt) адаптирует RGBA-значение
  `HEXAColor` для CSS Compose Web. Эти утилиты — прямые API, а не сервисы Koin.
