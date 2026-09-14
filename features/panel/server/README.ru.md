# Сервер панели

[English version](README.md)

- Gradle-модуль: `:frameswork.features.panel.server`
- Мультиплатформенный Maven-модуль: `space.kscience:frameswork.features.panel.server`
- JVM-публикация Maven: `space.kscience:frameswork.features.panel.server-jvm`

Этот Kotlin Multiplatform-модуль предоставляет серверные реализации API
[`PanelFeature`](../common/src/commonMain/kotlin/PanelFeature.kt), а на JVM — добавление маршрутов Ktor
для чтения и изменения компоновки панели. Он экспортирует как API
[`panel/common`](../common/README.ru.md) и инфраструктуру
[`common/server`](../../common/server/README.ru.md).

## Классы и поведение

| Объявление | Назначение |
| --- | --- |
| [`InMemoryPanelFeature`](src/commonMain/kotlin/InMemoryPanelFeature.kt) | Хранит текущий `PanelInfo` в одном процессе. Начальное текущее значение равно `null`; отдельно переданное значение по умолчанию не используется как автоматическая замена. Корутины читают и изменяют текущее значение под мьютексом. |
| [`GlobalKVBindedPanelFeature`](src/commonMain/kotlin/GlobalKVBindedPanelFeature.kt) | Кодирует текущий `PanelInfo` общим экземпляром `Json` и хранит его в `GlobalKVRepo` под фиксированным ключом `panel_config`. Значение по умолчанию хранится отдельно. Запись считается успешной, только если прочитанное обратно значение равно переданному. |
| [`PanelRoutingsConfigurator`](src/jvmMain/kotlin/configurators/PanelRoutingsConfigurator.kt) | Реализует `ApplicationRoutingConfigurator.Element` и устанавливает три перечисленных ниже HTTP-маршрута панели. |
| [`Plugin.DefaultPanelInfoConfig`](src/commonMain/kotlin/Plugin.kt) | Сериализуемая обёртка необязательной конфигурации панели по умолчанию. Её JSON-свойство называется `default`. |

Обе реализации функциональности возвращают `null` из `getPanelConfig()`, пока текущего значения нет,
а переданный конструктору `defaultPanel` — только из `getDefaultConfig()`. Значение по умолчанию не
копируется в текущее хранилище. `GlobalKVBindedPanelFeature` не перехватывает ошибки репозитория и
сериализации; некорректный JSON под ключом `panel_config` не преобразуется в `null`.

## Запуск и конфигурация

При использовании загрузчика плагинов на JVM загрузите
[`space.kscience.frameswork.features.panel.server.JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt). Сначала
он выполняет JVM-настройку `panel/common`, затем настройку общего `Plugin` этого модуля и после этого
регистрирует добавление маршрутов. Делегированные плагины `panel/common` сейчас не добавляют
определений Koin.

Необязательный объект верхнего уровня `panel_info` декодируется как `Plugin.DefaultPanelInfoConfig`.
Например:

```json
{
  "panel_info": {
    "default": {
      "horizontalSlots": 3,
      "verticalSlots": 2,
      "items": []
    }
  }
}
```

Если `panel_info` отсутствует, обе реализации получают `null` как значение по умолчанию, а определения
`Plugin.DefaultPanelInfoConfig` в Koin нет. Если передать `panel_info` без корректного `default`
типа `PanelInfo`, разрешение singleton-объекта конфигурации завершится ошибкой.

Этот модуль **не** запускает `space.kscience.frameswork.features.common.server.JVMPlugin`. Полный
сервер обычно загружает тот плагин отдельно: он предоставляет неквалифицированные `Json` и
`GlobalKVRepo`, собирает элемент маршрутов, при необходимости применяет `KtorConfig.rootRoute` и
запускает Ktor. Простое добавление Maven-зависимости не запускает ни один из плагинов.

## Внедрение зависимостей

После выполнения `JVMPlugin.setupDI` модуль добавляет следующие singleton-объекты Koin:

| Тип | Регистрация и квалификатор | Точное получение |
| --- | --- | --- |
| `Plugin.DefaultPanelInfoConfig` | Без квалификатора; регистрируется, только когда есть ключ верхнего уровня `panel_info`. | `koin.get<Plugin.DefaultPanelInfoConfig>()` или `koin.getOrNull<Plugin.DefaultPanelInfoConfig>()` |
| `InMemoryPanelFeature` | Неквалифицированный singleton конкретного класса; определение добавляется всегда. | `koin.get<InMemoryPanelFeature>()` |
| `GlobalKVBindedPanelFeature` | Неквалифицированный singleton конкретного класса; определение добавляется всегда. | `koin.get<GlobalKVBindedPanelFeature>()` |
| `PanelFeature` | Неквалифицированный singleton интерфейса, разрешающийся в тот же экземпляр `GlobalKVBindedPanelFeature`. | `koin.get<PanelFeature>()` |
| `ApplicationRoutingConfigurator.Element` | Singleton на основе `PanelRoutingsConfigurator` со сгенерированным случайным квалификатором. | `koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()`; элемент выбирают из коллекции добавлений. |

Сгенерированный квалификатор маршрута намеренно нестабилен и не имеет имени для прикладного кода.
Конфигуратор зарегистрирован только как `ApplicationRoutingConfigurator.Element`, а не по конкретному
классу, поэтому запрос `koin.get<PanelRoutingsConfigurator>()` некорректен. Для получения коллекции
импортируйте `dev.inmo.micro_utils.koin.getAllDistinct`.

Определения функциональности требуют неквалифицированный `Json`, а `GlobalKVBindedPanelFeature`
дополнительно требует неквалифицированный `GlobalKVRepo`. Обычный JVM-плагин `common/server`
предоставляет обе зависимости. Хотя определения добавляются безусловно, singleton-объекты Koin
создаются при разрешении; запрос `PanelFeature` выбирает реализацию с репозиторием и поэтому требует
репозиторий. Для локального хранения в процессе явно запрашивайте `InMemoryPanelFeature`.

`Plugin` и `JVMPlugin` — singleton-объекты запуска, а не определения Koin. Все три локальных класса
также имеют открытые конструкторы и могут использоваться без Koin:

```kotlin
val inMemory: PanelFeature = InMemoryPanelFeature(
    defaultPanel = defaultPanel,
    json = json,
)

val persistent: PanelFeature = GlobalKVBindedPanelFeature(
    globalKVRepo = globalKVRepo,
    json = json,
    defaultPanel = defaultPanel,
)

val routes = PanelRoutingsConfigurator(persistent)
```

Аргумент `json` сейчас сохраняется в `InMemoryPanelFeature`, но не используется; публичный конструктор
всё равно его требует. Два экземпляра с репозиторием, использующие один `GlobalKVRepo`, также
используют одно значение `panel_config`.

## Конфигуратор и конечные точки

[`PanelRoutingsConfigurator`](src/jvmMain/kotlin/configurators/PanelRoutingsConfigurator.kt) —
единственный конфигуратор модуля. Он устанавливает HTTP-маршруты и не устанавливает WebSocket-точек.
Сам элемент маршрутов использует фиксированные значения из `PanelConstants`:
`rootPanelPath = "panel"`, `getPanelSubpath = "get"`, `getPanelDefaultSubpath = "default"` и
`setPanelSubpath = "set"`.

Все пути в таблице **относительные**: в них нет схемы, хоста и начального слеша.

| Метод | Относительный путь | Назначение и ответ |
| --- | --- | --- |
| `GET` | `panel/get` | Возвращает текущий `PanelInfo`. Если текущей конфигурации нет, отвечает строкой `null`. |
| `GET` | `panel/default` | Возвращает настроенный `PanelInfo` по умолчанию. Если значения по умолчанию нет, отвечает строкой `null`. |
| `POST` | `panel/set` | Принимает `PanelInfo`, передаёт его в `PanelFeature.setPanelConfig` и возвращает результат этого метода типа Boolean. |

Когда модуль `common/server` устанавливает элемент маршрутов, необязательный `KtorConfig.rootRoute`
добавляется перед каждым из трёх путей. Например, при `rootRoute = "api"` первый относительный путь —
`api/panel/get`. `rootRoute` относится к внешнему модулю; без него пути в точности соответствуют
таблице.

Сам конфигуратор не выполняет аутентификацию или авторизацию. Для декодирования JSON-запроса и
кодирования типизированных ответов требуется настройка content negotiation сервера. В модуле нет
конфигураторов, не связанных с маршрутизацией, поэтому дополнительных конфигураторов без конечных
точек тоже нет.
