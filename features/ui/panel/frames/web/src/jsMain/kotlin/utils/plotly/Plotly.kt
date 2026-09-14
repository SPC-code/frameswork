package space.kscience.frameswork.features.ui.panel.frames.utils.plotly

import org.w3c.dom.HTMLElement
import kotlin.js.Json

/**
 * Kotlin/JS facade for the global Plotly.js API.
 *
 * The host page must load Plotly.js and expose `Plotly` globally before these declarations are
 * called; this module does not load the JavaScript library.
 */
external object Plotly {
    /** Plot-management operations exposed by Plotly.js. */
    val Plots: Plots

    /** Creates a plot in [element] from [coords], layout [parameters], and [displayOptions]. */
    fun newPlot(
        element: HTMLElement,
        coords: Array<Json>,
        parameters: Json,
        displayOptions: Json
    )

    /** Updates the data and layout of the plot hosted by [element]. */
    fun react(element: HTMLElement, coords: Array<Json>, parameters: Json)
}
