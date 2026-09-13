package space.kscience.frameswork.features.panel.web

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.ktor.client.bodyOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import space.kscience.frameswork.features.panel.common.PanelConstants
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

class KtorPanelFeature(
    private val client: HttpClient
) : PanelFeature {
    override suspend fun getPanelConfig(): PanelInfo? {
        return client.get(PanelConstants.getPanelFullPath).bodyOrNull<PanelInfo> {
            it.bodyAsText() != "null"
        }
    }
    override suspend fun getDefaultConfig(): PanelInfo? {
        return client.get(PanelConstants.getPanelDefaultPath).bodyOrNull<PanelInfo> {
            it.bodyAsText() != "null"
        }
    }

    override suspend fun setPanelConfig(config: PanelInfo): Boolean {
        return client.post(PanelConstants.setPanelFullPath) {
            setBody(config)
        }.body()
    }
}