package space.kscience.frameswork.features.ui.panel.frames.utils.plotly

import org.w3c.dom.HTMLElement
import kotlin.js.Json

external object Plotly {
    val Plots: Plots
    fun newPlot(
        element: HTMLElement,
        coords: Array<Json>,
        parameters: Json,
        displayOptions: Json
    )

    fun react(element: HTMLElement, coords: Array<Json>, parameters: Json)
}
