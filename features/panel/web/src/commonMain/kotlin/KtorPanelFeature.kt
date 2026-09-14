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

/**
 * A [PanelFeature] implementation that delegates panel operations to relative HTTP endpoints.
 *
 * The read operations decode a [PanelInfo] response unless its body is exactly the JSON literal
 * `null`. The write operation sends [PanelInfo] as the request body and decodes the response as a
 * [Boolean]. Transport and serialization failures from [client] are propagated to the caller.
 *
 * @property client Ktor client used to resolve the relative paths in [PanelConstants] and serialize
 * requests and responses.
 */
class KtorPanelFeature(
    private val client: HttpClient
) : PanelFeature {
    /**
     * Requests the current panel layout from [PanelConstants.getPanelFullPath].
     *
     * @return the decoded layout, or `null` when the response body is exactly `null`.
     */
    override suspend fun getPanelConfig(): PanelInfo? {
        return client.get(PanelConstants.getPanelFullPath).bodyOrNull<PanelInfo> {
            it.bodyAsText() != "null"
        }
    }

    /**
     * Requests the default panel layout from [PanelConstants.getPanelDefaultPath].
     *
     * @return the decoded default layout, or `null` when the response body is exactly `null`.
     */
    override suspend fun getDefaultConfig(): PanelInfo? {
        return client.get(PanelConstants.getPanelDefaultPath).bodyOrNull<PanelInfo> {
            it.bodyAsText() != "null"
        }
    }

    /**
     * Posts [config] to [PanelConstants.setPanelFullPath].
     *
     * @return the Boolean value decoded from the response body.
     */
    override suspend fun setPanelConfig(config: PanelInfo): Boolean {
        return client.post(PanelConstants.setPanelFullPath) {
            setBody(config)
        }.body()
    }
}
