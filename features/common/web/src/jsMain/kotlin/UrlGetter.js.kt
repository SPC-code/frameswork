package space.kscience.frameswork.features.common.web

import kotlinx.browser.window

/** Returns the browser window's current absolute URL. */
actual suspend fun getUrl(): String {
    return window.location.href
}
