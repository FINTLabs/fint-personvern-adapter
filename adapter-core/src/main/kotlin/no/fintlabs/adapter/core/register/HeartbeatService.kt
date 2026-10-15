package no.fintlabs.adapter.core.register

import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.TenantRegistry
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.AdapterHeartbeat

/**
 * Sends a heartbeat per registered tenant; an unregistered tenant is registered again instead.
 */
class HeartbeatService(
    private val registry: TenantRegistry,
    private val registrationService: RegistrationService,
) {
    private val log = logger<HeartbeatService>()

    fun beatAll() = registry.forEach { beat(it) }

    fun beat(client: TenantClient) {
        if (!client.registered) {
            log.info("{} is not registered yet, registering instead of sending heartbeat", client)
            registrationService.register(client).subscribe()
            return
        }
        val heartbeat = AdapterHeartbeat.builder()
            .adapterId(client.adapterId)
            .username(client.username)
            .orgId(client.orgId)
            .time(System.currentTimeMillis())
            .build()
        client.webClient.post()
            .uri("/provider/heartbeat")
            .bodyValue(heartbeat)
            .retrieve()
            .bodyToMono(String::class.java)
            .subscribe(
                { log.debug("Heartbeat for {} answered: {}", client, it) },
                { log.error("Heartbeat for {} failed: {}", client, it.describe()) },
            )
    }
}
