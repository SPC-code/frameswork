# Веб-клиент панели кадров

[Английская версия](README.md)

Модуль Gradle: `:frameswork.features.ui.panel.frames.web`  
Модуль Maven: `space.kscience:frameswork.features.ui.panel.frames.web`

Этот модуль Kotlin Multiplatform предоставляет браузерный клиент и Compose Web UI для
отображения кадров камеры из массивов байтов в редактируемой панели. Он объединяет
HTTP/WebSocket-реализацию [общего контракта панели кадров](../common/README.ru.md),
кэширование метаданных и ограничение частоты кадров, навигационные модели камеры,
селектор камеры и окно прямой трансляции, а также небольшие внешние фасады Plotly.js.

Текущая конфигурация Gradle публикует только цель Kotlin/JS. Файлы плагинов в `jvmMain` и
`androidMain` этим модулем не компилируются.

## Подключение

Добавьте multiplatform-координату в JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.ui.panel.frames.web:<version>")
        }
    }
}
```

Модуль экспортирует `frameswork.features.ui.panel.frames.common`, `frameswork.features.ui.panel` и
`frameswork.features.frames.web` как API-зависимости.

## Транспорт и потоки кадров

[`KtorFramesDataInfoFeature`](src/commonMain/kotlin/ktor/KtorFramesDataInfoFeature.kt) реализует
`FramesDataInfoFeature` с помощью переданных Ktor `HttpClient` и `Json`:

- снимки метаданных запрашиваются по HTTP;
- каждый вызов `getFramesFlow` создаёт поток на основе переподключающегося WebSocket,
  разделяемый в переданном `CoroutineScope`;
- по умолчанию после одной секунды без кадров клиент отправляет прикладное текстовое
  сообщение `ping` и ждёт ответ пять секунд; и
- по умолчанию после завершения или сбоя сессии он ждёт три секунды перед переподключением.

[`GetFramesProxyFramesDataInfoFeature`](src/commonMain/kotlin/features/GetFramesProxyFramesDataInfoFeature.kt)
делегирует метаданные и применяет заданное вызывающим кодом преобразование к потокам
кадров. `GetFramesBackPressurePanelCameraInfoFeature` создаёт этот декоратор с `Flow.sample`, поэтом
промежуточные кадры отбрасываются, если источник работает быстрее выбранного интервала.

[`CacheFramesDataInfoFeature`](src/commonMain/kotlin/features/CacheFramesDataInfoFeature.kt) независимо
кэширует снимок процессоров и снимки источников для каждого процессора на пять секунд по
умолчанию, включая nullable-результат источников. Он также запоминает объект fallback-потока для
каждой пары «процессор/источник». Сам класс не превращает холодный fallback-поток в горячий; в стандартном
графе Koin он оборачивает уже разделяемый Ktor-поток.

Стандартный конвейер плагина:

```text
KtorFramesDataInfoFeature
  -> GetFramesProxyFramesDataInfoFeature (выборка каждые 30 мс, квалификатор "back_pressure")
  -> CacheFramesDataInfoFeature
  -> неквалифицированный FramesDataInfoFeature
  -> CameraModel
```

Все три реализации можно также скомпоновать напрямую:

```kotlin
val remote = KtorFramesDataInfoFeature(
    client = client,
    json = json,
    scope = scope,
)
val sampled = GetFramesBackPressurePanelCameraInfoFeature(remote, 30.milliseconds)
val frames: FramesDataInfoFeature = CacheFramesDataInfoFeature(
    fallback = sampled,
    scope = scope,
)
```

## Модель камеры и UI

- [`CameraModel`](src/commonMain/kotlin/ui/camera/CameraModel.kt) — UI-контракт для поиска процессоров,
  поиска источников камеры и потоков кадров. Плагин предоставляет адаптер на основе
  неквалифицированного `FramesDataInfoFeature`; приложения могут реализовать его напрямую.
- [`CameraViewConfig`](src/commonMain/kotlin/ui/camera/CameraViewConfig.kt) — сериализуемый `ViewConfig`,
  содержащий `FramesFlowFeatureId`. Создавайте его напрямую при выборе потока.
- [`CameraViewModel`](src/commonMain/kotlin/ui/camera/CameraViewModel.kt) переключает свой поток `frames`, когда
  навигационная конфигурация выбирает другую пару «процессор/источник».
- [`CameraPanelViewConfigProvider`](src/jsMain/kotlin/ui/camera/CameraPanelViewConfigProvider.kt) рисует
  выпадающие списки процессоров и камер, обновляет их снимки раз в секунду и передаёт
  завершённый `CameraViewConfig` редактору панели.
- [`CameraView`](src/jsMain/kotlin/ui/camera/CameraView.kt) кодирует каждую полезную нагрузку в base64 как
  JPEG data URL. Наложение панели показывает выбранный поток, состояние «онлайн/офлайн»,
  измеренную частоту кадров и размеры при наличии метаданных `FrameSourceWidth` и `FrameSourceHeight`.
  После перехода потока в офлайн последнее изображение остаётся видимым.
- [`CommonPanelStrings`](src/commonMain/kotlin/CommonPanelStrings.kt) и
  [`CameraStrings`](src/jsMain/kotlin/CameraStrings.kt) содержат английские/русские строковые ресурсы. Это
  объекты Kotlin: обращайтесь к ним напрямую, а не через Koin.

## Регистрации Koin

Добавьте `space.kscience.frameswork.features.ui.panel.frames.JSPlugin` в список `StartLauncherPlugin`
приложения. Он устанавливает общие определения ниже и два JS-вклада. Он не активирует
стартовые плагины API-зависимостей, поэтому хост должен также установить нужные ему плагины
общего приложения/web и панели. В частности, граф должен предоставить неквалифицированные
определения `HttpClient`, `Json` и `CoroutineScope`.

| Зарегистрированный тип | Квалификатор | Получение | Примечания |
| --- | --- | --- | --- |
| `KtorFramesDataInfoFeature` | нет | `koin.get<KtorFramesDataInfoFeature>()` | Singleton; требует неквалифицированные `HttpClient`, `Json` и `CoroutineScope`. |
| `FramesDataInfoFeature` | `StringQualifier("back_pressure")` | `koin.get<FramesDataInfoFeature>(named("back_pressure"))` | Singleton-прокси с интервалом выборки 30 мс. |
| `CacheFramesDataInfoFeature` | нет | `koin.get<CacheFramesDataInfoFeature>()` | Singleton вокруг квалифицированного прокси с выборкой. |
| `FramesDataInfoFeature` | нет | `koin.get<FramesDataInfoFeature>()` | Разрешается в тот же singleton кэша. |
| `CameraModel` | нет | `koin.get<CameraModel>()` | Singleton-адаптер вокруг неквалифицированного сервиса кадров. |
| `CameraViewModel` | нет | `koin.get<CameraViewModel> { parametersOf(node) }` | Фабрика; требует параметр `NavigationNode<CameraViewConfig, ViewConfig>`. `CameraView` передаёт себя как этот параметр. |
| `SerializersModule` | сгенерированный случайный квалификатор | `koin.getAllDistinct<SerializersModule>()` | Добавляет полиморфные сериализаторы `CameraViewConfig` для `Any` и `ViewConfig`. |
| `NavigationNodeFactory<ViewConfig>` | сгенерированный случайный квалификатор | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | JS-фабрика с типом, создающая `CameraView`. |
| `PanelViewConfigProvider` | сгенерированный случайный квалификатор | `koin.getAllDistinct<PanelViewConfigProvider>()` | JS-вклад, создающий `CameraPanelViewConfigProvider`. |

`named("back_pressure")` создаёт тот же строковый квалификатор, что и явный
`StringQualifier("back_pressure")` плагина. У случайных квалификаторов, создаваемых `singleWithRandomQualifier`, нет
стабильного значения; получайте эти вклады как коллекции через `dev.inmo.micro_utils.koin.getAllDistinct`, а не по
выдуманному имени. Конкретные типы `CameraPanelViewConfigProvider` и `CameraView` не являются определениями Koin.

Получайте агрегированные `Json`, навигационные фабрики и провайдеры панели только после того,
как все стартовые модули внесут свои определения. Сами singleton-объекты плагинов — прямые объекты Kotlin,
а не сервисы Koin.

## Относительные клиентские пути и владение сервером

Все пути — относительные, без начального слэша; они разрешаются относительно базового URL переданного
`HttpClient`.

| Транспорт | Относительный путь | Запрос | Назначение |
| --- | --- | --- | --- |
| `GET` | `cameras/getAvailableProcessors` | Без параметров | Вернуть имена доступных процессоров; клиент использует пустое множество, если статус ответа не равен `200 OK`. |
| `GET` | `cameras/getAvailableCameras` | Query-параметр `id={processorName}` | Вернуть `Set<FramesSourceId>?` этого процессора. `id` — общий `idParameterName`. |
| WebSocket | `cameras/getFrames` | Первое текстовое сообщение — сериализованный `FramesFlowFeatureId` | Получать закодированные `ByteArrayFrameData` в бинарных кадрах; текстовые сообщения `ping`/`pong` обеспечивают прикладной heartbeat. |

Этот клиентский модуль не устанавливает маршруты или server endpoints. Соответствующие endpoints монтирует
`FramesDataInfoFeatureRoutingsConfigurator` из отдельного
[серверного модуля панели кадров](../server/README.ru.md). Хост-приложение может смонтировать этот
конфигуратор под дополнительным префиксом, который также нужно отразить в базовом URL клиента.

## Фасады Plotly.js

[`Plotly`](src/jsMain/kotlin/utils/plotly/Plotly.kt) открывает для Kotlin/JS операции `newPlot`, `react` и вложенную
[`Plots.resize`](src/jsMain/kotlin/utils/plotly/Plots.kt). Это внешние объявления, а не реализации или сервисы Koin.
Этот артефакт не включает Plotly.js; хост-страница должна загрузить совместимый скрипт, который
предоставит глобальный объект `Plotly`, до вызова этих операций.
