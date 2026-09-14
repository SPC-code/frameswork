# Клиент Frameswork

[English version](README.md)

Gradle-модуль `:frameswork.client` содержит браузерную точку инициализации Frameswork для Kotlin/JS.
Он объединяет стандартные плагины функциональности, создаёт конфигурацию запуска после загрузки
браузерного окна, инициализирует навигацию и отображает интерфейс Compose for Web в DOM-элементе
`content`.

- Gradle-модуль: `:frameswork.client`
- Kotlin-пакет: `space.kscience.frameswork.client`
- Maven-координаты Kotlin Multiplatform: `space.kscience:frameswork.client:<version>`
- Maven-координаты JavaScript-таргета: `space.kscience:frameswork.client-js:<version>`

Maven-координаты соответствуют сгенерированным публикациям модуля; версия проекта задаётся в
корневом файле `gradle.properties`.

## Инициализация в браузере

[`initClient`](src/jsMain/kotlin/Main.kt) — публичная функция инициализации модуля. Она регистрирует
обработчик браузерного события `load`, а затем запускает `StartLauncherPlugin.start` в корутине со
следующей последовательностью плагинов в конфигурации:

1. Все плагины из `plugins` в порядке, заданном вызывающим кодом.
2. Все плагины из `defaultPlugins` в указанном ниже порядке.

Лаунчер настраивает DI, перебирая эту последовательность, но запускает стартовые обработчики
плагинов асинхронно и ожидает завершения всех обработчиков. Положение в списке не означает, что
стартовый обработчик одного плагина завершится до запуска следующего.

Стандартный список:

1. `ClientJSPlugin`
2. `features.common.common.JSPlugin`
3. `features.common.web.JSPlugin`
4. `features.frames.web.JSPlugin`
5. `features.panel.web.JSPlugin`
6. `features.processor.web.JSPlugin`
7. `features.ui.sample.JSPlugin`
8. `features.ui.panel.JSPlugin`
9. `features.ui.panel.frames.JSPlugin`

Вызовите `initClient` напрямую из точки входа исполняемого модуля Kotlin/JS до завершения загрузки
документа:

```kotlin
import space.kscience.frameswork.client.initClient

fun main() {
    initClient(
        plugins = listOf(MyApplicationPlugin),
    )
}
```

В HTML-контейнере должен находиться узел, в который Compose встроит интерфейс:

```html
<div id="content"></div>
```

`initClient` не является функцией `main` и не регистрируется в Koin: её нужно вызывать напрямую.
Передача аргумента `defaultPlugins` полностью заменяет стандартный список, поэтому новый список
должен включать `ClientJSPlugin` и все необходимые приложению плагины функциональности.

## Плагины и внедрение зависимостей

[`ClientPlugin`](src/commonMain/kotlin/ClientPlugin.kt) и
[`ClientJSPlugin`](src/jsMain/kotlin/ClientJSPlugin.kt) — объекты Kotlin, реализующие `StartPlugin`.
Они не являются Koin-привязками и не имеют Koin-квалификатора: обращайтесь непосредственно к
объектам-одиночкам или добавляйте их в список плагинов запуска. `ClientJSPlugin` делегирует общий
жизненный цикл плагину `ClientPlugin`.

`ClientJSPlugin.setupDI` добавляет ровно две неквалифицированные привязки-одиночки:

| Привязка | Регистрация | Получение |
|---|---|---|
| Функция установки блока Compose, `(@Composable () -> Unit) -> Unit` | Неквалифицированный `single`; переданный блок записывается в `ClientJSPlugin.currentDrawingBlock` | `koin.get<(@Composable () -> Unit) -> Unit>()` |
| `NavigationConfigsRepo<ViewConfig>` | Неквалифицированный `single`; реализация — `NavigationConfigsRepo.InMemory<ViewConfig>` | `koin.get<NavigationConfigsRepo<ViewConfig>>()` |

У этих двух привязок нет именованных или случайных квалификаторов. Сам
`ClientPlugin.setupDI` не добавляет привязок. `ClientJSPlugin.currentDrawingBlock` — свойство
объекта-плагина, а не DI-привязка, поэтому при необходимости прямого доступа используется
`ClientJSPlugin.currentDrawingBlock`.

При запуске `ClientPlugin` получает из Koin функцию установки блока и репозиторий конфигураций
навигации. Он получает собранную из плагинов функциональности фабрику узлов навигации, запускает
навигацию с `EmptyConfig` и добавляет начальный `PanelViewConfig`. Затем `ClientJSPlugin` добавляет
`FlotViewStyleSheet`, встраивает Compose в `content` и отображает текущий блок.

## Зависимости модуля

Модуль экспортирует следующие модули Frameswork как API-зависимости `commonMain`:

- `:frameswork.features.common.web`
- `:frameswork.features.panel.web`
- `:frameswork.features.processor.web`
- `:frameswork.features.ui.sample`
- `:frameswork.features.ui.panel`
- `:frameswork.features.ui.panel.frames.web`

Также экспортируются библиотеки запуска, navigation MVVM и Compose Runtime. Compose Web Core —
implementation-зависимость `jsMain`. Общие Gradle-шаблоны также добавляют kotlinx serialization как
API-зависимость, а стандартную библиотеку Kotlin — как implementation-зависимость. В стандартном
списке инициализации дополнительно указаны плагины общего ядра и Frames Web: они доступны через граф
зависимостей функциональности, а не через прямые объявления в `client/build.gradle`.

## Пути времени выполнения и конечные точки

Клиентский модуль не определяет HTTP-конечных точек, маршрутов Ktor или серверных конфигураторов
маршрутизации.

Единственная жёстко заданная цель времени выполнения — DOM-элемент с идентификатором `content`.
Следующие браузерные ресурсы упаковываются по относительным путям; модуль не генерирует основной
HTML-файл и не добавляет теги для их подключения:

- `css/bootstrap.min.css`
- `css/internal.css`
- `js/bootstrap.bundle.min.js`
- `js/plotly.min.js`

## Сборка и проверка

Из корня репозитория:

```bash
./gradlew :frameswork.client:compileKotlinJs
./gradlew :frameswork.client:jsNodeTest
./gradlew :frameswork.client:build
```

Сейчас в модуле нет исходного кода тестов Kotlin. Модуль также не объявляет исполняемый бинарный
файл Kotlin/JS и не содержит основной HTML-файл, поэтому у него нет собственной задачи запуска в
браузере или Webpack. Исполняемую точку входа предоставляет использующее модуль приложение, которое
и вызывает `initClient`.
