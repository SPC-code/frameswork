# Общие контракты панели кадров

`space.kscience:frameswork.features.ui.panel.frames.common`

Этот Kotlin Multiplatform-модуль содержит общие контракты, которыми пользуются клиенты и серверы
панели кадров. Он определяет интерфейс получения метаданных и потока кадров, сериализуемый выбор
потока и относительные сегменты путей, общие для обеих сторон. В модуле нет пользовательского
интерфейса, реализации транспорта и конфигуратора серверной маршрутизации.

Сейчас модуль публикует варианты для JVM и Kotlin/JS IR. JS-цель поддерживает окружения браузера и
Node.js. Общий API используется обеими целями.

## Зависимость

Используйте корневые multiplatform-координаты, а Gradle выберет вариант для нужной цели:

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.ui.panel.frames.common:<version>")
}
```

## Публичный API

### `FramesDataInfoFeature`

`FramesDataInfoFeature` — независимый от реализации контракт, которым пользуется панель кадров:

- `getAvailableProcessors()` возвращает снимок множества имён процессоров;
- `getAvailableFramesSources(processorName)` возвращает снимок множества идентификаторов источников
  или `null`, если процессор неизвестен;
- `getFramesFlow(processorName, id)` возвращает `Flow<ByteArrayFrameData>` для одного процессора и
  источника. Совместное использование потока, переподключение и поведение при недоступном источнике
  определяются реализацией.

Этот модуль предоставляет интерфейс, но не его реализацию.

### `FramesFlowFeatureId`

`FramesFlowFeatureId` — сериализуемая пара из `processorName` и `FramesSourceId`. Удалённые реализации
используют её для указания потока, который запросил клиент. Создавайте значение напрямую:

```kotlin
val streamId = FramesFlowFeatureId(
    processorName = "main",
    framesSourceId = FramesSourceId("camera-1"),
)
```

### `PanelCameraConstants`

`PanelCameraConstants` — объект Kotlin. Обращайтесь к нему напрямую: это не DI-сервис. Все значения —
относительные сегменты путей без начальных и конечных косых черт.

| Константа | Значение | Составной относительный путь | Назначение |
| --- | --- | --- | --- |
| `rootPathPart` | `cameras` | `cameras` | Группирует операции панели кадров. |
| `getAvailableProcessorsPathPart` | `getAvailableProcessors` | `cameras/getAvailableProcessors` | Возвращает имена процессоров. |
| `getAvailableCamerasPathPart` | `getAvailableCameras` | `cameras/getAvailableCameras` | Возвращает источники кадров процессора. |
| `getFramesPathPart` | `getFrames` | `cameras/getFrames` | Обозначает соединение для передачи потока кадров. |

Эти константы описывают только общий сетевой контракт. Данный модуль не подключает конечные точки:
HTTP/WebSocket-методы, параметры и возможный префикс развёртывания задаёт серверный модуль.

## Внедрение зависимостей

Метод `Plugin.setupDI` этого модуля пуст. Стартовые адаптеры JVM и JS делегируют настройку этому
пустому методу, поэтому подключение модуля не регистрирует определения Koin и не объявляет
квалификаторы. Следовательно:

- создавайте `FramesFlowFeatureId` напрямую;
- обращайтесь к `PanelCameraConstants` напрямую;
- получайте `FramesDataInfoFeature` только после того, как приложение или модуль с реализацией
  зарегистрирует этот интерфейс.

Например, приложение может зарегистрировать собственную реализацию без квалификатора и получить её
по типу интерфейса:

```kotlin
val featureModule = module {
    single<FramesDataInfoFeature> { MyFramesDataInfoFeature(/* зависимости */) }
}

val feature: FramesDataInfoFeature = koin.get()
```

Регистрация из этого примера принадлежит приложению, а не данному модулю. Если приложение использует
квалификатор, при получении сервиса нужно передать тот же квалификатор, определённый приложением;
этот модуль не определяет ни одного.
