# Общая работа с кадрами

Gradle-модуль: `:frameswork.features.frames.common`  
Maven-модуль: `space.kscience:frameswork.features.frames.common`

Этот Kotlin Multiplatform-модуль определяет основные абстракции Frameswork для данных кадра,
источников, коннекторов и коллекторов. Он также предоставляет разделение одного подключения к
источнику между подписчиками, бинарный контейнер кадра и JVM-адаптеры для JavaCV/FFmpeg и Exposed.
Настроенные цели публикации — JVM и JavaScript; Gradle выбирает соответствующий платформенный
вариант из multiplatform-зависимости.

Документация на английском: [README.md](README.md).

## Зависимость

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.frames.common:<version>")
}
```

## Модель кадра и метаданные

- [`FrameData`](src/commonMain/kotlin/models/FrameData.kt) представляет полезную нагрузку вместе с
  `MetaContainer`. `toByteArray()` преобразует нагрузку, а `copyWithModifiedMeta` сохраняет семантику
  нагрузки конкретной реализации и копирует метаданные.
- [`ByteArrayFrameData`](src/commonMain/kotlin/models/ByteArrayFrameData.kt) хранит готовый массив байт
  и возвращает этот же массив без копирования.
- [`FramesSource`](src/commonMain/kotlin/models/FramesSource.kt) создаёт потоки кадров.
  [`OneConnectionFramesSource`](src/commonMain/kotlin/models/OneConnectionFramesSource.kt) разделяет
  один выбранный вышестоящий источник между подписчиками и переключается при смене источника.
- [`FramesSourceId`](src/commonMain/kotlin/models/FramesSourceId.kt) — сериализуемый идентификатор
  источника.
- [`FramesSourceIdMeta`, `FrameSourceWidth`, `FrameSourceHeight`, `FrameReceiveTimestamp` и
  `FrameSourceTimestampMicroseconds`](src/commonMain/kotlin/models/FrameMeta.kt) — сериализуемые ключи
  метаданных для идентификатора источника, размеров, времени получения и метки времени источника.
- [`FrameSourceConnectorConfig`](src/commonMain/kotlin/models/FrameSourceConnectorConfig.kt) воссоздаёт
  [`FrameSourceConnector`](src/commonMain/kotlin/services/FrameSourceConnector.kt), который объединяет
  идентификатор источника, поток кадров и воспроизводимую конфигурацию.

Эти типы модели не имеют отдельных определений Koin. Создавайте значения напрямую, используйте
объекты ключей метаданных напрямую, а коннекторы получайте из `FramesSourcesCollector`.

## Коллекторы

- [`FramesSourcesCollector`](src/commonMain/kotlin/services/FramesSourcesCollector.kt) перечисляет
  идентификаторы источников и предоставляет обновляемый поток коннектора для каждого идентификатора.
- [`MutableFramesSourcesCollector`](src/commonMain/kotlin/services/MutableFramesSourcesCollector.kt)
  добавляет операции вставки и удаления коннекторов.
- [`InMemoryFramesSourcesCollector`](src/commonMain/kotlin/services/InMemoryFramesSourcesCollector.kt)
  последовательно выполняет изменения в coroutine actor и теряет содержимое после завершения процесса.
- [`KVBasedFramesSourcesCollector`](src/commonMain/kotlin/services/KVBasedFramesSourcesCollector.kt)
  сохраняет коннекторы в `KeyValueRepo` и отслеживает обновления репозитория.
- [`FramesCollector`](src/commonMain/kotlin/services/FramesCollector.kt) — реестр потоков кадров для
  потребителей. [`DefaultFramesCollector`](src/commonMain/kotlin/services/DefaultFramesCollector.kt)
  разделяет вышестоящее подключение каждого доступного источника между нижестоящими подписчиками.

## Запуск и DI через Koin

Загрузите [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) или
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) через механизм запуска `StartPlugin` приложения. Обе
платформенные точки входа передают общие регистрации в
[`Plugin`](src/commonMain/kotlin/Plugin.kt).

Общие регистрации:

| Зарегистрированный тип | Квалификатор | Точное получение | Примечание |
| --- | --- | --- | --- |
| `DefaultFramesCollector` | нет | `koin.get<DefaultFramesCollector>()` | Требует `FramesSourcesCollector` без квалификатора и `CoroutineScope` |
| `FramesCollector` | нет | `koin.get<FramesCollector>()` | Возвращает тот же singleton `DefaultFramesCollector` |
| `InMemoryFramesSourcesCollector` | нет | `koin.get<InMemoryFramesSourcesCollector>()` | Начальные коннекторы берутся через `getAllDistinct<FrameSourceConnector>()`; требует `CoroutineScope` |
| `SerializersModule` | случайный, генерируется внутри | `koin.getAllDistinct<SerializersModule>()` | Добавляет сериализаторы для `FramesSourceId` и всех ключей метаданных |

Общий плагин намеренно **не** предоставляет `InMemoryFramesSourcesCollector` как
`FramesSourcesCollector` или `MutableFramesSourcesCollector`. На JS или в собственной общей настройке
добавьте эти привязки интерфейсов, если `DefaultFramesCollector` должен использовать реестр в памяти:

```kotlin
single<FramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }
single<MutableFramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }
```

JVM-плагину дополнительно требуется объект конфигурации верхнего уровня `cameras`, содержащий список
`presets`. При наличии этого блока регистрируются:

| Зарегистрированный тип | Квалификатор | Точное получение |
| --- | --- | --- |
| `JVMPlugin.Config` | нет | `koin.get<JVMPlugin.Config>()` |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `exposed_cameras_collector` | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cameras_collector"))` |
| `KeyValueRepo<FramesSourceId, FrameSourceConnector>` | `exposed_cached_cameras_collector` | `koin.get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cached_cameras_collector"))` |
| `KVBasedFramesSourcesCollector` | нет | `koin.get<KVBasedFramesSourcesCollector>()` |
| `FramesSourcesCollector` | нет | `koin.get<FramesSourcesCollector>()` |
| `MutableFramesSourcesCollector` | нет | `koin.get<MutableFramesSourcesCollector>()` |
| `SerializersModule` | нет | `koin.get<SerializersModule>()` |

Оба интерфейса коллектора источников возвращают один и тот же `KVBasedFramesSourcesCollector`. Его
кешированный репозиторий оборачивает исходный репозиторий Exposed. JVM-модуль без квалификатора
`SerializersModule` содержит регистрации `FFMPEGRTSPConfig`; вызов
`getAllDistinct<SerializersModule>()` возвращает его вместе с общей регистрацией со случайным
квалификатором. Для JVM-настройки приложение или предварительно загруженные плагины должны
предоставить `Json`, `Database` и `CoroutineScope`. Во время запуска конфигурации из `presets`
преобразуются в коннекторы и добавляются, только если их идентификаторы ещё не сохранены.

## JVM-интеграции

- [`FFMPEGRTSPConfig`](src/jvmMain/kotlin/models/FFMPEGRTSPConfig.kt) — полиморфная конфигурация
  `ffmpeg_rtsp`, создающая
  [`FFMPEGRTSPFrameSourceConnector`](src/jvmMain/kotlin/services/connectors/FFMPEGRTSPFrameSourceConnector.kt).
  Коннектор читает RTSP по TCP через JavaCV, преобразует изображения в `BufferedImageFrameData` и
  повторяет неудавшиеся попытки запуска.
- [`BufferedImageFrameData`](src/jvmMain/kotlin/models/javacv/BufferedImageFrameData.kt) оборачивает AWT
  `BufferedImage` и кодирует его в JPEG.
- [`JavaCVFrameData`](src/jvmMain/kotlin/models/javacv/JavaCVFrameData.kt) оборачивает JavaCV `Frame` и
  предоставляет внутренний массив его буфера данных.
- [`ExposedKVCamerasCollectorsRepo`](src/jvmMain/kotlin/services/connectors/ExposedKVCamerasCollectorsRepo.kt)
  создаёт исходный репозиторий Exposed, который использует JVM-плагин. Вызывайте эту функцию напрямую
  только при построении собственной DI-конфигурации.

JVM-классы кадров, конфигурации и коннектора не зарегистрированы как отдельные экземпляры Koin.
Коннекторы создаются из сохранённых или заданных в `presets` конфигураций. Цель JavaScript использует
только общие API модели и коллекторов. Исходный код Android-плагина существует для повторного
использования, но текущая Gradle-конфигурация модуля не публикует Android-цель.

## Бинарный контейнер кадра

[`FrameData.encodeToByteArray`](src/commonMain/kotlin/utils/ByteArrayFrameMetaToByteArray.kt) записывает
четырёхбайтовую длину метаданных в порядке big-endian, закодированный в JSON `MetaContainer` и
полезную нагрузку кадра. `ByteArray.decodeFrameData` выполняет обратное преобразование в
`ByteArrayFrameData`. На обеих сторонах передавайте совместимые экземпляры `Json` с сериализаторами
для всех ключей и значений метаданных. Эти функции расширения вызываются напрямую и не
регистрируются в Koin.

Этот модуль не устанавливает серверные маршруты и не предоставляет HTTP-эндпоинты.
