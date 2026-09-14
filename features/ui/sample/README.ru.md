# Пример UI

[Английская версия](README.md)

Maven-модуль: `space.kscience:frameswork.features.ui.sample`

Этот Kotlin Multiplatform-модуль — минимальная навигационная функция Frameswork для
JavaScript. Он определяет сериализуемую конфигурацию примера view, соответствующие
модель и view model, Compose Web view, локализованные строки и DI-вклады, связывающие
их с общей инфраструктурой навигации и сериализации.

## Подключение и запуск модуля

Добавьте опубликованный артефакт в JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.ui.sample:<version>")
        }
    }
}
```

В браузерном приложении добавьте
[`space.kscience.frameswork.features.ui.sample.JSPlugin`](src/jsMain/kotlin/JSPlugin.kt) в список
плагинов `StartLauncherPlugin`. `JSPlugin` устанавливает и общие регистрации из
[`Plugin`](src/commonMain/kotlin/Plugin.kt), и JavaScript-фабрику navigation view. Запускайте его в одном
графе запуска с общими плагинами Frameswork, которые предоставляют общие `Json`, Koin и
инфраструктуру веб-навигации. Отдельная установка `Plugin` вместе с `JSPlugin` не нужна,
так как `JSPlugin.setupDI` делегирует ему настройку.

## Публичные типы

- [`SampleViewConfig`](src/commonMain/kotlin/ui/SampleViewConfig.kt) — сериализуемый `ViewConfig`.
  Значение `backgroundColor` по умолчанию равно `null`, а `text` — `"Hello, world!"`.
- [`SampleModel`](src/commonMain/kotlin/ui/SampleModel.kt) — контракт модели. Сейчас в нём нет
  членов; плагин регистрирует пустую реализацию-заглушку для поведения экрана-примера.
- [`SampleViewModel`](src/commonMain/kotlin/ui/SampleViewModel.kt) связывает
  `NavigationNode<SampleViewConfig, ViewConfig>` с моделью. Сейчас она не добавляет состояний или
  действий к базовому навигационному `ViewModel`.
- [`SampleView`](src/jsMain/kotlin/ui/SampleView.kt) — navigation node для Compose Web. Она лениво получает
  view model и сейчас отображает эталонную панель из беспараметрического
  `FlotViewDashboard`. Текущий код отрисовки не использует `SampleViewConfig.backgroundColor` и `text`.
- [`SampleStrings`](src/jsMain/kotlin/SampleStrings.kt) предоставляет строковый ресурс `sample` с
  английским значением `"Sample"` по умолчанию и русским переводом `"Пример"`. Это
  объект для прямого обращения, а не Koin-сервис; в Compose-коде получайте перевод через
  `SampleStrings.sample.translation()`.

## Внедрение зависимостей

Плагины запуска добавляют следующие определения Koin:

| Тип | Scope и qualifier | Как получить | Назначение |
| --- | --- | --- | --- |
| `SampleModel` | singleton, без qualifier-а | `koin.get<SampleModel>()` | Пустая реализация модели по умолчанию. |
| `SampleViewModel` | factory, без qualifier-а | `koin.get<SampleViewModel> { parametersOf(node) }` | Новая view model для переданного `NavigationNode<SampleViewConfig, ViewConfig>`; `SampleModel` получается из Koin. |
| `SerializersModule` | singleton, случайный сгенерированный qualifier | `koin.getAllDistinct<SerializersModule>()` | Полиморфно регистрирует `SampleViewConfig` и как `Any`, и как `ViewConfig`. Общая JSON-настройка собирает эти вклады. |
| `NavigationNodeFactory<ViewConfig>` | singleton, случайный сгенерированный qualifier | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | Создаёт `SampleView` для `SampleViewConfig`; устанавливается только `JSPlugin`. |

`singleWithRandomQualifier` намеренно выдаёт каждому агрегируемому вкладу нестабильный
qualifier. Не используйте выдуманный именованный qualifier или одиночный неквалифицированный
`get()` для `SerializersModule` или `NavigationNodeFactory<ViewConfig>`; собирайте их через `getAllDistinct` из
`dev.inmo.micro_utils.koin`.

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.parameter.parametersOf
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.sample.ui.SampleModel
import space.kscience.frameswork.features.ui.sample.ui.SampleViewModel

val model: SampleModel = koin.get()
val viewModel: SampleViewModel = koin.get { parametersOf(node) }
val serializerContributions: List<SerializersModule> = koin.getAllDistinct()
val viewFactories: List<NavigationNodeFactory<ViewConfig>> = koin.getAllDistinct()
```

`SampleViewConfig`, `SampleView` и `SampleStrings` не регистрируются в Koin напрямую. Создавайте
конфигурацию сами. Обычно агрегированная навигационная фабрика создаёт view, а view
запрашивает свою view model с `parametersOf(this@SampleView)`. Можно также напрямую создать
`SampleViewModel(node, model)`, если обе зависимости уже доступны. Прямое создание `SampleView(chain, config)`
повторяет работу фабрики, но Koin должен быть запущен до обращения к её ленивой `viewModel`.

## Использование навигации

После того как `JSPlugin` добавил типизированную фабрику, приложение может перейти к
`SampleViewConfig`. Например, внутри уже созданной навигационной композиции:

```kotlin
InjectNavigationNode(
    SampleViewConfig(text = "Diagnostic sample")
)
```

Агрегированная в приложении `NavigationNodeFactory<ViewConfig>` выбирает типизированную фабрику и
создаёт `SampleView(chain, config)`. Модуль `features.ui.panel` также использует `SampleViewConfig` в своём
провайдере элемента-примера и в данных панели по умолчанию.

## Сетевые пути и серверные endpoints

В модуле нет server source set, Ktor-конфигураторов, HTTP-маршрутов, WebSocket-маршрутов или
клиентских запросов. Поэтому он не задаёт endpoints или клиентские пути — ни относительные, ни
абсолютные.
