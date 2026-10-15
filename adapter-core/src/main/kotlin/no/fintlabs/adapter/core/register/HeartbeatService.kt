package no.fintlabs.adapter.core.register

import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.TenantRegistry
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.AdapterHeartbeat

/**
 * Sends a heartbeat per registered (credential, org); an org without a contract is registered again instead.
 */
class HeartbeatService(
    private val registry: TenantRegistry,
    private val registrationService: RegistrationService,
) {
    private val log = logger<HeartbeatService>()

    fun beatAll() = registry.forEach { client -> client.orgs.forEach { beat(client, it) } }

    fun beat(client: TenantClient, orgId: String) {
        if (!client.isRegistered(orgId)) {
            log.info("{} has no contract for {} yet, registering instead of sending heartbeat", client, orgId)
            registrationService.register(client, orgId).subscribe()
            return
        }
        val heartbeat = AdapterHeartbeat.builder()
            .adapterId(client.adapterId)
            .username(client.username)
            .orgId(orgId)
            .time(System.currentTimeMillis())
            .build()
        client.webClient.post()
            .uri("/provider/heartbeat")
            .bodyValue(heartbeat)
            .retrieve()
            .bodyToMono(String::class.java)
            .subscribe(
                { log.debug("Heartbeat for {} ({}) answered: {}", client, orgId, it) },
                { log.error("Heartbeat for {} ({}) failed: {}", client, orgId, it.describe()) },
            )
    }
}
