package space.kscience.frameswork.features.common.server

actual val isInDebugMode
    get() = System.getenv("DEBUG") ?.lowercase() == "true"
