package space.kscience.frameswork.features.ui.panel.frames.utils.plotly

import org.w3c.dom.Element

/** Kotlin/JS facade for Plotly.js plot-management operations. */
external interface Plots {
    /** Recalculates a plot's size after its containing [element] changes dimensions. */
    fun resize(element: Element)
}
