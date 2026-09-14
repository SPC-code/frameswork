# Общая серверная инфраструктура Frameswork

[English version](README.md)

Gradle-модуль: `:frameswork.features.common.server`  
Мультиплатформенный Maven-модуль: `space.kscience:frameswork.features.common.server`  
JVM-публикация Maven: `space.kscience:frameswork.features.common.server-jvm`

Этот модуль предоставляет общую JVM-инфраструктуру сервера для функциональных модулей Frameswork. Он
создаёт встроенный Netty-сервер, устанавливает общие компоненты Ktor, собирает расширения аутентификации
и маршрутизации из Koin, публикует настроенные каталоги статических файлов и предоставляет глобальный
репозиторий «ключ — значение» на PostgreSQL с полным кэшем в памяти.

Точка запуска — `space.kscience.frameswork.features.common.server.JVMPlugin`. При настройке DI она
также запускает настройку общей функциональности, поэтому неквалифицированные `Json` и
`CoroutineScope`, используемые ниже, поступают из `frameswork.features.common.common`.

[`Plugin`](src/commonMain/kotlin/Plugin.kt) — общая мультиплатформенная точка запуска; сейчас она не
добавляет привязок и не выполняет действий при запуске. [`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt)
добавляет описанные здесь JVM-привязки и запускает встроенный сервер.

## Модели конфигурации

Оба объекта конфигурации декодируются из одного и того же стартового JSON-объекта. Общий экземпляр
`Json` допускает неизвестные ключи, а две точные, чувствительные к регистру строки-квалификатора Koin
разделяют модели.

| Модель | Назначение | Получение из DI |
| --- | --- | --- |
| [`Config`](src/jvmMain/kotlin/models/Config.kt) | Настройки базы данных, публичной адресации и сопоставления статических каталогов. JSON-ключ свойства `databaseConfig` — `database`. | `koin.get<Config>(StringQualifier("ConfigJsonQualifier"))` |
| [`KtorConfig`](src/jvmMain/kotlin/models/KtorConfig.kt) | Фактические `host` и `port`, на которых слушает Netty, а также необязательный `rootRoute` для добавляемых маршрутов. | `koin.get<KtorConfig>(StringQualifier("KtorConfigJsonQualifier"))` |
| [`DatabaseConfig`](src/jvmMain/kotlin/models/DatabaseConfig.kt) | JDBC URL, драйвер PostgreSQL, учётные данные и полученный дескриптор Exposed `Database`. | Вложен в квалифицированный `Config`; его `Database` также доступен без квалификатора, как описано ниже. |

У всех трёх data-классов есть открытые конструкторы, поэтому без стартового DI их можно создавать
напрямую. `DatabaseConfig` создаёт свойство `Database` из параметров подключения. В том же исходном
файле объявлены `defaultDatabaseParamsName` (`"defaultDatabase"`) и nullable-аксессор
`Map<String, Any>.database` для карт, в которых `DatabaseConfig` хранится под этим ключом.

Сервер читает `host` и `port` слушателя из `KtorConfig`, а не из `Config`. Поля `wss` в этом модуле
только хранят данные конфигурации и не включают TLS. На JVM `isInDebugMode` равен `true`, только если
переменная окружения `DEBUG` без учёта регистра равна `true`. Режим отладки включает development mode
Ktor и уровень журналирования вызовов `TRACE`; в обычном режиме используется `WARN`.

## Привязки DI

После выполнения `JVMPlugin.setupDI` следующие определения являются singleton-объектами Koin:

| Привязка | Как получить | Примечание |
| --- | --- | --- |
| `Config` | `koin.get<Config>(StringQualifier("ConfigJsonQualifier"))` | Декодируется из стартового JSON. |
| `KtorConfig` | `koin.get<KtorConfig>(StringQualifier("KtorConfigJsonQualifier"))` | Декодируется из того же стартового JSON. |
| `Database` | `koin.get<Database>()` | Дескриптор из квалифицированного `Config.databaseConfig`. |
| `VersionsRepo<Database>` | `koin.get<VersionsRepo<Database>>()` | Хранит версии таблиц в `tables_versions`. |
| `InternalApplicationRoutingConfigurator` | `koin.get<InternalApplicationRoutingConfigurator>()` | Неквалифицированная привязка конкретного типа; запускается отдельно после общих конфигураторов. |
| `EmbeddedServer<*, *>` | `koin.get<EmbeddedServer<*, *>>()` | Настроенный Netty-сервер. Стартовый плагин запускает его в блокирующем режиме. |
| `ExposedGlobalKVRepo` | `koin.get<ExposedGlobalKVRepo>()` | Персистентная реализация репозитория. |
| `FullyCachedGlobalKVRepo` | `koin.get<FullyCachedGlobalKVRepo>()` | Полный кэш в памяти поверх `ExposedGlobalKVRepo`. |
| `GlobalKVRepo` | `koin.get<GlobalKVRepo>()` | Возвращает тот же кэширующий репозиторий. Это предпочтительная привязка для прикладного кода. |
| Все общие конфигураторы Ktor | `koin.getAllDistinct<KtorApplicationConfigurator>()` | Каждый зарегистрирован как `KtorApplicationConfigurator` со сгенерированным случайным квалификатором. |
| Все добавления маршрутов | `koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()` | Включают добавление статических файлов из этого модуля и добавления функциональных модулей. |

Для квалифицированного и группового поиска нужны следующие импорты:

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import org.koin.core.qualifier.StringQualifier
```

Случайные квалификаторы намеренно не являются стабильными именами для поиска. Поэтому
`ApplicationAuthenticationConfigurator`, `ContentNegotiationKtorApplicationConfigurator` и
`GZipConfigurator` получают из DI в составе коллекции `KtorApplicationConfigurator`, а не по их
конкретным типам. `StatusPagesConfigurator`, `ApplicationCachingHeadersConfigurator` и
`ApplicationSessionsConfigurator` из MicroUtils регистрируются в той же коллекции.

`WebSocketsConfiguration` не зарегистрирован в Koin. Сервер создаёт его напрямую с
неквалифицированным `Json` и применяет до всех DI-конфигураторов. Локальные классы-конфигураторы также
имеют открытые конструкторы и могут использоваться явно:

```kotlin
val authentication = ApplicationAuthenticationConfigurator(authenticationElements)
val contentNegotiation = ContentNegotiationKtorApplicationConfigurator(json)
val gzip = GZipConfigurator()
val routing = InternalApplicationRoutingConfigurator(routeElements, rootPath = "api")
val webSockets = WebSocketsConfiguration(json)
```

Расширения аутентификации собираются как экземпляры
`ApplicationAuthenticationConfigurator.Element`. Этот модуль не регистрирует таких элементов;
функциональные модули могут добавить несколько элементов через `singleWithRandomQualifier`.

## Репозитории

[`GlobalKVRepo`](src/commonMain/kotlin/repos/GlobalKVRepo.kt) — глобальный прикладной репозиторий строковых
ключей и значений. [`ExposedGlobalKVRepo`](src/jvmMain/kotlin/repos/ExposedGlobalKVRepo.kt) хранит его
данные в таблице `globals` с текстовыми столбцами `key` и `value`.

[`FullyCachedGlobalKVRepo`](src/commonMain/kotlin/repos/FullyCachedGlobalKVRepo.kt) оборачивает этот
персистентный репозиторий в `FullKeyValueCacheRepo` с кэшем `MapKeyValueRepo`. Переданный прикладной
`CoroutineScope` выполняет первоначальное заполнение кэша. Записи проходят через персистентный
репозиторий и обновляют кэш. Прикладному коду обычно следует запрашивать `GlobalKVRepo`, а служебный
код, которому нужен именно персистентный слой, может запросить `ExposedGlobalKVRepo`.

## Конфигураторы и конечные точки

Все пути ниже **относительные**: в них нет схемы, хоста и начального слеша. Значения из конфигурации
могут содержать слеши, но приведённые обозначения нормализованы. `rootRoute` необязателен.

| Конфигуратор или установщик маршрута | Устанавливаемые HTTP/WebSocket-точки | Назначение |
| --- | --- | --- |
| [`ApplicationAuthenticationConfigurator`](src/jvmMain/kotlin/configurators/ApplicationAuthenticationConfigurator.kt) | Нет. | Устанавливает провайдеры аутентификации, добавленные как `ApplicationAuthenticationConfigurator.Element`; маршруты сами выбирают, использовать ли их. |
| [`ContentNegotiationKtorApplicationConfigurator`](src/jvmMain/kotlin/configurators/ContentNegotiationKtorApplicationConfigurator.kt) | Нет. | Устанавливает преобразование JSON для запросов и ответов с общим экземпляром `Json`. |
| [`GZipConfigurator`](src/jvmMain/kotlin/configurators/GZipConfigurator.kt) | Нет. | Включает gzip для подходящих ответов размером не менее 1 024 байт. |
| [`WebSocketsConfiguration`](src/jvmMain/kotlin/configurators/WebSocketsConfiguration.kt) | WebSocket-точек нет. | Устанавливает поддержку протокола WebSocket и преобразование JSON-фреймов; фактические сокеты задают элементы маршрутов функциональных модулей. |
| [`InternalApplicationRoutingConfigurator`](src/jvmMain/kotlin/configurators/InternalApplicationRoutingConfigurator.kt) | Собственных фиксированных точек нет. Он устанавливает всё, что объявляют `ApplicationRoutingConfigurator.Element`, под `rootRoute`, если тот задан. | Объединяет все добавления маршрутов из DI в маршрутизацию Ktor. |
| Элемент статических файлов `ApplicationRoutingConfigurator.Element` в `JVMPlugin` | `GET [rootRoute/]<static-path>/{file...}` для каждой записи `Config.staticFolders`. Если путь означает корень маршрутизации, относительная форма — `[rootRoute/]{file...}`. | Рекурсивно публикует файлы из сопоставленного локального каталога. Для запроса каталога используется `index.html`, отсутствующий ресурс даёт `404`. При пустом `staticFolders` точек нет. |
| `StatusPagesConfigurator` из MicroUtils | Нет. | Применяет добавленные обработчики статусных страниц. |
| `ApplicationCachingHeadersConfigurator` из MicroUtils | Нет. | Применяет добавленные правила заголовков кэширования. |
| `ApplicationSessionsConfigurator` из MicroUtils | Нет. | Применяет добавленную настройку сессий. |

Таким образом, модуль не задаёт фиксированных REST- или WebSocket-точек. Единственный конкретный
HTTP-маршрут — настраиваемый `GET` статических файлов; все остальные прикладные точки поступают из
элементов маршрутов функциональных модулей. Например, при `rootRoute = "api"` и сопоставлении
`"assets" -> "./public"` файл доступен по относительному пути `api/assets/{file...}`.
