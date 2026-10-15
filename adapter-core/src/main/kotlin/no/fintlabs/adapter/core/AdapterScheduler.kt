package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.event.EventService
import no.fintlabs.adapter.core.register.HeartbeatService
import no.fintlabs.adapter.core.register.RegistrationService
import no.fintlabs.adapter.core.sync.FullSyncService
import org.springframework.context.SmartLifecycle
import org.springframework.scheduling.TaskScheduler
import java.time.Instant
import java.util.concurrent.ScheduledFuture

/**
 * Wires configuration to work: registers every tenant, and schedules heartbeat, full syncs and event polling
 * from the intervals in [AdapterProperties] and the bound resources.
 */
class AdapterScheduler(
    private val properties: AdapterProperties,
    private val registry: TenantRegistry,
    private val bindings: ResourceBindings,
    private val registrationService: RegistrationService,
    private val heartbeatService: HeartbeatService,
    private val fullSyncService: FullSyncService,
    private val eventService: EventService,
    private val taskScheduler: TaskScheduler,
) : SmartLifecycle {
    private val log = logger<AdapterScheduler>()
    private val futures = mutableListOf<ScheduledFuture<*>>()

    @Volatile
    private var running = false

    override fun start() {
        verifyTenants()
        log.info("Starting adapter for {} tenant(s) {} serving {}", registry.tenants.size, registry.tenants, bindings.bindings)
        registrationService.registerAll()

        val now = Instant.now()
        futures += taskScheduler.scheduleAtFixedRate(task("heartbeat") { heartbeatService.beatAll() }, now.plus(properties.heartbeatInterval), properties.heartbeatInterval)

        bindings.forEach { binding ->
            log.info("Full sync of {} every {}{}", binding.ref, binding.fullSyncInterval, if (binding.handler != null) ", events for ${binding.handler.operations}" else ", no events")
            futures += taskScheduler.scheduleAtFixedRate(
                task("full sync of ${binding.ref}") { registry.forEach { client -> client.orgs.forEach { org -> fullSyncService.sync(client, org, binding) } } },
                now.plus(properties.fullSyncInitialDelay),
                binding.fullSyncInterval,
            )
        }

        registry.forEach { client ->
            bindings.filter { it.handler != null }.forEach { binding ->
                futures += taskScheduler.scheduleWithFixedDelay(
                    task("event poll of ${binding.ref} as ${client.username}") { eventService.poll(client, binding) },
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

    private fun verifyTenants() {
        require(registry.tenants.isNotEmpty()) { "No enabled tenants under fint.adapter.tenants" }
        val orgTwice = properties.orgs.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(orgTwice.isEmpty()) { "Orgs served by more than one tenant: $orgTwice" }
    }

    private fun task(name: String, body: () -> Unit) = Runnable {
        try {
            body()
        } catch (e: Exception) {
            log.error("Scheduled {} failed", name, e)
        }
    }
}
