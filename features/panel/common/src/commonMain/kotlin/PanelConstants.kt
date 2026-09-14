package space.kscience.frameswork.features.panel.common

/**
 * Relative path segments shared by panel HTTP clients and servers.
 *
 * These constants do not install routes. The server and web modules use them to keep their route
 * definitions and requests in sync.
 */
object PanelConstants {
    /** The path segment under which all panel operations are grouped. */
    const val rootPanelPath = "panel"

    /** The child path segment used to read the current panel configuration. */
    const val getPanelSubpath = "get"

    /** The child path segment used to replace the current panel configuration. */
    const val setPanelSubpath = "set"

    /** The child path segment used to read the default panel configuration. */
    const val getPanelDefaultSubpath = "default"

    /** The relative path composed from [rootPanelPath] and [getPanelSubpath]. */
    const val getPanelFullPath = "$rootPanelPath/$getPanelSubpath"

    /** The relative path composed from [rootPanelPath] and [getPanelDefaultSubpath]. */
    const val getPanelDefaultPath = "$rootPanelPath/$getPanelDefaultSubpath"

    /** The relative path composed from [rootPanelPath] and [setPanelSubpath]. */
    const val setPanelFullPath = "$rootPanelPath/$setPanelSubpath"
}
