package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventService
import no.fintlabs.adapter.core.register.HeartbeatService
import no.fintlabs.adapter.core.register.RegistrationService
import no.fintlabs.adapter.core.sync.FullSyncService
import no.fintlabs.adapter.core.sync.ResourceSource
import org.springframework.context.SmartLifecycle
import org.springframework.scheduling.TaskScheduler
import java.time.Instant
import java.util.concurrent.ScheduledFuture

/**
 * Wires configuration to work: checks that every capability has a source, registers every tenant, and
 * schedules heartbeat, full syncs and event polling from the intervals in [AdapterProperties].
 */
class AdapterScheduler(
    private val properties: AdapterProperties,
    private val registry: TenantRegistry,
    private val registrationService: RegistrationService,
    private val heartbeatService: HeartbeatService,
    private val fullSyncService: FullSyncService,
    private val eventService: EventService,
    private val sources: List<ResourceSource<*>>,
    private val handlers: List<EventHandler<*>>,
    private val taskScheduler: TaskScheduler,
) : SmartLifecycle {
    private val log = logger<AdapterScheduler>()
    private val futures = mutableListOf<ScheduledFuture<*>>()

    @Volatile
    private var running = false

    override fun start() {
        verifyConfiguration()
        log.info("Starting adapter for {} tenant(s): {}", registry.tenants.size, registry.tenants)
        registrationService.registerAll()

        val now = Instant.now()
        futures += taskScheduler.scheduleAtFixedRate(task("heartbeat") { heartbeatService.beatAll() }, now.plus(properties.heartbeatInterval), properties.heartbeatInterval)

        sources.forEach { source ->
            val interval = properties.fullSyncIntervalFor(properties.capability(source.resourceName))
            log.info("Full sync of {} every {}", source.resourceName, interval)
            futures += taskScheduler.scheduleAtFixedRate(
                task("full sync of ${source.resourceName}") { registry.forEach { client -> fullSyncService.sync(client, source) } },
                now.plus(properties.fullSyncInitialDelay),
                interval,
            )
        }

        registry.forEach { client ->
            handlers.forEach { handler ->
                futures += taskScheduler.scheduleWithFixedDelay(
                    task("event poll of ${handler.resourceName} for ${client.orgId}") { eventService.poll(client, handler) },
                    now.plus(properties.fullSyncInitialDelay),
                    properties.eventPollInterval,
                )
            }
        }
        running = true
    }

    override fun stop() {
        futures.forEach { it.cancel(false) }
        futures.clear()
        running = false
    }

    override fun isRunning(): Boolean = running

    /** After the web server and other beans, so the whole context is up before anything talks to the gateway. */
    override fun getPhase(): Int = Int.MAX_VALUE - 1024

    private fun verifyConfiguration() {
        val configured = properties.capabilities.keys
        val sourced = sources.map { it.resourceName }
        val handled = handlers.map { it.resourceName }

        val missing = configured - sourced.toSet()
        require(missing.isEmpty()) { "Capabilities without a ResourceSource bean: $missing" }
        val unknown = (sourced + handled).filterNot { it in configured }
        require(unknown.isEmpty()) { "ResourceSource/EventHandler beans without a capability under fint.adapter.capabilities: $unknown" }
        val duplicates = sourced.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "More than one ResourceSource bean for: $duplicates" }
        require(registry.tenants.isNotEmpty()) { "No enabled tenants under fint.adapter.tenants" }
    }

    private fun task(name: String, body: () -> Unit) = Runnable {
        try {
            body()
        } catch (e: Exception) {
            log.error("Scheduled {} failed", name, e)
        }
    }
}
