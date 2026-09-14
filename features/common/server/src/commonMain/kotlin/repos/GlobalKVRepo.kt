package space.kscience.frameswork.features.common.server.repos

import dev.inmo.micro_utils.repos.KeyValueRepo

/**
 * Repository for application-wide string values addressed by string keys.
 */
interface GlobalKVRepo : KeyValueRepo<String, String>
