# Сервер панели кадров

[English version](README.md)

- Gradle-модуль: `:frameswork.features.ui.panel.frames.server`
- Мультиплатформенный Maven-модуль: `space.kscience:frameswork.features.ui.panel.frames.server`
- JVM-публикация Maven: `space.kscience:frameswork.features.ui.panel.frames.server-jvm`

Этот Kotlin Multiplatform-модуль реализует серверную сторону контракта
[`FramesDataInfoFeature`](../common/src/commonMain/kotlin/features/FramesDataInfoFeature.kt). Он читает
локальное состояние процессоров и источников кадров и добавляет JVM API Ktor, которым пользуется
клиент панели кадров. Модуль экспортирует API
[`frames panel common`](../common/README.ru.md),
[`common/server`](../../../../common/server/README.ru.md),
[`frames/server`](../../../../frames/server/README.ru.md) и
[`processor/server`](../../../../processor/server/README.ru.md).

## Классы и поведение

| Объявление | Назначение |
| --- | --- |
| [`ServerFramesDataInfoFeature`](src/commonMain/kotlin/features/ServerFramesDataInfoFeature.kt) | Реализует `FramesDataInfoFeature` поверх локального `ProcessorsContainer`. Перечисляет процессоры, читает текущий снимок источников каждого процессора, преобразует кадры без байтового представления и фильтрует кадры по времени получения. |
| [`FramesDataInfoFeatureRoutingsConfigurator`](src/jvmMain/kotlin/configurators/FramesDataInfoFeatureRoutingsConfigurator.kt) | Реализует `ApplicationRoutingConfigurator.Element` и устанавливает две HTTP-точки и одну WebSocket-точку, перечисленные ниже. |

`ServerFramesDataInfoFeature.getFramesFlow` создаёт холодный поток. При начале сбора он разрешает
запрошенный процессор и его постоянный поток источника. Для неизвестного процессора поток не выдаёт
значений и завершается. Для известного процессора постоянный поток не выдаёт значений, пока источник
отсутствует, и начинает следовать за ним после его добавления или замены. Каждый объект, не являющийся
`ByteArrayFrameData`, преобразуется вызовом `toByteArray()`. Кадры без `FrameReceiveTimestamp`
отбрасываются, как и кадры с отметкой времени раньше последней выданной; одинаковые отметки времени
разрешены. Начальное значение для сравнения — `DateTime(0)`.

Конструктору также требуется `FramesSourcesCollector`. Текущая реализация сохраняет эту зависимость,
но получает снимки источников и потоки через `ProcessorsContainer`.

## Запуск и внедрение зависимостей

При использовании загрузчика плагинов на JVM загрузите
[`space.kscience.frameswork.features.ui.panel.frames.server.JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt).
Его `setupDI` вызывает JVM-настройку `frames panel common`, затем настройку общего `Plugin` этого
модуля и после этого добавляет маршруты. Локальные плагины не читают ключи конфигурации.

После выполнения `JVMPlugin.setupDI` модуль добавляет следующие singleton-объекты Koin:

| Тип | Регистрация и квалификатор | Точное получение |
| --- | --- | --- |
| `ServerFramesDataInfoFeature` | Неквалифицированный singleton конкретного класса. | `koin.get<ServerFramesDataInfoFeature>()` |
| `FramesDataInfoFeature` | Неквалифицированный singleton интерфейса, разрешающийся в тот же экземпляр `ServerFramesDataInfoFeature`. | `koin.get<FramesDataInfoFeature>()` |
| `ApplicationRoutingConfigurator.Element` | `FramesDataInfoFeatureRoutingsConfigurator`, зарегистрированный со сгенерированным случайным квалификатором. | `koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()`; из коллекции добавлений выбирают элемент `FramesDataInfoFeatureRoutingsConfigurator`. |

Сгенерированный квалификатор маршрутов намеренно нестабилен и не имеет имени, которым мог бы
пользоваться прикладной код. Конфигуратор не зарегистрирован по своему конкретному типу, поэтому
`koin.get<FramesDataInfoFeatureRoutingsConfigurator>()` — некорректный запрос. Для получения коллекции
добавьте импорт `dev.inmo.micro_utils.koin.getAllDistinct`.

Для разрешения `ServerFramesDataInfoFeature` нужны неквалифицированные определения
`ProcessorsContainer` и `FramesSourcesCollector`. Для разрешения добавления маршрутов также нужны
неквалифицированные `FramesDataInfoFeature` и `Json`. Плагины этого модуля не регистрируют эти
зависимости: приложение должно загрузить подходящие плагины processor, frames и common server/common
либо предоставить эквивалентные определения. Простое добавление Maven-зависимости не запускает
startup-плагин.

Например, после загрузки всех предварительных зависимостей и определений этого модуля:

```kotlin
val implementation = koin.get<ServerFramesDataInfoFeature>()
val feature = koin.get<FramesDataInfoFeature>()
check(feature === implementation)

val routes = koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()
val framesRoutes = routes.filterIsInstance<FramesDataInfoFeatureRoutingsConfigurator>().single()
```

`Plugin` и `JVMPlugin` — объекты запуска, а не определения Koin. У обоих локальных классов есть
открытые конструкторы, поэтому их также можно создать без Koin:

```kotlin
val feature: FramesDataInfoFeature = ServerFramesDataInfoFeature(
    processorsContainer = processorsContainer,
    framesSourcesCollector = framesSourcesCollector,
)
val routes = FramesDataInfoFeatureRoutingsConfigurator(feature, json)
```

## Конфигуратор и конечные точки

`FramesDataInfoFeatureRoutingsConfigurator` — единственный конфигуратор модуля. Он устанавливает все
три операции под `PanelCameraConstants.rootPathPart = "cameras"`:

| Константа | Значение |
| --- | --- |
| `PanelCameraConstants.getAvailableProcessorsPathPart` | `getAvailableProcessors` |
| `PanelCameraConstants.getAvailableCamerasPathPart` | `getAvailableCameras` |
| `PanelCameraConstants.getFramesPathPart` | `getFrames` |

Все пути ниже **относительные**: в них нет схемы, хоста и начального слеша. Родительский маршрут Ktor
может добавить ещё один префикс; в стандартной сборке сервера таким необязательным внешним префиксом
служит `KtorConfig.rootRoute`.

| Метод | Относительный путь | Входные данные | Назначение |
| --- | --- | --- | --- |
| `GET` | `cameras/getAvailableProcessors` | Нет. | Возвращает текущее множество имён процессоров из `FramesDataInfoFeature.getAvailableProcessors()`. |
| `GET` | `cameras/getAvailableCameras` | Обязательный query-параметр `id={processorName}`; `id` — значение `idParameterName`. | Возвращает текущий `Set<FramesSourceId>` процессора или nullable-ответ, если процессор неизвестен. Несмотря на имя пути, значения являются идентификаторами источников кадров. |
| `WebSocket` | `cameras/getFrames` | Описанные ниже текстовые сообщения выбора. | Передаёт закодированные кадры для последнего корректно выбранного процессора и источника. |

Обязательный идентификатор процессора — query-параметр, а не динамический сегмент пути. Поэтому полный
относительный адрес запроса может выглядеть как `cameras/getAvailableCameras?id=main`. Если `id`
отсутствует, `getOrFail` создаёт ошибку, HTTP-представление которой определяется глобальной
конфигурацией Ktor приложения.

### Протокол WebSocket

WebSocket не отправляет кадры, пока клиент не передаст текстовое сообщение с сериализованным
`FramesFlowFeatureId`:

```json
{
  "processorName": "main",
  "framesSourceId": "camera-left"
}
```

Следующее корректное сообщение переключает поток через `flatMapLatest`, отменяя сбор предыдущего
выбора. Некорректный JSON записывается в журнал и игнорируется, а предыдущий выбор не меняется. На
текстовое сообщение в точности `ping` сервер отвечает текстом `pong`. Входящие бинарные кадры и кадры
Close, Ping и Pong не вызывают дополнительных прикладных действий в этом конфигураторе.

Каждый полученный кадр отправляется одним финальным бинарным WebSocket-кадром. Его содержимое создаёт
`FrameData.encodeToByteArray(json)`: четыре байта длины метаданных в формате big-endian, метаданные
JSON в UTF-8 и затем исходные байты кадра. Клиент должен использовать совместимые сериализаторы.

Конфигуратор не добавляет аутентификацию или авторизацию. Для HTTP-сериализации нужна настройка
content negotiation Ktor, а WebSocket-маршруту требуется плагин Ktor WebSockets. В этом модуле нет
других конфигураторов, HTTP-точек, WebSocket-точек или динамических сегментов пути.
