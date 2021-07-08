package zone.bi.biplan.ignite

import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.apache.ignite.Ignite
import org.apache.ignite.cache.CacheEntryProcessor
import org.slf4j.LoggerFactory
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
    @Value("\${list-size}") private val  size : Int
) {

    private val randomizer = ThreadLocalRandom.current()

    companion object {
        private val log = LoggerFactory.getLogger(Endpoint::class.java)
    }

    @Post
    fun run(body: Mono<String>): Mono<Void> {
        return body
            .flatMapIterable { (0 until 80).toList() }
            .flatMap {
                val cache = this.ignite.cache<String, List<Map<String, Any>>>("biplanCacheTestCache")
                Mono.create { sink: MonoSink<Void> ->
                    cache.invokeAsync(
                        "consumer-${randomizer.nextInt(0, 10000)}",
                        CacheEntryProcessor<String, List<Map<String, Any>>, Void> { entry, _ ->
                            entry.value = (0 until size).map {
                                mapOf(
                                    "id" to UUID.randomUUID().toString(),
                                    "timestamp" to OffsetDateTime.now(),
                                    "amount" to BigDecimal.valueOf(10000)
                                )
                            }
                                .toMutableList()
                            null
                        })
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