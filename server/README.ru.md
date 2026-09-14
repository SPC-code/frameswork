# Серверный комплект Frameswork

[English version](README.md)

- Модуль Gradle: `:frameswork.server`
- Координаты Maven: `space.kscience:frameswork.server`
- Целевая JVM: Java 17

Этот модуль — публикуемый комплект JVM-зависимостей для сервера Frameswork. Его `api`-зависимости
добавляют в classpath потребителя серверные модули функций, серверную и клиентскую поддержку Ktor,
Exposed JDBC, а также драйверы PostgreSQL и H2.

В модуле нет каталога `server/src`. Поэтому его JAR не содержит собственных Kotlin-деклараций,
стартового плагина, определений внедрения зависимостей, конфигураторов, маршрутов, ресурсов или функции
`main`. Подключение зависимости делает экспортируемые API доступными, но не запускает Koin, не
загружает плагины функций и не запускает Ktor.

## Собранные модули функций

Все пять проектных зависимостей экспортируются через Gradle-конфигурацию `api`:

| Функция | Основное назначение | Документация модуля |
| --- | --- | --- |
| `:frameswork.features.common.server` | Начальная настройка Koin/Ktor, общая конфигурация сервера, хранение данных, статические файлы и общие серверные конфигураторы. | [`features/common/server`](../features/common/server/README.ru.md) |
| `:frameswork.features.panel.server` | Постоянная и хранящаяся в памяти конфигурация панели, а также её HTTP API. | [`features/panel/server`](../features/panel/server/README.ru.md) |
| `:frameswork.features.frames.server` | Серверная интеграция источников и сборщиков кадров. | [`features/frames/server`](../features/frames/server/README.ru.md) |
| `:frameswork.features.processor.server` | Настраиваемые конвейеры обработки кадров и JVM-middleware для обрезки. | [`features/processor/server`](../features/processor/server/README.ru.md) |
| `:frameswork.features.ui.panel.frames.server` | Серверная реализация и маршруты для интерфейса панели кадров. | [`features/ui/panel/frames/server`](../features/ui/panel/frames/server/README.ru.md) |

Комплект также экспортирует Ktor Tomcat, WebSockets, Apache client и интеграцию kotlinx serialization;
Exposed JDBC; драйверы PostgreSQL и H2. Сейчас общая серверная функция создаёт встроенный сервер
Netty, хотя этот комплект дополнительно экспортирует артефакт Tomcat.

Чтобы использовать опубликованный комплект в другом JVM-проекте, укажите нужную приложению версию
из репозитория (версия в этой рабочей копии — `0.0.1`):

```groovy
dependencies {
    implementation "space.kscience:frameswork.server:0.0.1"
}
```

Внутри этой сборки используйте `implementation project(":frameswork.server")`.

## Запуск и конфигурация

Плагина `space.kscience.frameswork.server.JVMPlugin` не существует. Приложение запуска должно
использовать MicroUtils startup launcher, доступный транзитивно из `common/server`, и перечислить все
необходимые стартовые плагины Frameswork в корневой JSON-конфигурации. Минимальная форма для пяти
собранных функций выглядит так:

```json
{
  "host": "127.0.0.1",
  "port": 8196,
  "publicHost": "127.0.0.1",
  "rootRoute": null,
  "staticFolders": {},
  "database": {
    "url": "jdbc:h2:./server/local.test",
    "driver": "org.h2.Driver",
    "username": "",
    "password": ""
  },
  "plugins": [
    "space.kscience.frameswork.features.common.server.JVMPlugin",
    "space.kscience.frameswork.features.frames.server.JVMPlugin",
    "space.kscience.frameswork.features.processor.server.JVMPlugin",
    "space.kscience.frameswork.features.panel.server.JVMPlugin",
    "space.kscience.frameswork.features.ui.panel.frames.server.JVMPlugin"
  ],
  "cameras": {
    "presets": []
  },
  "processors": {}
}
```

Поля `host`, `port` и `rootRoute` настраивают слушатель Netty и префикс маршрутов через
`common/server`. Объект `database` настраивает его подключение Exposed. Раздел `cameras` читает
Frames JVM-плагин, а `processors` обязателен для серверного плагина процессоров, даже когда ни один
процессор не настроен. Ключи и ограничения отдельных функций описаны в README соответствующих
модулей по ссылкам выше.

Относительные пути файловой системы разрешаются от рабочего каталога процесса запуска. В примере
выше `jdbc:h2:./server/local.test` подходит для процесса, запущенного из корня репозитория. Напротив,
статическое сопоставление `"": "../client/build/dist/js/developmentExecutable"` предполагает запуск
из `server/`, как в [`sample.config.dev.json`](sample.config.dev.json). Контейнерный шаблон
[`sample.config.docker-compose.json`](sample.config.docker-compose.json) берёт статические файлы из
`/static/`.

Две добавленные в репозиторий конфигурации — исторические шаблоны развёртывания, а не минимальные
проверенные конфигурации: в них указаны плагины `space.kscience.frameswork.features.markup.*`, хотя в
текущем [`settings.gradle`](../settings.gradle) нет модуля markup, а в объектах базы H2 не указан
обязательный драйвер `org.h2.Driver`. Перед применением любого шаблона удалите недоступные плагины и
задайте драйвер (либо укажите URL PostgreSQL и учётные данные).

## Внедрение зависимостей

Этот агрегирующий модуль не определяет классов и не регистрирует определений Koin, поэтому у него нет
собственного типа, который можно получить через `get()` или `inject()`. Одни объявления зависимостей
не регистрируют bindings собранных модулей: startup launcher должен выполнить их методы
`JVMPlugin.setupDI` (либо приложение должно выполнить эквивалентную начальную настройку).

После завершения настройки DI соответствующими плагинами функций основные bindings без qualifier
можно получить так:

```kotlin
val globals = koin.get<GlobalKVRepo>()
val frames = koin.get<FramesCollector>()
val frameSources = koin.get<FramesSourcesCollector>()
val panel = koin.get<PanelFeature>()
val processors = koin.get<ProcessorsContainer>()
val framesData = koin.get<FramesDataInfoFeature>()
```

Это транзитивные bindings, а не bindings модуля `server`. Некоторые определения функций зависят от
наличия конфигурации, а вклады конфигураторов используют коллекционные bindings или qualifiers.
Полный список bindings, точные qualifiers, условия доступности и варианты прямого создания типов
смотрите в README соответствующей функции по ссылкам выше.

## Конфигураторы и конечные точки

В этом модуле нет собственного стартового плагина или исходного кода, поэтому он не устанавливает ни
одного конфигуратора и ни одной конечной точки HTTP или WebSocket. В частности, простое подключение
`space.kscience:frameswork.server` не устанавливает перечисленные ниже маршруты.

Следующие вклады маршрутизации доступны только транзитивно и устанавливаются лишь при загрузке плагина
исходной функции и общего серверного плагина. Все пути **относительные**: без схемы, хоста и начального
слеша. Ненулевой `rootRoute` добавляется перед каждым внесённым маршрутом.

| Источник | Конфигуратор | Метод и относительный путь | Назначение |
| --- | --- | --- | --- |
| [`common/server`](../features/common/server/README.ru.md#конфигураторы-и-конечные-точки) | `InternalApplicationRoutingConfigurator` и его элемент статических файлов | `GET [rootRoute/]<настроенный-static-path>/{file...}` | Раздаёт каждый настроенный локальный каталог статических файлов. Остальные общие конфигураторы устанавливают инфраструктуру без фиксированных конечных точек. |
| [`panel/server`](../features/panel/server/README.ru.md#конфигуратор-и-конечные-точки) | `PanelRoutingsConfigurator` | `GET panel/get` | Возвращает текущую конфигурацию панели. |
| [`panel/server`](../features/panel/server/README.ru.md#конфигуратор-и-конечные-точки) | `PanelRoutingsConfigurator` | `GET panel/default` | Возвращает настроенную конфигурацию панели по умолчанию. |
| [`panel/server`](../features/panel/server/README.ru.md#конфигуратор-и-конечные-точки) | `PanelRoutingsConfigurator` | `POST panel/set` | Сохраняет переданную конфигурацию панели и возвращает результат операции. |
| [`frames/server`](../features/frames/server/README.ru.md#конфигураторы-и-конечные-точки) | Нет | Нет | Эта функция не вносит маршрутов. |
| [`processor/server`](../features/processor/server/README.ru.md#конфигураторы-и-конечные-точки) | Нет | Нет | Эта функция не вносит маршрутов. |
| [`frames panel/server`](../features/ui/panel/frames/server/README.ru.md#конфигуратор-и-конечные-точки) | `FramesDataInfoFeatureRoutingsConfigurator` | `GET cameras/getAvailableProcessors` | Возвращает идентификаторы доступных процессоров. |
| [`frames panel/server`](../features/ui/panel/frames/server/README.ru.md#конфигуратор-и-конечные-точки) | `FramesDataInfoFeatureRoutingsConfigurator` | `GET cameras/getAvailableCameras?id={processorName}` | Возвращает текущие идентификаторы источников кадров для одного процессора. |
| [`frames panel/server`](../features/ui/panel/frames/server/README.ru.md#конфигуратор-и-конечные-точки) | `FramesDataInfoFeatureRoutingsConfigurator` | `WebSocket cameras/getFrames` | Принимает выбор процессора/источника и передаёт поток закодированных кадров. |

Точные тела запросов, nullable-ответы, сообщения WebSocket, двоичный формат кадров, регистрацию DI и
требования к инфраструктуре смотрите в README исходного модуля.

## Сборка, публикация и запуск

Из корня репозитория соберите и проверьте комплект командой:

```bash
./gradlew :frameswork.server:build
```

Обычный агрегирующий JAR записывается в `server/build/libs/frameswork.server-<version>.jar`. Для
локальной проверки публикации Maven выполните:

```bash
./gradlew :frameswork.server:publishToMavenLocal
```

Текущий `server/build.gradle` не применяет Gradle-плагин `application`. Поэтому у модуля нет задач
`run`, `startScripts`, `assembleDist`, `distTar` или `distZip`, и его нельзя запустить напрямую.
Запускаемый модуль-потребитель должен применить `application`, подключить этот комплект и задать:

```groovy
application {
    mainClass = "dev.inmo.micro_utils.startup.launcher.MainKt"
}
```

После этого потребитель может передать launcher один абсолютный путь к конфигурации, например:

```bash
./gradlew :your.runner:run --args="/absolute/path/to/frameswork/server/config.json"
```

[`Dockerfile`](Dockerfile) и два скрипта развёртывания сейчас ожидают архив
`server/build/distributions/frameswork.server.tar`, который текущий Gradle-модуль не создаёт, а также
`server/build/productionExecutable.tar`. Их можно использовать только после того, как сборка
запускаемого приложения предоставит эти архивы. [`docker-compose.yml`](docker-compose.yml) запускает
только PostgreSQL для разработки; [`sample.docker-compose.yml`](sample.docker-compose.yml) показывает
предполагаемую схему приложения и контейнера, но не создаёт отсутствующий дистрибутив.
