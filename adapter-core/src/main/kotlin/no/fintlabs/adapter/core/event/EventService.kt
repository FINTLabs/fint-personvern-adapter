package no.fintlabs.adapter.core.event

import no.fintlabs.adapter.core.ResourceBinding
import no.fintlabs.adapter.core.ResourceRef
import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.novari.fint.model.resource.FintResource
import org.springframework.core.ParameterizedTypeReference
import tools.jackson.databind.ObjectMapper

/**
 * Polls the gateway for pending events as one credential and answers them through the resources'
 * [EventHandler]s. One poll per domain returns the events of every resource the contract covers and every org
 * the credential has a contract for, so polling is per credential, not per resource or org, and the answer
 * carries the event's own orgId.
 */
class EventService(
    private val objectMapper: ObjectMapper,
) {
    private val log = logger<EventService>()

    /** One request per domain the handled resources span; the gateway serves every package the credential has a role for. */
    fun poll(
        client: TenantClient,
        bindings: Collection<ResourceBinding<*>>,
    ) {
        val handled = bindings.filter { it.handler != null }.associateBy { it.ref }
        handled.keys.map { it.domainName }.distinct().forEach { domain ->
            val events = fetch(client, domain)
            if (events.isEmpty()) return@forEach
            log.info("Received {} events for {} as {}", events.size, domain, client.username)
            events.forEach { event ->
                val binding = handled[ResourceRef(event.domainName, event.packageName, event.resourceName)]
                if (binding == null) {
                    log.warn(
                        "Event {} is for {}/{}/{}, which has no EventHandler; left for its time to live to expire",
                        event.corrId,
                        event.domainName,
                        event.packageName,
                        event.resourceName,
                    )
                } else {
                    answer(client, event, binding)
                }
            }
        }
    }

    private fun <T : FintResource> answer(
        client: TenantClient,
        event: RequestFintEvent,
        binding: ResourceBinding<T>,
    ) {
        val handler = binding.handler ?: return
        if (event.orgId !in client.orgs) {
            log.warn(
                "Event {} is for {}, which {} is not configured to serve",
                event.corrId,
                event.orgId,
                client,
            )
        }
        post(client, event, response(client, event, binding, handle(event, binding, handler)))
    }

    fun <T : FintResource> handle(
        event: RequestFintEvent,
        binding: ResourceBinding<T>,
        handler: EventHandler<T>,
    ): EventResult<T> {
        val resource =
            try {
                objectMapper.readValue(event.value, binding.resourceType)
            } catch (e: Exception) {
                log.warn(
                    "Event {} carries a value that is not a {}: {}",
                    event.corrId,
                    binding.resourceType.simpleName,
                    e.message,
                )
                return EventResult.Failed("Could not read ${binding.resourceType.simpleName}: ${e.message}")
            }
        return try {
            handler.handle(event, resource)
        } catch (e: Exception) {
            log.error("Handling event {} failed", event.corrId, e)
            EventResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    fun <T : FintResource> response(
        client: TenantClient,
        event: RequestFintEvent,
        binding: ResourceBinding<T>,
        result: EventResult<T>,
    ): ResponseFintEvent {
        val response =
            ResponseFintEvent
                .builder()
                .corrId(event.corrId)
                .orgId(event.orgId)
                .adapterId(client.adapterId)
                .operationType(event.operationType)
                .handledAt(System.currentTimeMillis())
                .value(SyncPageEntry())
                .build()
        when (result) {
            is EventResult.Saved -> {
                response.value =
                    SyncPageEntry.of(binding.identifier(result.resource), result.resource)
            }

            is EventResult.Rejected -> {
                response.isRejected = true
                response.rejectReason = result.reason
            }

            is EventResult.Failed -> {
                response.isFailed = true
                response.errorMessage = result.message
            }
        }
        return response
    }

    private fun fetch(
        client: TenantClient,
        domain: String,
    ): List<RequestFintEvent> =
        try {
            client.webClient
                .get()
                .uri("/provider/event/$domain")
                .retrieve()
                .bodyToMono(EVENT_LIST)
                .block() ?: emptyList()
        } catch (e: Exception) {
            log.error("Checking events for {} as {} failed: {}", domain, client.username, e.describe())
            emptyList()
        }

    private fun post(
        client: TenantClient,
        event: RequestFintEvent,
        response: ResponseFintEvent,
    ) {
        client.webClient
            .post()
            .uri("/provider/event")
            .bodyValue(response)
            .retrieve()
            .toBodilessEntity()
            .doOnNext { log.info("Answer to event {} ({}) returned {}", event.corrId, event.orgId, it.statusCode) }
            .doOnError { log.error("Answer to event {} ({}) failed: {}", event.corrId, event.orgId, it.describe()) }
            .onErrorComplete()
            .block()
    }

    companion object {
        private val EVENT_LIST = object : ParameterizedTypeReference<List<RequestFintEvent>>() {}
    }
}
