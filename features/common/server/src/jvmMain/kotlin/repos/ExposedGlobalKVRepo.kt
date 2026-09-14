package space.kscience.frameswork.features.common.server.repos

import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * PostgreSQL/Exposed implementation of [GlobalKVRepo] stored in the `globals` table.
 *
 * The table uses text columns named `key` and `value`.
 *
 * @param database Exposed database in which the repository table is managed.
 */
class ExposedGlobalKVRepo(
    database: Database
) : GlobalKVRepo, ExposedKeyValueRepo<String, String>(
    database = database,
    keyColumnAllocator = { text("key") },
    valueColumnAllocator = { text("value") },
    tableName = "globals"
) {
}
