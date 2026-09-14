package space.kscience.frameswork.features.common.server

/**
 * Indicates whether the current runtime should enable development-oriented server behavior.
 *
 * Platform implementations define how the flag is detected.
 */
expect val isInDebugMode: Boolean
