package zone.bi.biplan.ignite

import io.micronaut.core.type.GenericArgument
import io.micronaut.runtime.Micronaut.build
import org.apache.ignite.Ignite

fun main(args: Array<String>) {
    build()
        .args(*args)
        .eagerInitSingletons(true)
        .packages("zone.bi.biplan.ignite")
        .start()
        .use { context ->
            val caches = context
                .getProperty("deleted.caches", object : GenericArgument<Set<String>>() {}).orElse(emptySet())
            context.getBean(Ignite::class.java).use { ignite ->
                ignite.destroyCaches(caches)
            }
        }
}

