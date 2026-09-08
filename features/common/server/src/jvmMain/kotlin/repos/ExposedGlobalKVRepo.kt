package space.kscience.frameswork.features.common.server.repos

import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import org.jetbrains.exposed.v1.jdbc.Database

class ExposedGlobalKVRepo(
    database: Database
) : GlobalKVRepo, ExposedKeyValueRepo<String, String>(
    database = database,
    keyColumnAllocator = { text("key") },
    valueColumnAllocator = { text("value") },
    tableName = "globals"
) {
}