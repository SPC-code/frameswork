# Общий модуль обработки кадров

Gradle-модуль: `:frameswork.features.processor.common`  
Maven-модуль: `space.kscience:frameswork.features.processor.common`

Этот Kotlin Multiplatform-модуль определяет общие контракты обработки кадров и их стандартную
сервисную реализацию. Процессор может преобразовать отдельный кадр напрямую или предоставить
потоки преобразованных кадров для каждого источника из `FramesCollector`. JVM-вариант также
содержит middleware, сохраняющий буферизованные изображения в JPEG-файлы.

Настроенные цели публикации — JVM и JavaScript. Gradle выбирает подходящий вариант из
multiplatform-зависимости.

Документация на английском: [README.md](README.md).

## Зависимость

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.processor.common:<version>")
}
```

## API обработки

- [`FramesProcessorMiddleware`](src/commonMain/kotlin/services/FramesProcessorMiddleware.kt)
  преобразует одно значение `FrameData`. Полученный результат становится входом следующего
  middleware.
- `FramesProcessorMiddleware.Factory` создаёт middleware, выбираемый по сериализуемому `Id`.
  `Factory.Simple` оборачивает существующий middleware и возвращает тот же экземпляр из
  `createMiddleware()`.
- [`FramesProcessor`](src/commonMain/kotlin/services/FramesProcessor.kt) объединяет контракт
  middleware с `FramesCollector`. Поэтому он поддерживает прямые вызовы `process(frame)`, поиск
  текущего потока по источнику и получение постоянного потока.
- [`FramesProcessorService`](src/commonMain/kotlin/services/FramesProcessorService.kt) — стандартная
  реализация. Она применяет middleware в порядке списка. Если middleware выбрасывает исключение,
  сервис записывает ошибку в журнал, сохраняет последний успешно полученный кадр и продолжает
  обработку следующим middleware.

`allocateFramesFlow(id)` возвращает известный в данный момент подписочно-зависимый поток
обработанных кадров или `null`, если источник недоступен. `allocatePersistentFramesFlow(id)` никогда
не возвращает `null`: пока источника нет, поток ничего не выдаёт, а при появлении или замене
источника автоматически переключается на его поток. `sourcesListUpdatesFlow` процессора содержит
только идентификаторы источников, для которых вышестоящий коллектор вернул поток.

## Создание и выполнение сервиса

Для создания `FramesProcessorService` нужны вышестоящий `FramesCollector`, упорядоченный список
middleware, положительный лимит параллельных работ, необязательный интервал троттлинга, флаг отмены
устаревших работ и владеющий `CoroutineScope`:

```kotlin
val processor = FramesProcessorService(
    framesCollector = collector,
    middlewares = listOf(firstMiddleware, secondMiddleware),
    parallelProcessorWorks = 4,
    processingThrottlingMillis = 30,
    rejectOldParallelHandling = true,
    scope = applicationScope,
)
```

Сервис наблюдает за изменениями списка источников, запрашивает у вышестоящего коллектора поток
каждого текущего источника и строит канал обработки для каждого успешного выделения. Эти каналы
являются наполовину холодными: подписчики одного источника используют общее чтение, а вышестоящий
источник читается только при наличии подписчиков у обработанного потока. При изменении списка
источников старый канал закрывается, а карта строится заново.

Если `processingThrottlingMillis` не равен `null`, сервис хранит последний ожидающий кадр между
попытками запуска, ограничивает активные задачи семафором и отправляет завершённые результаты в
обработанный поток источника. Занятые или вытесненные кадры могут быть отброшены. Значение `0`
сохраняет этот путь выдачи без намеренной задержки. Текущая ветвь для `null` использует немедленный
запуск по мере возможности, но не выдаёт запланированные результаты в выделенный поток источника;
если в этом режиме нужен результат, вызывайте `process` напрямую.

Если включён `rejectOldParallelHandling` и кадры содержат `FrameReceiveTimestamp`, появление более
нового завершённого результата отменяет всё ещё выполняемую старую работу. Переданный scope владеет
наблюдением за источниками и задачами общих потоков; для остановки фоновой работы процессора нужно
отменить этот scope.

## Внедрение зависимостей Koin

Загрузите [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) или
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) через механизм запуска `StartPlugin` приложения. Оба
делегируют общую настройку [`Plugin`](src/commonMain/kotlin/Plugin.kt).

Встроенные регистрации в точности таковы:

| Зарегистрированный тип | Цель | Qualifier | Точное получение | Значение |
| --- | --- | --- | --- | --- |
| `SerializersModule` | JVM и JS | сгенерированный случайный qualifier | `koin.getAllDistinct<SerializersModule>()` | Добавление полиморфного сериализатора `FramesProcessorMiddleware.Factory.Simple` одновременно для `Any` и `FramesProcessorMiddleware.Factory` |
| `FramesProcessorMiddleware` | только JVM | сгенерированный случайный qualifier | `koin.getAllDistinct<FramesProcessorMiddleware>()` | Singleton `BufferedImageSaverMiddleware("./local/")` |
| `FramesProcessorMiddleware.Factory` | только JVM | сгенерированный случайный qualifier | `koin.getAllDistinct<FramesProcessorMiddleware.Factory>()` | Singleton фабрики сохранения с ID `images_saver` и каталогом `./local/` |

`singleWithRandomQualifier` не предоставляет стабильного имени qualifier-а. Импортируйте
`dev.inmo.micro_utils.koin.getAllDistinct` и используйте получение коллекции этих добавлений;
получение одного значения без qualifier-а не является контрактом этих регистраций. JVM-регистрации
middleware и фабрики — разные singleton-ы. Вызов фабрики создаёт новый сохраняющий middleware, а не
возвращает singleton middleware.

Модуль **не** регистрирует `FramesProcessorService` и не связывает `FramesProcessor`: соответствующий
код в общем плагине закомментирован. `FramesProcessorMiddleware.Factory.Id` и `Factory.Simple` —
типы-значения и вспомогательные типы, которые также не имеют отдельных определений Koin. Создайте
процессор напрямую либо добавьте принадлежащие приложению регистрации с подходящими ему параметрами:

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import org.koin.dsl.module

val processorModule = module {
    single {
        FramesProcessorService(
            framesCollector = get(),
            middlewares = getAllDistinct<FramesProcessorMiddleware>(),
            parallelProcessorWorks = 4,
            processingThrottlingMillis = 30,
            rejectOldParallelHandling = true,
            scope = get(),
        )
    }
    single<FramesProcessor> { get<FramesProcessorService>() }
}

val processor = koin.get<FramesProcessor>()
```

Для этого пользовательского определения приложение должно предоставить зависимости
`FramesCollector` и `CoroutineScope` без qualifier-а. На JS нет встроенных добавлений middleware,
поэтому коллекция будет пустой, если приложение или другой функциональный модуль ничего не
зарегистрирует.

## Сохранение изображений на JVM

[`BufferedImageSaverMiddleware`](src/jvmMain/kotlin/services/middlewares/BufferedImageSaverMiddleware.kt)
создаёт каталог назначения при конструировании. Для каждого `BufferedImageFrameData` он записывает
JPEG с именем `HH-mm-ss_dd-MM-yyyy.jpg`, после чего возвращает исходный кадр. Другие реализации
`FrameData` проходят без записи файла. Стандартный путь JVM-плагина `./local/` задаётся относительно
рабочего каталога процесса. Поскольку разрешение имени файла равно одной секунде, несколько кадров,
обработанных в одну секунду, могут указывать на один файл.

Цель JS содержит только общие контракты обработки и сервис. Исходный код Android-плагина также
делегирует общую настройку, однако текущая конфигурация Gradle этого модуля не собирает и не
публикует Android-цель.

Этот модуль не предоставляет конфигураторы Ktor, серверные маршруты или конечные точки HTTP.
