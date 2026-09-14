package space.kscience.frameswork.features.common.web

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Shared Russian-first string resources used by common client UI actions and labels. */
object CommonResources {
    /** Label for cancelling the current operation. */
    val cancel = buildStringResource("Отменить") {
        IetfLang.English("Cancel")
    }

    /** Label for creating a new item. */
    val create = buildStringResource("Создать") {
        IetfLang.English("Create")
    }

    /** Label for opening an existing item. */
    val open = buildStringResource("Открыть") {
        IetfLang.English("Open")
    }

    /** Label for saving changes. */
    val save = buildStringResource("Сохранить") {
        IetfLang.English("Save")
    }

    /** Label for continuing without saving changes. */
    val dontSave = buildStringResource("Не сохранять") {
        IetfLang.English("Do not save")
    }

    /** Label indicating that an operation is finished. */
    val done = buildStringResource("Готово") {
        IetfLang.English("Done")
    }

    /** Short affirmative acknowledgement label. */
    val ok = buildStringResource("Ок") {
        IetfLang.English("Ok")
    }

    /** Label for accepting a proposed value or action. */
    val accept = buildStringResource("Принять") {
        IetfLang.English("Accept")
    }

    /** Label for clearing entered or selected values. */
    val clear = buildStringResource("Очистить") {
        IetfLang.English("Clear")
    }

    /** Label for renaming an item. */
    val rename = buildStringResource("Переименовать") {
        IetfLang.English("Rename")
    }

    /** Label for deleting an item. */
    val delete = buildStringResource("Удалить") {
        IetfLang.English("Delete")
    }

    /** Label for an item's title field. */
    val title = buildStringResource("Название") {
        IetfLang.English("Title")
    }

    /** Label for confirming a pending action. */
    val confirm = buildStringResource("Подтвердить") {
        IetfLang.English("Confirm")
    }

    /** Label for copying a value to the clipboard or another destination. */
    val copy = buildStringResource("Копировать") {
        IetfLang.English("Copy")
    }

    /** Label for creating a copy of an item. */
    val copyUp = buildStringResource("Скопировать") {
        IetfLang.English("Copy up")
    }

    /** Label for adding an item. */
    val add = buildStringResource("Добавить") {
        IetfLang.English("Add")
    }

    /** Label for ending the current authenticated session. */
    val logout = buildStringResource("Выйти") {
        IetfLang.English("Log out")
    }

    /** Label for opening the current user's profile. */
    val profile = buildStringResource("Профиль") {
        IetfLang.English("Profile")
    }

    /** Label for returning to the previous screen. */
    val back = buildStringResource("Назад") {
        IetfLang.English("Back")
    }

    /** Label for marking a workflow as complete. */
    val complete = buildStringResource("Готово") {
        IetfLang.English("Complete")
    }

    /** Label for closing the current view or dialog. */
    val close = buildStringResource("Закрыть") {
        IetfLang.English("Close")
    }

    /** Prompt shown when a file must be selected. */
    val selectFileSuggestion = buildStringResource("Выберите файл") {
        IetfLang.English("Select file")
    }

    /** Prompt shown when a directory must be selected. */
    val selectDirectorySuggestion = buildStringResource("Выберите папку") {
        IetfLang.English("Select folder")
    }

    /** Placeholder used when no displayable value is available. */
    val noValueSymbol = buildStringResource("--")
}
