package zone.bi.biplan.ignite

import io.micronaut.runtime.Micronaut.build
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.eagerInitSingletons(true)
		.packages("zone.bi.biplan.ignite")
		.start()
}

