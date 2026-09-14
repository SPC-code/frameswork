package space.kscience.frameswork.features.common.server

/**
 * JVM debug-mode flag, enabled only when the `DEBUG` environment variable equals `true`, ignoring case.
 */
actual val isInDebugMode
    get() = System.getenv("DEBUG") ?.lowercase() == "true"
