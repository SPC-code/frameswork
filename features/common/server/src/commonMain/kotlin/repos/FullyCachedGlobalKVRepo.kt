package space.kscience.frameswork.features.common.server.repos

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import kotlinx.coroutines.CoroutineScope

/**
 * Cache that fully caches the original repository. Any changes are passing through this repo and followed in change of cache
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