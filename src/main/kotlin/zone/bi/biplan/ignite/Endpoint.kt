package zone.bi.biplan.ignite

import io.micronaut.context.annotation.Value
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
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Named

@Controller
class Endpoint(
    private val ignite: Ignite,
    @Named("ignite") private val threadPool: ExecutorService,
    @Value("\${list-size}") private val size: Int,
    @Value("\${pause}") private val pause : Long
) {

    private val randomizer = ThreadLocalRandom.current()

    private val counter = AtomicLong()

    companion object {
        private val log = LoggerFactory.getLogger(Endpoint::class.java)
    }

    private val cacheEntryProcessor = CacheEntryProcessor<String, List<Map<String, Any>>, Void> { entry, arg ->
        val count = arg[0] as Int
        entry.value = (0 until count).map {
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "timestamp" to OffsetDateTime.now(),
                "amount" to BigDecimal.valueOf(10000)
            )
        }.toMutableList()
        null
    }

    @Post
    fun run(): Mono<Void> {
        val cache = this.ignite.getOrCreateCache<String, List<Map<String, Any>>>("biplanCacheTestCache")
        return Flux.fromIterable((0 until 80).toList())
            .delayElements(Duration.ofMillis(pause), Schedulers.single())
            .flatMap {
                val start = System.currentTimeMillis()
                Mono.create { sink: MonoSink<Void> ->
                    cache.invokeAsync(
                        "consumer-${counter.getAndIncrement()}",
                        cacheEntryProcessor, this.size
                    )
                        .listenAsync({ future ->
                            try {
                                future.get(200).let { sink.success() }
                                log.info("Insert list(size = $size) for ${System.currentTimeMillis() - start} ms")
                            } catch (ex: Throwable) {
                                log.error("Insert list(size = $size) for ${System.currentTimeMillis() - start} ms")
                                sink.error(ex)
                            }
                        }, this.threadPool)
                }
                    .subscribeOn(Schedulers.fromExecutor(threadPool))
            }
            .flatMap {
                val start = System.currentTimeMillis()
                Mono.create { sink: MonoSink<List<Map<String, Any>>> ->
                    cache.getAsync("consumer-${counter.getAndDecrement()}")
                        .listenAsync({ future ->
                            try {
                                future.get(200).let { sink.success() }
                                log.info("Read list(size = $size) for ${System.currentTimeMillis() - start} ms")
                            } catch (ex: Throwable) {
                                log.error("Read list(size = $size) for ${System.currentTimeMillis() - start} ms")
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
            .doOnTerminate {
                this.ignite.destroyCache("biplanCacheTestCache")
                log.info("Destroy cache")
            }
    }
}