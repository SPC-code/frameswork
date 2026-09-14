package space.kscience.frameswork.features.common.web.utils

import io.ktor.http.URLBuilder
import io.ktor.http.clone

/**
 * Updates the properties of the current [URLBuilder] instance with values from the specified `defaultValues`
 * instance if those properties are not already set or are in an invalid initial state.
 *
 * @param defaultValues The [URLBuilder] instance containing default values to fill in for missing or invalid
 *                       parts of the [this] [URLBuilder] instance.
 */
fun URLBuilder.fillAbsentPartsWith(defaultValues: URLBuilder) {
    protocolOrNull = protocolOrNull ?: defaultValues.protocolOrNull
    host = host.takeIf { it.isNotEmpty() } ?: defaultValues.host
    port = port.takeIf { it != 0 } ?: defaultValues.port
    encodedPathSegments = encodedPathSegments.takeIf { it.isNotEmpty() } ?: defaultValues.encodedPathSegments
    encodedUser = encodedUser ?: defaultValues.encodedUser
    encodedPassword = encodedPassword ?: defaultValues.encodedPassword
    encodedParameters = encodedParameters.takeIf { it.isEmpty() == false } ?: defaultValues.encodedParameters
    encodedFragment = encodedFragment.takeIf { it.isNotEmpty() } ?: defaultValues.encodedFragment
    trailingQuery = trailingQuery || defaultValues.trailingQuery
}

/**
 * Merges [fallbackValues] into this builder while appending the fallback path and combining query
 * parameters.
 *
 * Scalar fields already present in this builder are retained. Its path is followed by the fallback
 * path; query parameters from both builders are preserved. When [forceSetDefaultProtocol] is true,
 * the fallback protocol replaces this builder's protocol, which lets a relative WebSocket request
 * keep its `ws` or `wss` scheme while adopting an HTTP base URL.
 *
 * @param fallbackValues values used for absent scalar fields and appended path/query components.
 * @param forceSetDefaultProtocol whether a non-null fallback protocol must replace the receiver's
 * protocol.
 */
fun URLBuilder.appendOrSetPartsWith(fallbackValues: URLBuilder, forceSetDefaultProtocol: Boolean = false) {
    val localCopyOfDefaults = fallbackValues.clone()
    protocolOrNull = if (forceSetDefaultProtocol && localCopyOfDefaults.protocolOrNull != null) localCopyOfDefaults.protocolOrNull else (protocolOrNull ?: localCopyOfDefaults.protocolOrNull)
    host = host.takeIf { it.isNotEmpty() } ?: localCopyOfDefaults.host
    port = port.takeIf { it != 0 } ?: localCopyOfDefaults.port
    encodedPathSegments = encodedPathSegments + localCopyOfDefaults.encodedPathSegments
    encodedUser = encodedUser ?: localCopyOfDefaults.encodedUser
    encodedPassword = encodedPassword ?: localCopyOfDefaults.encodedPassword
    val defaultEncodedParameters = localCopyOfDefaults.encodedParameters
    encodedParameters.entries().forEach {
        defaultEncodedParameters.appendAll(it.key, it.value)
    }
    encodedParameters = defaultEncodedParameters
    encodedFragment = encodedFragment.takeIf { it.isNotEmpty() } ?: localCopyOfDefaults.encodedFragment
    trailingQuery = trailingQuery || localCopyOfDefaults.trailingQuery
}

/**
 * Replaces every URL component in this builder with the corresponding component from [from].
 *
 * @param from the builder whose current state is copied.
 */
fun URLBuilder.set(from: URLBuilder) {
    protocolOrNull = from.protocolOrNull
    host = from.host
    port = from.port
    encodedPathSegments = from.encodedPathSegments
    encodedUser = from.encodedUser
    encodedPassword = from.encodedPassword
    encodedParameters = from.encodedParameters
    encodedFragment = from.encodedFragment
    trailingQuery = from.trailingQuery
}
