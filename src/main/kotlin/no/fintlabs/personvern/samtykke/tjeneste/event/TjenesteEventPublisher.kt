package no.fintlabs.personvern.samtykke.tjeneste.event

import no.fintlabs.ResourceVerifierService
import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventPublisher
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.logger
import no.fintlabs.personvern.samtykke.tjeneste.TjenesteRepository
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Service
class TjenesteEventPublisher(
    adapterProperties: AdapterProperties,
    repository: TjenesteRepository,
    webClient: WebClient,
    objectMapper: ObjectMapper,
    private val verifier: ResourceVerifierService,
) : EventPublisher<TjenesteResource>("tjeneste", TjenesteResource::class.java, webClient, adapterProperties, repository, objectMapper) {

    private val log = logger<TjenesteEventPublisher>()

    @Scheduled(initialDelayString = "9000", fixedDelayString = "1000")
    override fun doCheckForNewEvents() = checkForNewEvents()

    override fun handleEvent(requestFintEvent: RequestFintEvent, resource: TjenesteResource) {
        val response = createResponse(requestFintEvent)
        if (verifier.verifyTjenesteResource(resource)) {
            try {
                val saved = repository.saveResources(resource, requestFintEvent)
                response.value = SyncPageEntry.of(saved.systemId.identifikatorverdi, saved)
            } catch (exception: Exception) {
                response.isFailed = true
                response.errorMessage = exception.message
                log.error("Error in repository.saveResources", exception)
            }
        } else {
            response.isRejected = true
            response.rejectReason = "Fields in tjenesteResource cannot be null or empty"
            log.warn("TjenesteResource not verified")
        }
        submit(response)
    }
}
