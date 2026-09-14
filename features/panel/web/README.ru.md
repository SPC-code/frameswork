# Веб-клиент панели

Gradle-модуль: `:frameswork.features.panel.web`  
Maven-модуль: `space.kscience:frameswork.features.panel.web`

Этот Kotlin Multiplatform-модуль предоставляет для веб-клиентов HTTP-реализацию общего контракта
[`PanelFeature`](../common/src/commonMain/kotlin/PanelFeature.kt). Она использует Ktor `HttpClient`,
чтобы читать текущую и стандартную раскладки панели и отправлять раскладку на замену.

Документация на английском: [README.md](README.md).

## Зависимость

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.panel.web:<version>")
}
```

Модуль транзитивно экспортирует API модулей panel-common и common-web.

## Основной класс и поведение

[`KtorPanelFeature`](src/commonMain/kotlin/KtorPanelFeature.kt) реализует `PanelFeature` с помощью трёх
HTTP-запросов:

- `getPanelConfig()` читает текущую раскладку;
- `getDefaultConfig()` читает стандартную раскладку;
- `setPanelConfig(config)` отправляет полную замену и возвращает полученный от сервера результат
  типа `Boolean`.

При чтении ответ, тело которого в точности равно JSON-литералу `null`, преобразуется в Kotlin
`null`. Любой другой ответ декодируется как `PanelInfo`. При записи `PanelInfo` сериализуется в тело
запроса, а ответ декодируется как `Boolean`. Класс не перехватывает ошибки транспорта, HTTP-клиента
или сериализации.

Поэтому переданному `HttpClient` нужны подходящий базовый URL, согласование содержимого JSON и JSON
в качестве типа содержимого запроса. `SerializationConfigurator` и
`DefaultUrlHttpClientConfigurator` из common-web настраивают эти части при загрузке плагина
common-web.

## Внедрение зависимостей через Koin

Общий [`Plugin`](src/commonMain/kotlin/Plugin.kt) регистрирует ровно два singleton-определения без
квалификаторов:

```kotlin
single { KtorPanelFeature(get()) }
single<PanelFeature> { get<KtorPanelFeature>() }
```

Следовательно, Koin уже должен содержать **неквалифицированный** `HttpClient`: при разрешении
зависимости конструктора не используется ни именованный, ни какой-либо другой квалификатор. Сам
плагин не создаёт `HttpClient`. После загрузки `setupDI` плагина оба типа извлекаются без
квалификатора:

```kotlin
val ktorPanelFeature: KtorPanelFeature = koin.get()
val panelFeature: PanelFeature = koin.get()

check(panelFeature === ktorPanelFeature)
```

Определение `PanelFeature` разрешает singleton `KtorPanelFeature`, поэтому оба запроса возвращают
один и тот же объект. При загрузке [`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) настройка и запуск
делегируются плагину panel-common, после чего устанавливаются эти определения panel-web. При этом
определение `HttpClient` из common-web всё равно не устанавливается. Загрузите стартовый плагин
common-web как часть приложения либо предоставьте настроенный клиент самостоятельно:

```kotlin
val dependencies = module {
    single<HttpClient> { configuredHttpClient }
}
```

Если внедрение зависимостей не требуется, реализацию можно создать напрямую. Такой способ обходит
все регистрации и квалификаторы Koin:

```kotlin
val panelFeature: PanelFeature = KtorPanelFeature(configuredHttpClient)
```

Не загружайте другую неквалифицированную реализацию `PanelFeature` в ту же область Koin, если только
приложение намеренно не определяет способ обработки дублирующихся определений.

## Относительные HTTP-пути

Во всех запросах используются константы путей из модуля panel-common. Указанные ниже строки
относительные: в них нет схемы, хоста или начального слеша, и их разрешает настроенный `HttpClient`.

| Метод | Относительный путь | Операция клиента | Назначение и ответ |
| --- | --- | --- | --- |
| `GET` | `panel/get` | `getPanelConfig()` | Прочитать текущий `PanelInfo` или литерал `null`, если он отсутствует. |
| `GET` | `panel/default` | `getDefaultConfig()` | Прочитать стандартный `PanelInfo` или литерал `null`, если он отсутствует. |
| `POST` | `panel/set` | `setPanelConfig(config)` | Отправить тело `PanelInfo` и декодировать результат типа `Boolean`. |

Этот клиентский модуль не устанавливает серверный конфигуратор маршрутизации и не предоставляет
собственных серверных конечных точек. Он не настраивает WebSocket-пути.
