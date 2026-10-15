package no.fintlabs.personvern.samtykke.samtykke.event

import no.fintlabs.ResourceVerifierService
import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventPublisher
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.operation.OperationType
import no.fintlabs.logger
import no.fintlabs.personvern.samtykke.samtykke.SamtykkeRepository
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper
import java.util.Date

@Service
class SamtykkeEventPublisher(
    adapterProperties: AdapterProperties,
    repository: SamtykkeRepository,
    webClient: WebClient,
    objectMapper: ObjectMapper,
    private val verifier: ResourceVerifierService,
) : EventPublisher<SamtykkeResource>("samtykke", SamtykkeResource::class.java, webClient, adapterProperties, repository, objectMapper) {

    private val log = logger<SamtykkeEventPublisher>()

    @Scheduled(initialDelayString = "9000", fixedDelayString = "1000")
    override fun doCheckForNewEvents() = checkForNewEvents()

    override fun handleEvent(requestFintEvent: RequestFintEvent, resource: SamtykkeResource) {
        val response = createResponse(requestFintEvent)
        response.value = SyncPageEntry()
        val isCreate = requestFintEvent.operationType == OperationType.CREATE
        if (isCreate || verifier.verifySamtykkeResource(resource)) {
            try {
                if (isCreate && resource.opprettet == null) resource.opprettet = Date()
                val saved = repository.saveResources(resource, requestFintEvent)
                response.value = SyncPageEntry.of(saved.systemId.identifikatorverdi, saved)
            } catch (exception: Exception) {
                response.isFailed = true
                response.errorMessage = exception.message
                log.error("Error in repository.saveResources", exception)
            }
        } else {
            response.isRejected = true
            response.rejectReason = "Fields in samtykkeResource cannot be null or empty"
            log.warn("SamtykkeResource not verified")
        }
        submit(response)
    }
}
