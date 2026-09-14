package space.kscience.frameswork.features.common.server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.v1.jdbc.Database
import org.postgresql.Driver

/** Key used by the `Map<String, Any>.database` extension to locate the default [DatabaseConfig]. */
const val defaultDatabaseParamsName = "defaultDatabase"

/**
 * Returns the Exposed database handle from the [DatabaseConfig] stored under
 * [defaultDatabaseParamsName], or `null` when that entry is absent or has another type.
 */
inline val Map<String, Any>.database: Database?
    get() = (get(defaultDatabaseParamsName) as? DatabaseConfig) ?.database

/**
 * Serializable PostgreSQL connection settings and their Exposed database handle.
 *
 * @property url JDBC connection URL.
 * @property driver fully qualified JDBC driver class name.
 * @property username database user name.
 * @property password database password.
 */
@Serializable
data class DatabaseConfig(
    val url: String = "jdbc:postgresql://localhost:5432/tablet",
    val driver: String = Driver::class.qualifiedName!!,
    val username: String = "",
    val password: String = ""
) {
    /** Exposed database handle created from this configuration's connection settings. */
    @Transient
    val database: Database = Database.connect(
        url,
        driver,
        username,
        password
    )
}
