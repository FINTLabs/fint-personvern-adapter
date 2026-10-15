package no.fintlabs.adapter.core.event

import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.adapter.models.sync.SyncPageEntry
import org.springframework.core.ParameterizedTypeReference
import tools.jackson.databind.ObjectMapper

/**
 * Polls the gateway for pending events of one resource as one credential and answers them through the
 * [EventHandler]. One poll returns the events of every org the credential has a contract for, so polling is
 * per credential, not per org, and the answer carries the event's own orgId.
 */
class EventService(
    private val properties: AdapterProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = logger<EventService>()

    fun <T : Any> poll(client: TenantClient, handler: EventHandler<T>) {
        val capability = properties.capability(handler.resourceName)
        val events = fetch(client, capability)
        if (events.isEmpty()) return
        log.info("Received {} events for {} as {}", events.size, capability.entityUri, client.username)
        events.forEach { event ->
            if (event.orgId !in client.orgs) log.warn("Event {} is for {}, which {} is not configured to serve", event.corrId, event.orgId, client)
            post(client, event, response(client, event, handler, handle(event, handler)))
        }
    }

    fun <T : Any> handle(event: RequestFintEvent, handler: EventHandler<T>): EventResult<T> {
        val resource = try {
            objectMapper.readValue(event.value, handler.resourceType)
        } catch (e: Exception) {
            log.warn("Event {} carries a value that is not a {}: {}", event.corrId, handler.resourceType.simpleName, e.message)
            return EventResult.Failed("Could not read ${handler.resourceType.simpleName}: ${e.message}")
        }
        return try {
            handler.handle(event, resource)
        } catch (e: Exception) {
            log.error("Handling event {} failed", event.corrId, e)
            EventResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    fun <T : Any> response(client: TenantClient, event: RequestFintEvent, handler: EventHandler<T>, result: EventResult<T>): ResponseFintEvent {
        val response = ResponseFintEvent.builder()
            .corrId(event.corrId)
            .orgId(event.orgId)
            .adapterId(client.adapterId)
            .operationType(event.operationType)
            .handledAt(System.currentTimeMillis())
            .value(SyncPageEntry())
            .build()
        when (result) {
            is EventResult.Saved -> response.value = SyncPageEntry.of(handler.identifier(result.resource), result.resource)
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

    private fun fetch(client: TenantClient, capability: AdapterCapability): List<RequestFintEvent> =
        try {
            client.webClient.get()
                .uri("/provider/event${capability.entityUri}")
                .retrieve()
                .bodyToMono(EVENT_LIST)
                .block() ?: emptyList()
        } catch (e: Exception) {
            log.error("Checking events for {} as {} failed: {}", capability.entityUri, client.username, e.describe())
            emptyList()
        }

    private fun post(client: TenantClient, event: RequestFintEvent, response: ResponseFintEvent) {
        client.webClient.post()
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
