# Processor Web

[English version](README.md)

Gradle-модуль: `:frameswork.features.processor.web`  
Maven-модуль: `space.kscience:frameswork.features.processor.web`

Этот Kotlin Multiplatform-модуль представляет собой JavaScript-комплект запуска для обработки
кадров. Он экспортирует API модулей [Processor Common](../common/README.ru.md) и
[Common Web](../../common/web/README.ru.md), а его JavaScript-плагин активирует вклад Processor
Common в сериализацию. Собственных веб-процессоров, промежуточных обработчиков, пользовательского
интерфейса или транспортного протокола модуль не объявляет.

## Зависимость и поддерживаемая платформа

Текущая Gradle-конфигурация собирает и публикует только JavaScript-таргет. Добавьте корневую
multiplatform-координату в набор исходников JavaScript; Gradle выберет её `-js`-вариант:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.processor.web:<version>")
        }
    }
}
```

В репозитории присутствуют файлы исходников `JVMPlugin.kt` и `AndroidPlugin.kt`, однако JVM- и
Android-таргеты в этом модуле не объявлены. Поэтому эти файлы не компилируются и не входят в
публикуемый сейчас модуль. Не используйте эту координату как JVM- или Android-зависимость.

## API модуля и запуск

Модуль не объявляет классов обработки. Единственные компилируемые объявления — Kotlin-объекты
плагинов запуска:

- [`Plugin`](src/commonMain/kotlin/Plugin.kt) — общий хук запуска; его методы настройки DI и запуска
  намеренно пусты.
- [`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) делегирует настройку DI и запуск сначала
  `space.kscience.frameswork.features.processor.common.JSPlugin`, а затем пустому локальному
  `Plugin`.

Объекты плагинов не являются определениями Koin и не имеют конструкторов. Обращайтесь к
JavaScript-плагину напрямую, обычно добавляя
`space.kscience.frameswork.features.processor.web.JSPlugin` в список плагинов
`StartLauncherPlugin` приложения:

```kotlin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import space.kscience.frameswork.features.processor.web.JSPlugin

val processorWebPlugin: StartPlugin = JSPlugin
```

Загрузка этого плагина **не** загружает
`space.kscience.frameswork.features.common.web.JSPlugin`. Если приложению нужны общий Ktor
`HttpClient`, веб-конфигураторы или регистрация навигации, этот плагин следует отдельно добавить в
список запуска.

## Внедрение зависимостей

Локальный `Plugin` не добавляет определений Koin. Благодаря делегированию Processor Common
`JSPlugin` добавляет ровно одно определение:

| Зарегистрированный тип | Квалификатор | Точное получение | Назначение |
| --- | --- | --- | --- |
| `SerializersModule` | сгенерированный случайный квалификатор | `koin.getAllDistinct<SerializersModule>()` | Добавляет полиморфные сериализаторы `FramesProcessorMiddleware.Factory.Simple` для базовых типов `Any` и `FramesProcessorMiddleware.Factory`. |

`singleWithRandomQualifier` не предоставляет стабильного квалификатора, поэтому одиночный
неквалифицированный вызов `koin.get<SerializersModule>()` не подходит для получения этого вклада.
Собирайте его вместе с остальными вкладами модулей сериализации:

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import kotlinx.serialization.modules.SerializersModule

val processorSerializers: List<SerializersModule> = koin.getAllDistinct()
```

JavaScript-плагин не регистрирует `FramesProcessor`, `FramesProcessorService`,
`FramesProcessorMiddleware` или фабрики промежуточных обработчиков. В частности,
`koin.get<FramesProcessor>()` недоступен, пока приложение-хост само не добавит это определение.
Экспортируемый из Processor Common сервис можно вместо этого создать напрямую, передав все
обязательные зависимости и настройки:

```kotlin
val processor = FramesProcessorService(
    framesCollector = framesCollector,
    middlewares = middlewares,
    parallelProcessorWorks = 1,
    processingThrottlingMillis = null,
    rejectOldParallelHandling = false,
    scope = scope,
)
```

Либо явно зарегистрируйте экземпляр и его интерфейс в Koin-модуле приложения:

```kotlin
single {
    FramesProcessorService(
        framesCollector = get(),
        middlewares = getAllDistinct(),
        parallelProcessorWorks = 1,
        processingThrottlingMillis = null,
        rejectOldParallelHandling = false,
        scope = get(),
    )
}
single<FramesProcessor> { get<FramesProcessorService>() }
```

Сбор коллекции промежуточных обработчиков в этом примере — соглашение приложения-хоста; сам
веб-модуль не добавляет JavaScript-определений обработчиков. Интерфейсы процессора, поведение
сервиса, требования конструктора и подробности сериализации описаны в документации
[Processor Common](../common/README.ru.md).

## Конфигураторы, маршруты и пути

Это не серверный модуль: он не устанавливает маршруты или конечные точки. Он также не объявляет
HTTP- или WebSocket-конфигураторы и фиксированные клиентские пути. API-зависимость от Common Web
лишь делает типы этого модуля доступными во время компиляции, но не активирует его плагин запуска.
Если приложение отдельно активирует Common Web, передавайте его общему клиенту относительные пути
запросов, как описано в [документации Common Web](../../common/web/README.ru.md).
