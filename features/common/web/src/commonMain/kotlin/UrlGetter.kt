package space.kscience.frameswork.features.common.web

/**
 * Returns the platform's current absolute URL for use as the default Ktor client base URL.
 *
 * @return the current page URL on JavaScript targets.
 */
expect suspend fun getUrl(): String
