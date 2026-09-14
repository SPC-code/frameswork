package space.kscience.frameswork.features.common.server.repos

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import kotlinx.coroutines.CoroutineScope

/**
 * A [GlobalKVRepo] decorator backed by an in-memory, full key-value cache.
 *
 * Reads are served from the cache when possible. Mutations are written to [originalRepo] and then
 * reflected in the cache by [FullKeyValueCacheRepo]. Initial cache population runs in [scope].
 *
 * @param originalRepo persistent repository wrapped by this cache.
 * @param scope coroutine scope used to populate and maintain the cache.
 */
class FullyCachedGlobalKVRepo(
    private val originalRepo: GlobalKVRepo,
    scope: CoroutineScope
) : GlobalKVRepo, FullKeyValueCacheRepo<String, String>(
    originalRepo,
    MapKeyValueRepo(),
    scope,
) {
}
