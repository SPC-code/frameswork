package space.kscience.frameswork.features.ui.panel.frames.common

/**
 * Relative URL path segments shared by the frames-panel client and server.
 *
 * The segments do not include leading or trailing slashes. This object only defines the wire
 * contract; it does not install HTTP or WebSocket routes.
 */
object PanelCameraConstants {
    /** Root path segment under which the frames-panel operations are grouped. */
    const val rootPathPart = "cameras"

    /** Child path segment used to request the available processor names. */
    const val getAvailableProcessorsPathPart = "getAvailableProcessors"

    /** Child path segment used to request the frame sources exposed by a processor. */
    const val getAvailableCamerasPathPart = "getAvailableCameras"

    /** Child path segment used for the frame-streaming connection. */
    const val getFramesPathPart = "getFrames"
}
