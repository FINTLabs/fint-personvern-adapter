package no.fintlabs.adapter.core.register

import no.fintlabs.adapter.core.ResourceBindings
import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.TenantRegistry
import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.AdapterContract
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Registers one contract per (credential, org) with the gateway, which keys contracts on username + orgId.
 * The contract lists every bound resource for sync and, for each resource with an event handler, the
 * operations it answers as event capabilities.
 */
class RegistrationService(
    private val properties: AdapterProperties,
    private val registry: TenantRegistry,
    private val bindings: ResourceBindings,
) {
    private val log = logger<RegistrationService>()

    fun registerAll() = registry.forEach { client -> registerAllOrgs(client).subscribe() }

    fun registerAllOrgs(client: TenantClient): Flux<Boolean> = Flux.fromIterable(client.orgs).concatMap { register(client, it) }

    /** Completes with whether the gateway accepted the contract; never errors. */
    fun register(client: TenantClient, orgId: String): Mono<Boolean> =
        client.webClient.post()
            .uri("/provider/register")
            .bodyValue(contract(client, orgId))
            .retrieve()
            .toBodilessEntity()
            .retryWhen(
                Retry.fixedDelay(3, Duration.ofSeconds(5))
                    .doBeforeRetry { log.warn("Registration of {} for {} failed, retrying: {}", client, orgId, it.failure().describe()) },
            )
            .map { response ->
                val ok = response.statusCode.is2xxSuccessful
                client.setRegistered(orgId, ok)
                if (ok) log.info("Registered {} for {} ({})", client, orgId, response.statusCode) else log.error("Registration of {} for {} returned {}", client, orgId, response.statusCode)
                ok
            }
            .onErrorResume { error ->
                client.setRegistered(orgId, false)
                log.error("Registration of {} for {} failed after retries: {}", client, orgId, error.describe())
                Mono.just(false)
            }

    fun contract(client: TenantClient, orgId: String): AdapterContract = AdapterContract.builder()
        .adapterId(client.adapterId)
        .orgId(orgId)
        .username(client.username)
        .heartbeatIntervalInMinutes(properties.heartbeatIntervalInMinutes)
        .capabilities(bindings.capabilities)
        .eventCapabilities(bindings.eventCapabilities)
        .time(System.currentTimeMillis())
        .build()
}
