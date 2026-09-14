# Frames Web

[English version](README.md)

Gradle-модуль: `:frameswork.features.frames.web`  
Maven-модуль: `space.kscience:frameswork.features.frames.web`

Этот Kotlin Multiplatform-модуль служит JavaScript-набором запуска для API предметной области
кадров. Он экспортирует [`frameswork.features.frames.common`](../common/README.ru.md) и
[`frameswork.features.common.web`](../../common/web/README.ru.md) как API-зависимости и
предоставляет JavaScript-плагин `StartPlugin`, который устанавливает регистрации общего модуля
кадров. Собственных веб-моделей кадров, сервисов, UI, конфигурации HTTP-клиента или транспортного
протокола модуль не добавляет.

## Зависимость и поддерживаемая платформа

Текущая конфигурация Gradle собирает и публикует только JavaScript-цель. Добавьте корневую
multiplatform-координату в JavaScript source set; Gradle выберет её `-js` вариант:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.frames.web:<version>")
        }
    }
}
```

В репозитории есть исходные файлы `JVMPlugin.kt` и `AndroidPlugin.kt`, но этот модуль не объявляет
JVM- или Android-цели. Поэтому эти файлы не компилируются и не входят в публикуемый сейчас модуль.
Не используйте эту координату как JVM- или Android-зависимость.

## API модуля и запуск

Модуль не объявляет классов предметной области. Его единственные компилируемые объявления —
Kotlin-объекты плагинов:

- [`Plugin`](src/commonMain/kotlin/Plugin.kt) — общий обработчик запуска. Его методы настройки DI и
  запуска намеренно пусты.
- [`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) делегирует настройку DI и запуск объекту
  `space.kscience.frameswork.features.frames.common.JSPlugin`, а затем пустому локальному `Plugin`.

Объекты плагинов не являются определениями Koin и не имеют конструкторов. Обращайтесь к ним
напрямую; обычно
`space.kscience.frameswork.features.frames.web.JSPlugin` добавляют в список плагинов
`StartLauncherPlugin` приложения.

```kotlin
val frameWebPlugin: StartPlugin =
    space.kscience.frameswork.features.frames.web.JSPlugin
```

Загрузка этого плагина **не** загружает
`space.kscience.frameswork.features.common.web.JSPlugin`. Приложение, которому нужен общий Ktor
`HttpClient`, должно отдельно добавить этот плагин в список запуска. Кроме того, до получения
коллекторов кадров хост должен предоставить общие регистрации приложения, включая
`CoroutineScope` без qualifier-а; стандартный вариант —
`space.kscience.frameswork.features.common.common.JSPlugin`.

## Регистрации Koin

Локальный общий плагин не добавляет определений Koin. Благодаря делегированию JavaScript-плагину
общего модуля кадров `JSPlugin` добавляет следующие определения:

| Зарегистрированный тип | Qualifier | Точное получение | Требование или результат |
| --- | --- | --- | --- |
| `DefaultFramesCollector` | нет | `koin.get<DefaultFramesCollector>()` | Требуются определения `FramesSourcesCollector` и `CoroutineScope` без qualifier-а. |
| `FramesCollector` | нет | `koin.get<FramesCollector>()` | Возвращает тот же singleton `DefaultFramesCollector` и имеет те же требования. |
| `InMemoryFramesSourcesCollector` | нет | `koin.get<InMemoryFramesSourcesCollector>()` | Требуется `CoroutineScope` без qualifier-а; начальные коннекторы берутся из всех различных вкладов `FrameSourceConnector`. |
| `SerializersModule` | сгенерированный случайный qualifier | `koin.getAllDistinct<SerializersModule>()` | Добавляет сериализаторы идентификаторов источников кадров и ключей метаданных кадров; стабильного qualifier-а для одиночного получения нет. |

Делегированный плагин намеренно не связывает `InMemoryFramesSourcesCollector` с
`FramesSourcesCollector` или `MutableFramesSourcesCollector`. Если реализация в памяти должна быть
основой пользовательского `FramesCollector`, добавьте привязки интерфейсов до его получения:

```kotlin
single<FramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }
single<MutableFramesSourcesCollector> { get<InMemoryFramesSourcesCollector>() }

val sources: FramesSourcesCollector = koin.get()
val mutableSources: MutableFramesSourcesCollector = koin.get()
val frames: FramesCollector = koin.get()
```

Экспортируемые реализации кадров можно также создать без Koin:

```kotlin
val sources = InMemoryFramesSourcesCollector(
    tmpPreset = connectors,
    scope = scope,
)
val frames = DefaultFramesCollector(
    framesCollector = sources,
    scope = scope,
)
```

Модель кадров, API коннекторов и коллекторов, а также все требования к созданию описаны в
[документации Frames Common](../common/README.ru.md). Отдельно активируемые регистрации
`HttpClient` и навигации описаны в [Common Web](../../common/web/README.ru.md).

## Маршруты и пути клиента

Это не серверный модуль: он не устанавливает маршрутов и не предоставляет endpoints. Он также не
определяет конфигураторов HTTP или WebSocket и фиксированных путей клиента. API-зависимость от
Common Web лишь делает типы этого модуля доступными во время компиляции, но не активирует его
клиентский плагин.
