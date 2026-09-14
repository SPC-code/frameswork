# Общая панель

Gradle-модуль: `:frameswork.features.panel.common`  
Maven-модуль: `space.kscience:frameswork.features.panel.common`

Этот Kotlin Multiplatform-модуль определяет общий контракт компоновки панели, сериализуемую модель
панели и относительные константы путей, используемые серверным и веб-модулями панели. Настроенные
цели публикации — JVM и JavaScript; Gradle выбирает соответствующий платформенный вариант из
multiplatform-зависимости.

Документация на английском: [README.md](README.md).

## Зависимость

```kotlin
commonMain.dependencies {
    implementation("space.kscience:frameswork.features.panel.common:<version>")
}
```

## API панели

[`PanelFeature`](src/commonMain/kotlin/PanelFeature.kt) — независимый от способа хранения контракт для
компоновки панели:

- `getPanelConfig()` возвращает текущую компоновку или `null`, если она не была сохранена.
- `getDefaultConfig()` возвращает компоновку по умолчанию или `null`, если она недоступна.
- `setPanelConfig(config)` предлагает реализации заменить текущую компоновку и сообщает, было ли
  принято обновление.

Интерфейс не задаёт правила хранения, параллельного доступа или проверки данных. Эти правила
определяет реализация.

## Модель

[`PanelInfo`](src/commonMain/kotlin/models/PanelInfo.kt) — сериализуемое описание панели. Оно хранит
количество слотов по горизонтали и вертикали и список значений `PanelItemInfo`. Тип не проверяет
положительность размеров, пересечение элементов и их размещение внутри сетки.

[`PanelItemInfo`](src/commonMain/kotlin/models/PanelItemInfo.kt) хранит начало координат и размер
элемента, а также его собственную конфигурацию в виде `JsonElement`. Методы проверки вхождения
используют полуоткрытые границы: `[x, x + width)` и `[y, y + height)`.

Создавайте значения модели напрямую:

```kotlin
val item = PanelItemInfo(
    x = 0,
    y = 1,
    width = 2,
    height = 1,
    config = buildJsonObject { put("type", "camera") },
)
val panel = PanelInfo(horizontalSlots = 4, verticalSlots = 3, items = listOf(item))
```

Вторичный конструктор `PanelItemInfo` принимает конфигурацию типа `Any` и кодирует её с помощью
`PolymorphicSerializer<Any>`. `decodeConfig(json)` выполняет обратную операцию. В обоих случаях
переданный экземпляр `Json` должен содержать полиморфный сериализатор конкретного типа конфигурации.

## Внедрение зависимостей через Koin

У общего [`Plugin`](src/commonMain/kotlin/Plugin.kt) реализация `setupDI` пуста.
[`JVMPlugin`](src/jvmMain/kotlin/JVMPlugin.kt) и
[`JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) передают настройку ему, поэтому загрузка только этого
модуля **не добавляет определений Koin**. Этот модуль не предоставляет квалификаторов или готовых
точных вызовов получения.

| Объявление | Встроенное определение Koin | Как получить |
| --- | --- | --- |
| `PanelFeature` | нет | Зарегистрировать или загрузить реализацию, затем вызвать `koin.get<PanelFeature>()` без квалификатора |
| `PanelInfo` | нет | Создать или десериализовать напрямую |
| `PanelItemInfo` | нет | Создать или десериализовать напрямую |
| `PanelConstants` | нет | Использовать Kotlin-объект напрямую |

Плагины соседних модулей `panel.server` и `panel.web` предоставляют привязку `PanelFeature` без
квалификатора к своей реализации. Также приложение может явно предоставить собственную реализацию:

```kotlin
val panelModule = module {
    single<PanelFeature> { ApplicationPanelFeature(/* зависимости приложения */) }
}

val panelFeature = koin.get<PanelFeature>()
```

Общий модуль не определяет `ApplicationPanelFeature` и не выбирает зависимости реализации: и то и
другое относится к приложению. Экземпляры модели обычно представляют данные запроса, ответа или
хранилища и не являются DI-сервисами.

## Относительные пути и платформы

[`PanelConstants`](src/commonMain/kotlin/PanelConstants.kt) содержит относительные строки, общие для
сопутствующих HTTP-модулей:

| Константа | Значение |
| --- | --- |
| `rootPanelPath` | `panel` |
| `getPanelSubpath` | `get` |
| `getPanelDefaultSubpath` | `default` |
| `setPanelSubpath` | `set` |
| `getPanelFullPath` | `panel/get` |
| `getPanelDefaultPath` | `panel/default` |
| `setPanelFullPath` | `panel/set` |

Все пути относительные и не имеют начального слеша. Этот модуль не содержит конфигуратора Ktor, не
устанавливает маршруты и поэтому сам по себе не предоставляет HTTP-эндпоинты. Текущая сборка
публикует цели JVM и JavaScript. Исходный код Android-плагина присутствует для повторного
использования, но сейчас модуль не настраивает и не публикует Android-цель.
