package zone.bi.biplan.ignite

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("zone.bi.biplan.ignite")
		.start()
}

