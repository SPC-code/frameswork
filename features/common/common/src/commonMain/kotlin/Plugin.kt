package space.kscience.frameswork.features.common.common

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.e
import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import io.ktor.http.ContentType
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.Module
import org.koin.dsl.binds
import space.kscience.frameswork.features.common.common.utils.DateTimeSerializer

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
//        single { SerializersModuleConfigurator(getAllDistinct()) }
        single<ContentType> { ContentType.Application.Json }
        single {
            CoroutineScope(
                Dispatchers.Default
            ).LinkedSupervisorScope(
                CoroutineExceptionHandler { context, throwable ->
                    KSLog.e("Default CoroutineExceptionHandler", "Unhandled exception in context $context", throwable)
                }
            )
        }
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, DateTime::class, DateTimeSerializer)
                polymorphic(Any::class, String::class, String.serializer())
                polymorphic(Any::class, Int::class, Int.serializer())
                polymorphic(Any::class, Long::class, Long.serializer())
                polymorphic(Any::class, Short::class, Short.serializer())
                polymorphic(Any::class, Byte::class, Byte.serializer())
                polymorphic(Any::class, Float::class, Float.serializer())
                polymorphic(Any::class, Double::class, Double.serializer())
            }
        }

        single {
            Json {
                ignoreUnknownKeys = true
                useArrayPolymorphism = true
                serializersModule = SerializersModule {
                    getAllDistinct<SerializersModule>().forEach {
                        include(it)
                    }
                }
                allowStructuredMapKeys = true
                allowSpecialFloatingPointValues = true
            }
        } binds arrayOf(
            StringFormat::class,
            SerialFormat::class
        )
    }
}
