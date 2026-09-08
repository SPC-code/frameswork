package space.kscience.frameswork.features.common.web

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

object CommonResources {
    val cancel = buildStringResource("Отменить") {
        IetfLang.English("Cancel")
    }
    val create = buildStringResource("Создать") {
        IetfLang.English("Create")
    }
    val open = buildStringResource("Открыть") {
        IetfLang.English("Open")
    }
    val save = buildStringResource("Сохранить") {
        IetfLang.English("Save")
    }
    val dontSave = buildStringResource("Не сохранять") {
        IetfLang.English("Do not save")
    }
    val done = buildStringResource("Готово") {
        IetfLang.English("Done")
    }
    val ok = buildStringResource("Ок") {
        IetfLang.English("Ok")
    }
    val accept = buildStringResource("Принять") {
        IetfLang.English("Accept")
    }
    val clear = buildStringResource("Очистить") {
        IetfLang.English("Clear")
    }
    val rename = buildStringResource("Переименовать") {
        IetfLang.English("Rename")
    }
    val delete = buildStringResource("Удалить") {
        IetfLang.English("Delete")
    }
    val title = buildStringResource("Название") {
        IetfLang.English("Title")
    }
    val confirm = buildStringResource("Подтвердить") {
        IetfLang.English("Confirm")
    }
    val copy = buildStringResource("Копировать") {
        IetfLang.English("Copy")
    }
    val copyUp = buildStringResource("Скопировать") {
        IetfLang.English("Copy up")
    }
    val add = buildStringResource("Добавить") {
        IetfLang.English("Add")
    }
    val logout = buildStringResource("Выйти") {
        IetfLang.English("Log out")
    }
    val profile = buildStringResource("Профиль") {
        IetfLang.English("Profile")
    }
    val back = buildStringResource("Назад") {
        IetfLang.English("Back")
    }
    val complete = buildStringResource("Готово") {
        IetfLang.English("Complete")
    }
    val close = buildStringResource("Закрыть") {
        IetfLang.English("Close")
    }
    val selectFileSuggestion = buildStringResource("Выберите файл") {
        IetfLang.English("Select file")
    }
    val selectDirectorySuggestion = buildStringResource("Выберите папку") {
        IetfLang.English("Select folder")
    }
    val noValueSymbol = buildStringResource("--")
}
