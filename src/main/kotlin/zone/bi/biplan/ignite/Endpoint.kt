package zone.bi.biplan.ignite

import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.apache.ignite.Ignite
import org.apache.ignite.cache.CacheEntryProcessor
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Named

@Controller
class Endpoint(
    private val ignite: Ignite,
    @Named("ignite") private val threadPool: ExecutorService,
    @Value("\${list-size}") private val size: Int
) {

    private val randomizer = ThreadLocalRandom.current()

    companion object {
        private val log = LoggerFactory.getLogger(Endpoint::class.java)
    }

    @Post
    fun run(@Body body: String): Mono<Void> {
        return Flux.fromIterable((0 until 80).toList())
            .flatMap {
                val cache = this.ignite.getOrCreateCache<String, List<Map<String, Any>>>("biplanCacheTestCache")
                Mono.create { sink: MonoSink<Void> ->
                    cache.invokeAsync(
                        "consumer-${randomizer.nextInt(0, 10000)}",
                        CacheEntryProcessor<String, List<Map<String, Any>>, Void> { entry, arg ->
                            val count = arg[0] as Int
                            entry.value = (0 until count).map {
                                mapOf(
                                    "id" to UUID.randomUUID().toString(),
                                    "timestamp" to OffsetDateTime.now(),
                                    "amount" to BigDecimal.valueOf(10000)
                                )
                            }
                                .toMutableList()
                            null
                        }, this.size)
                        .listenAsync({ future ->
                            try {
                                future.get(200).let { sink.success() }
                            } catch (ex: Throwable) {
                                sink.error(ex)
                            }
                        }, this.threadPool)
                }
                    .subscribeOn(Schedulers.fromExecutor(threadPool))
            }
            .onErrorContinue { ex, obj: Any? ->
                log.error(obj.toString(), ex)
            }
            .collectList()
            .then()
    }
}