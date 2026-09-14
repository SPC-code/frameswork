# Серверная интеграция Frames

[English version](README.md)

- Gradle-модуль: `:frameswork.features.frames.server`
- Мультиплатформенный Maven-модуль: `space.kscience:frameswork.features.frames.server`
- JVM-публикация Maven: `space.kscience:frameswork.features.frames.server-jvm`

Этот Kotlin Multiplatform-модуль служит точкой серверной JVM-интеграции функциональности Frames. Он
экспортирует [`frames/common`](../common/README.ru.md) и инфраструктуру
[`common/server`](../../common/server/README.ru.md) как API-зависимости и предоставляет точку запуска,
которая делегирует JVM-плагину Frames. Сейчас модуль не содержит собственных серверных классов
предметной области, реализаций сервисов, конфигураторов Ktor или маршрутов.

## Точки запуска

| Объявление | Назначение |
| --- | --- |
| [`Plugin`](src/commonMain/kotlin/Plugin.kt) | Общая точка запуска `StartPlugin`. Её реализации не добавляют локальных привязок или специфичных для модуля действий при запуске. |
| [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) | JVM-точка запуска `StartPlugin`. При настройке DI и запуске она выполняет `space.kscience.frameswork.features.frames.common.JVMPlugin`, а затем локальный связующий `Plugin`. |

Оба объявления являются Kotlin-объектами (`object`), поэтому на них ссылаются напрямую как на
`Plugin` и `JVMPlugin`; их не создают и не получают из Koin.

Одного добавления артефакта в зависимости недостаточно для запуска плагинов. При использовании
стартового загрузчика следует загрузить
`space.kscience.frameswork.features.frames.server.JVMPlugin`. Одновременно загружать
`space.kscience.frameswork.features.frames.common.JVMPlugin` не нужно: серверная точка входа уже ему
делегирует.

Несмотря на API-зависимость от `common/server`, этот модуль **не** вызывает
`space.kscience.frameswork.features.common.server.JVMPlugin`. Полноценный сервер должен отдельно
загрузить этот плагин (или иным способом предоставить его зависимости) перед запуском плагина Frames.
Поэтому обычный набор плагинов выглядит так:

```json
{
  "plugins": [
    "space.kscience.frameswork.features.common.server.JVMPlugin",
    "space.kscience.frameswork.features.frames.server.JVMPlugin"
  ],
  "cameras": {
    "presets": []
  }
}
```

При запуске делегированный JVM-плагин Frames ожидает объект `cameras`. Его массив `presets` содержит
полиморфные значения `FrameSourceConnectorConfig`. Например, встроенный RTSP-коннектор при общей
настройке `Json` с полиморфизмом на основе массивов кодируется как
`["ffmpeg_rtsp", {"id": "camera-id", "url": "rtsp://..."}]`.

## Внедрение зависимостей

Сам модуль не добавляет определений Koin. Вызов `JVMPlugin.setupDI` делегирует регистрацию следующих
определений модулю `frames/common`; они перечислены здесь, чтобы можно было отличить сервисы из DI от
типов API, создаваемых напрямую.

| Тип | Точный квалификатор | Доступность | Получение из Koin |
| --- | --- | --- | --- |
| `DefaultFramesCollector` | Нет | Регистрируется всегда | `koin.get<DefaultFramesCollector>()` |
| `FramesCollector` | Нет | Регистрируется всегда; возвращает `DefaultFramesCollector` | `koin.get<FramesCollector>()` |
| `InMemoryFramesSourcesCollector` | Нет | Регистрируется всегда | `koin.get<InMemoryFramesSourcesCollector>()` |
| Добавление `SerializersModule` для метаданных кадров | Сгенерированный случайный квалификатор | Регистрируется всегда | `koin.getAllDistinct<SerializersModule>()` |
| `space.kscience.frameswork.features.frames.common.JVMPlugin.Config` | Нет | Регистрируется при наличии `cameras` | `koin.get<space.kscience.frameswork.features.frames.common.JVMPlugin.Config>()` |
| Добавление `SerializersModule` для RTSP | Нет | Регистрируется при наличии `cameras` | `koin.get<SerializersModule>()` (также входит в `getAllDistinct`) |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `StringQualifier("exposed_cameras_collector")` | Регистрируется при наличии `cameras` | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cameras_collector"))` |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `StringQualifier("exposed_cached_cameras_collector")` | Регистрируется при наличии `cameras` | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cached_cameras_collector"))` |
| `KVBasedFramesSourcesCollector` | Нет | Регистрируется при наличии `cameras` | `koin.get<KVBasedFramesSourcesCollector>()` |
| `FramesSourcesCollector` | Нет | Регистрируется при наличии `cameras`; возвращает `KVBasedFramesSourcesCollector` | `koin.get<FramesSourcesCollector>()` |
| `MutableFramesSourcesCollector` | Нет | Регистрируется при наличии `cameras`; возвращает `KVBasedFramesSourcesCollector` | `koin.get<MutableFramesSourcesCollector>()` |

Для получения коллекции нужен импорт `dev.inmo.micro_utils.koin.getAllDistinct`, а для именованных
привязок — `org.koin.core.qualifier.StringQualifier`. Сгенерированный случайный квалификатор намеренно
не является стабильным именем, поэтому его нельзя копировать в прикладной код.

Делегированным определениям нужны неквалифицированные `Json`, `CoroutineScope` и `Database`.
Рекомендуемый JVM-плагин `common/server` предоставляет эти зависимости. При первом получении
`DefaultFramesCollector` также нужен `FramesSourcesCollector`; обычная настройка `cameras`
предоставляет реализацию `KVBasedFramesSourcesCollector`.

Локально объявленных классов с конструкторами нет. Публичные модели кадров, интерфейсы коллекторов и
их реализации поступают из экспортируемого модуля `frames/common`. Без DI их можно создавать напрямую,
например `InMemoryFramesSourcesCollector(connectors, scope)` и
`DefaultFramesCollector(sourceRegistry, scope)`; полный API описан в
[`README` модуля `frames/common`](../common/README.ru.md).

## Конфигураторы и конечные точки

Этот модуль не предоставляет конфигураторов приложения Ktor или маршрутизации. Он устанавливает:

- ни одной конечной точки HTTP;
- ни одной конечной точки WebSocket.

Следовательно, относительных путей конечных точек нет. Отдельно загружаемый плагин `common/server`
устанавливает свою инфраструктуру и маршруты на основе конфигурации; его конфигураторы и относительные
пути описаны в [`README` модуля `common/server`](../../common/server/README.ru.md).
