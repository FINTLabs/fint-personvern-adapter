package no.fintlabs.adapter.core.register

import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.TenantRegistry
import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.AdapterContract
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Registers one contract per tenant with the gateway. The gateway keys contracts on (username, orgId), so each
 * tenant's contract is independent of the others.
 */
class RegistrationService(
    private val properties: AdapterProperties,
    private val registry: TenantRegistry,
) {
    private val log = logger<RegistrationService>()

    fun registerAll() = registry.forEach { register(it).subscribe() }

    /** Completes with whether the gateway accepted the contract; never errors. */
    fun register(client: TenantClient): Mono<Boolean> =
        client.webClient.post()
            .uri("/provider/register")
            .bodyValue(contract(client))
            .retrieve()
            .toBodilessEntity()
            .retryWhen(
                Retry.fixedDelay(3, Duration.ofSeconds(5))
                    .doBeforeRetry { log.warn("Registration of {} failed, retrying: {}", client, it.failure().describe()) },
            )
            .map { response ->
                client.registered = response.statusCode.is2xxSuccessful
                if (client.registered) log.info("Registered {} ({})", client, response.statusCode) else log.error("Registration of {} returned {}", client, response.statusCode)
                client.registered
            }
            .onErrorResume { error ->
                log.error("Registration of {} failed after retries: {}", client, error.describe())
                Mono.just(false)
            }

    fun contract(client: TenantClient): AdapterContract = AdapterContract.builder()
        .adapterId(client.adapterId)
        .orgId(client.orgId)
        .username(client.username)
        .heartbeatIntervalInMinutes(properties.heartbeatIntervalInMinutes)
        .capabilities(properties.capabilities.values.toHashSet())
        .time(System.currentTimeMillis())
        .build()
}
