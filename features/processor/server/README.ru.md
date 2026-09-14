# Серверный модуль Processor

[English version](README.md)

- Gradle-модуль: `:frameswork.features.processor.server`
- Мультиплатформенный Maven-модуль: `space.kscience:frameswork.features.processor.server`
- JVM-публикация Maven: `space.kscience:frameswork.features.processor.server-jvm`

Этот Kotlin Multiplatform-модуль собирает именованные конвейеры обработки кадров для JVM-сервера.
Он экспортирует API [`processor/common`](../common/README.ru.md) и инфраструктуру
[`common/server`](../../common/server/README.ru.md), предоставляет контейнер настроенных процессоров и
добавляет JVM-middleware для обрезки изображений.

## Классы

| Объявление | Назначение |
| --- | --- |
| `Plugin.Config` | Сериализуемая корневая конфигурация с картой именованных определений процессоров. |
| [`ProcessorsContainer`](src/commonMain/kotlin/ProcessorsContainer.kt) | Создаёт по одному `FramesProcessorService` для каждой записи конфигурации `processors` и предоставляет инициализированные процессоры по идентификатору. |
| [`ProcessorsContainer.ProcessorConfig`](src/commonMain/kotlin/ProcessorsContainer.kt) | Описывает упорядоченную цепочку middleware, параллелизм, ограничение частоты и отмену обработки устаревших кадров для одного процессора. |
| [`FrameCroppingMiddleware`](src/jvmMain/kotlin/processor_middleware/FrameCroppingMiddleware.kt) | Обрезает кадр до заданного прямоугольника, возвращает `BufferedImageFrameData` и сохраняет метаданные кадра. |
| [`FrameCroppingMiddleware.CropData`](src/jvmMain/kotlin/processor_middleware/FrameCroppingMiddleware.kt) | Сериализуемый прямоугольник обрезки (`x`, `y`, `width` и `height`) в пикселях от левого верхнего угла изображения. |
| [`FrameCroppingMiddleware.Factory`](src/jvmMain/kotlin/processor_middleware/FrameCroppingMiddleware.kt) | Создаёт middleware обрезки с идентификатором для процессоров `crop_<suffix>`. |

Каждую строку в списке `middlewares` процессора `ProcessorsContainer` сначала сопоставляет с
зарегистрированным `FramesProcessorMiddleware.Factory.id`. Если подходящей фабрики нет, строка
считается идентификатором другого настроенного процессора, благодаря чему процессоры можно
компоновать. Ссылки на middleware вычисляются в порядке списка.

Контейнер инициализирует карту процессоров в переданном `CoroutineScope`. Методы
`availableProcessors()` и `getProcessor(id)` приостанавливаются, пока карта не будет готова.

## Запуск и конфигурация

При использовании загрузчика запуска следует загрузить
`space.kscience.frameswork.features.processor.server.JVMPlugin`. При настройке DI и запуске он
делегирует `space.kscience.frameswork.features.processor.common.JVMPlugin`, а затем общему `Plugin`
этого модуля. `Plugin` и `JVMPlugin` — объекты Kotlin (`object`); на них ссылаются напрямую, а не
получают из Koin.

На практике объект верхнего уровня `processors` обязателен, поскольку `ProcessorsContainer`
является singleton-объектом, создаваемым при запуске. Объект `middlewares.crops` необязателен. Каждая
запись внутри `crops` создаёт фабрику, идентификатор которой состоит из префикса `crop_` и имени
записи:

```json
{
  "middlewares": {
    "crops": {
      "preview": {
        "x": 40,
        "y": 20,
        "width": 640,
        "height": 360
      }
    }
  },
  "processors": {
    "preview": {
      "middlewares": ["crop_preview"],
      "parallelProcessing": 8,
      "processingThrottlingMillis": 30,
      "rejectOldParallelHandling": false
    }
  }
}
```

Поле `middlewares` обязательно для каждого процессора, а `parallelProcessing` должно быть
положительным. Остальные поля процессора имеют значения по умолчанию, показанные в примере. Значение
`null` для `processingThrottlingMillis` отключает ограничение по времени. В текущей реализации
`FramesProcessorService` эта ветвь выполняет запуск по мере возможности, но не выдаёт завершённые
кадры в выделенные потоки источников; потребителям потоков следует оставить ненулевое значение, а
когда результат нужен напрямую — вызвать `process(frame)`. Координаты и размеры обрезки должны
задавать допустимый прямоугольник внутри каждого входного изображения.

Сервер также должен предоставить неквалифицированные `Json`, `CoroutineScope` и `FramesCollector`,
необходимые этим определениям. Обычное приложение загружает JVM-плагин `common/server` для `Json` и
области корутин, а JVM-плагин `frames/common` с его конфигурацией `cameras` — для коллектора до
данного плагина. Приложение может предоставить эквивалентные привязки самостоятельно. Одной
зависимости от этих артефактов недостаточно для запуска их плагинов.

## Внедрение зависимостей и прямое создание

Вызов `JVMPlugin.setupDI` создаёт следующие локальные определения:

| Тип | Регистрация и квалификатор | Получение |
| --- | --- | --- |
| `Plugin.Config` | Неквалифицированный singleton, регистрируемый только при наличии ключа верхнего уровня `processors`. Декодируется из полной корневой конфигурации. | `koin.get<Plugin.Config>()` |
| `ProcessorsContainer` | Неквалифицированный singleton с `createdAtStart = true`; его определение добавляется всегда. | `koin.get<ProcessorsContainer>()` |
| `FramesProcessorMiddleware.Factory` | По одному singleton для каждой записи `middlewares.crops`; каждый зарегистрирован со сгенерированным случайным квалификатором и реализован через `FrameCroppingMiddleware.Factory`. | `koin.getAllDistinct<FramesProcessorMiddleware.Factory>()`; выбор выполняется по `factory.id`, например `crop_preview`. |

Для получения коллекции нужен импорт `dev.inmo.micro_utils.koin.getAllDistinct`. Случайные
квалификаторы генерируются при регистрации и не являются стабильными прикладными именами. Поэтому
фабрики обрезки нельзя получать по скопированному квалификатору или через
`koin.get<FrameCroppingMiddleware.Factory>()`.

Делегированный JVM-плагин `processor/common` также добавляет свой модуль сериализаторов,
`BufferedImageSaverMiddleware` по умолчанию и его фабрику `images_saver`. Эти добавления в коллекции
описаны в [`README` модуля `processor/common`](../common/README.ru.md).

Все объявленные в модуле классы можно создать напрямую, без Koin:

```kotlin
val crop = FrameCroppingMiddleware(
    FrameCroppingMiddleware.CropData(x = 40, y = 20, width = 640, height = 360)
)
val factory = FrameCroppingMiddleware.Factory(
    FrameCroppingMiddleware.CropData(x = 40, y = 20, width = 640, height = 360),
    suffix = "preview"
)
val container = ProcessorsContainer(
    framesCollector = framesCollector,
    scope = scope,
    processorsConfigs = mapOf(
        "preview" to ProcessorsContainer.ProcessorConfig(
            middlewares = listOf("crop_preview")
        )
    ),
    middlewaresFactories = listOf(factory)
)
```

Созданные напрямую middleware и фабрики обрезки не зависят от DI. Созданный напрямую контейнер всё
равно инициализируется асинхронно в переданном scope.

## Конфигураторы и конечные точки

Этот модуль не предоставляет конфигураторов приложения Ktor или маршрутизации. Он устанавливает:

- ни одной конечной точки HTTP;
- ни одной конечной точки WebSocket.

Поэтому относительных путей конечных точек нет. Слово `server` в названии означает серверную сборку
процессоров, а не HTTP API. Другие серверные функциональные модули могут получать
неквалифицированный `ProcessorsContainer` и публиковать его процессоры через собственные
относительные маршруты.
