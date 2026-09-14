package space.kscience.frameswork.features.ui.panel

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized string resources used by the panel view and its built-in sample provider. */
object PanelStrings {
    /** Loading-state label. */
    val loading = buildStringResource("Loading...") {
        IetfLang.Russian("Загрузка...")
    }

    /** Label for the control that enters or leaves panel editing. */
    val editPanel = buildStringResource("Edit panel") {
        IetfLang.Russian("Редактирование панели")
    }

    /** Label for exporting the current editable panel configuration. */
    val exportPanel = buildStringResource("Export panel") {
        IetfLang.Russian("Экспорт панели")
    }

    /** Label for importing a panel configuration. */
    val importPanel = buildStringResource("Import panel") {
        IetfLang.Russian("Импорт панели")
    }

    /** Label for persisting panel edits. */
    val save = buildStringResource("Save panel") {
        IetfLang.Russian("Сохранить панель")
    }

    /** Label shown in the drag-to-delete area. */
    val removeCell = buildStringResource("Remove cell") {
        IetfLang.Russian("Удалить ячейку")
    }

    /** Title of the built-in sample configuration provider. */
    val panelSampleTitle = buildStringResource("Sample") {
        IetfLang.Russian("Пример")
    }

    /** Label for the sample configuration's text input. */
    val sampleConfigTextLabel = buildStringResource("Text of sample") {
        IetfLang.Russian("Текст конфигурации-примера")
    }

    /** Label for the sample configuration's background-color input. */
    val sampleConfigBackgroundColorLabel = buildStringResource("Background color") {
        IetfLang.Russian("Цвет фона")
    }

    /** Title of the add-item configuration dialog. */
    val addConfigTitle = buildStringResource("Add new config") {
        IetfLang.Russian("Добавление новой конфигурации")
    }

    /** Label for cancelling the add-item workflow. */
    val cancelConfigAdding = buildStringResource("Cancel") {
        IetfLang.Russian("Отмена")
    }

    /** Label for accepting a completed new-item configuration. */
    val completeConfigAdding = buildStringResource("Save") {
        IetfLang.Russian("Сохранить")
    }
}
