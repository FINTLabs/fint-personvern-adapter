package no.fintlabs.personvern.samtykke.behandling.event

import no.fintlabs.ResourceVerifierService
import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventPublisher
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.logger
import no.fintlabs.personvern.samtykke.behandling.BehandlingRepository
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Service
class BehandlingEventPublisher(
    adapterProperties: AdapterProperties,
    repository: BehandlingRepository,
    webClient: WebClient,
    objectMapper: ObjectMapper,
    private val verifier: ResourceVerifierService,
) : EventPublisher<BehandlingResource>("behandling", BehandlingResource::class.java, webClient, adapterProperties, repository, objectMapper) {

    private val log = logger<BehandlingEventPublisher>()

    @Scheduled(initialDelayString = "9000", fixedDelayString = "1000")
    override fun doCheckForNewEvents() = checkForNewEvents()

    override fun handleEvent(requestFintEvent: RequestFintEvent, resource: BehandlingResource) {
        val response = createResponse(requestFintEvent)
        if (verifier.verifyBehandlingResource(resource)) {
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
            response.rejectReason = "Fields in behandlingResource cannot be null or empty"
            log.warn("BehandlingResource not verified")
        }
        submit(response)
    }
}
