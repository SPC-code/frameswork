# UI панели

[Английская версия](README.md)

Gradle-модуль: `:frameswork.features.ui.panel`

Maven-модуль: `space.kscience:frameswork.features.ui.panel`

Этот Kotlin Multiplatform-модуль предоставляет браузерный UI для отображения и
редактирования панели из дочерних navigation view Frameswork. Он определяет
навигационную конфигурацию панели, контракты model и view model, Compose Web view,
локализованные строки и встроенный редактор элементов-примеров.

## Зависимость и запуск

Добавьте опубликованный артефакт в JavaScript source set:

```kotlin
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation("space.kscience:frameswork.features.ui.panel:<version>")
        }
    }
}
```

Добавьте [`space.kscience.frameswork.features.ui.panel.JSPlugin`](src/jsMain/kotlin/JSPlugin.kt)
в список плагинов `StartLauncherPlugin` браузерного приложения. Его настройка
включает общие регистрации из [`Plugin`](src/commonMain/kotlin/Plugin.kt), поэтому не нужно
добавлять оба UI-плагина панели отдельно.

Тот же граф запуска должен предоставлять общие `Json`, инфраструктуру навигации
и `PanelFeature`. В клиенте репозитория для этого запускаются JS-плагины common-common,
common-web и panel-web. Там же запускается sample UI plugin: он добавляет сериализатор
и фабрику навигации, необходимые встроенному `SamplePanelViewConfigProvider` и
резервной sample-панели. Maven-зависимость делает эти API доступными при компиляции,
но не запускает их плагины.

## Основные типы

- [`PanelViewConfig`](src/commonMain/kotlin/ui/PanelViewConfig.kt) — сериализуемый `ViewConfig` без
  полей, идентифицирующий экран панели. Сам layout загружается моделью.
- [`PanelModel`](src/commonMain/kotlin/ui/PanelModel.kt) — UI-контракт layout-а. Он предоставляет
  `Flow<PanelInfo>`, принимает полную замену и выдаёт `Json` для декодирования
  полиморфной view-конфигурации каждого элемента.
- [`PanelViewModel`](src/commonMain/kotlin/ui/PanelViewModel.kt) загружает сохранённый layout и
  управляет режимами просмотра/редактирования, добавлением элемента, выбором
  провайдера и импортом/экспортом JSON. Сохранение запускает
  `PanelModel.updatePanelInfo`, после чего завершает режим редактирования.
- [`PanelViewConfigProvider`](src/commonMain/kotlin/ui/PanelViewConfigProvider.kt) — точка расширения
  для Compose-редакторов, создающих специфичные для элемента `ViewConfig`. Провайдер
  сообщает `null`, пока форма не заполнена, и конфигурацию, когда она корректна.
- [`PanelView`](src/jsMain/kotlin/ui/PanelView.kt) — navigation view для Compose Web. Она отрисовывает
  каждый декодированный элемент панели как дочерний navigation node и поддерживает
  добавление, перемещение, изменение размера, удаление, импорт, экспорт и сохранение.
- [`PanelStrings`](src/jsMain/kotlin/PanelStrings.kt) содержит английские строковые ресурсы панели
  и русские переводы. Объект используется напрямую, например
  `PanelStrings.save.translation()`.
- [`SamplePanelViewConfigProvider`](src/jsMain/kotlin/ui/sample/SamplePanelViewConfigProvider.kt) —
  встроенный провайдер. Его форма создаёт `SampleViewConfig` только после ввода
  непустого текста и цвета фона.

Стандартный `PanelModel`, регистрируемый плагином, делегирует хранение `PanelFeature`.
Его flow сначала пытается взять текущий layout, затем layout по умолчанию из функции,
принимая любой из них, только если каждая конфигурация элемента декодируется общим
`Json`. Если оба варианта непригодны, он выдаёт сгенерированную панель три на три из
sample-элементов. Метод обновления model делегирует замену `PanelFeature` и, если этот
вызов не выполнился с исключением, сигнализирует flow о повторной загрузке; Boolean-результат
`PanelFeature.setPanelConfig` не предоставляется через `PanelModel`.

## Внедрение зависимостей

Плагины запуска устанавливают следующие определения Koin:

| Зарегистрированный тип | Scope и qualifier | Как получить | Назначение |
| --- | --- | --- | --- |
| `PanelModel` | singleton, без qualifier-а | `koin.get<PanelModel>()` | Стандартная model на основе `PanelFeature`; требует неквалифицированные определения `PanelFeature` и `Json`. |
| `PanelViewModel` | factory, без qualifier-а | `koin.get<PanelViewModel> { parametersOf(node) }` | Создаёт view model для переданного `NavigationNode<PanelViewConfig, ViewConfig>`; получает `PanelModel`, `Json` и все вклады провайдеров из Koin. |
| `SerializersModule` | singleton, случайный сгенерированный qualifier | `koin.getAllDistinct<SerializersModule>()` | Полиморфно регистрирует `PanelViewConfig` и для `Any`, и для `ViewConfig`. |
| `NavigationNodeFactory<ViewConfig>` | singleton, случайный сгенерированный qualifier | `koin.getAllDistinct<NavigationNodeFactory<ViewConfig>>()` | JS-фабрика типа, создающая `PanelView(chain, config)` для `PanelViewConfig`. |
| `PanelViewConfigProvider` | singleton, случайный сгенерированный qualifier | `koin.getAllDistinct<PanelViewConfigProvider>()` | JS-вклад на основе объекта `SamplePanelViewConfigProvider`. |

`singleWithRandomQualifier` создаёт нестабильный qualifier для каждого агрегируемого вклада.
Не изобретайте именованный qualifier и не используйте одиночный неквалифицированный
`get()` для `SerializersModule`, `NavigationNodeFactory<ViewConfig>` и
`PanelViewConfigProvider`; импортируйте `dev.inmo.micro_utils.koin.getAllDistinct` и
получайте их коллекции.

```kotlin
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.parameter.parametersOf
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.ui.PanelModel
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfigProvider
import space.kscience.frameswork.features.ui.panel.ui.PanelViewModel

val model: PanelModel = koin.get()
val viewModel: PanelViewModel = koin.get { parametersOf(node) }
val serializers: List<SerializersModule> = koin.getAllDistinct()
val viewFactories: List<NavigationNodeFactory<ViewConfig>> = koin.getAllDistinct()
val itemEditors: List<PanelViewConfigProvider> = koin.getAllDistinct()
```

`PanelViewConfig`, `PanelView` и `PanelStrings` не регистрируются в Koin напрямую. Создавайте
конфигурацию через `PanelViewConfig()`. Обычно view создаёт агрегированная
фабрика навигации; можно напрямую создать `PanelView(chain, config)`, но Koin должен быть
запущен до обращения к её ленивой `viewModel`. Поддерживается и прямое создание
`PanelViewModel(node, model, providers, json)`. Стандартный `PanelModel` плагина анонимен;
без DI реализуйте `PanelModel` самостоятельно. Sample-провайдер является объектом,
и к нему можно обращаться напрямую, хотя Koin предоставляет его только как один из вкладов
`PanelViewConfigProvider` со случайным qualifier-ом.

Чтобы добавить другой редактор элементов, реализуйте `PanelViewConfigProvider` и добавьте
его под типом интерфейса:

```kotlin
singleWithRandomQualifier<PanelViewConfigProvider> { MyPanelItemProvider }
```

Для типа конфигурации элемента также нужны вклад полиморфного сериализатора и
соответствующая `NavigationNodeFactory<ViewConfig>`, чтобы сохранённые элементы можно было
декодировать и отрисовать.

## Использование навигации

После того как `JSPlugin` добавил типизированную фабрику, откройте панель из уже
существующей navigation composition через её конфигурацию без полей:

```kotlin
InjectNavigationNode(
    PanelViewConfig()
)
```

Агрегированная в приложении `NavigationNodeFactory<ViewConfig>` выбирает типизированную
фабрику панели. Внутри `PanelView` каждый `PanelItemInfo.config` декодируется с помощью
`PanelModel.decodingJson` и передаётся в ещё один `InjectNavigationNode`, поэтому каждая ячейка сетки
может содержать view любого зарегистрированного типа.

## Сетевые пути и серверные endpoints

В модуле нет server source set, Ktor-конфигуратора маршрутов, HTTP- или WebSocket-endpoint-ов,
прямых запросов `HttpClient` или констант клиентских путей. Его стандартная model вызывает
интерфейс `PanelFeature` и не выбирает URL. Конкретная реализация `PanelFeature` может хранить
данные локально или удалённо; собственные относительные клиентские пути репозиторной panel-web-
реализации описаны в [README panel-web](../../panel/web/README.ru.md).
