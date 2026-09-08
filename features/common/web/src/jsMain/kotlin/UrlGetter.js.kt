package space.kscience.frameswork.features.common.web

import kotlinx.browser.window

actual suspend fun getUrl(): String {
    return window.location.href
}