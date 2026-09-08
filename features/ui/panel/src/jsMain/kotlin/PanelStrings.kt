package space.kscience.frameswork.features.ui.panel

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

object PanelStrings {
    val loading = buildStringResource("Loading...") {
        IetfLang.Russian("Загрузка...")
    }
    val editPanel = buildStringResource("Edit panel") {
        IetfLang.Russian("Редактирование панели")
    }
    val exportPanel = buildStringResource("Export panel") {
        IetfLang.Russian("Экспорт панели")
    }
    val importPanel = buildStringResource("Import panel") {
        IetfLang.Russian("Импорт панели")
    }
    val save = buildStringResource("Save panel") {
        IetfLang.Russian("Сохранить панель")
    }
    val removeCell = buildStringResource("Remove cell") {
        IetfLang.Russian("Удалить ячейку")
    }
    val panelSampleTitle = buildStringResource("Sample") {
        IetfLang.Russian("Пример")
    }
    val sampleConfigTextLabel = buildStringResource("Text of sample") {
        IetfLang.Russian("Текст конфигурации-примера")
    }
    val sampleConfigBackgroundColorLabel = buildStringResource("Background color") {
        IetfLang.Russian("Цвет фона")
    }
    val addConfigTitle = buildStringResource("Add new config") {
        IetfLang.Russian("Добавление новой конфигурации")
    }
    val cancelConfigAdding = buildStringResource("Cancel") {
        IetfLang.Russian("Отмена")
    }
    val completeConfigAdding = buildStringResource("Save") {
        IetfLang.Russian("Сохранить")
    }
}