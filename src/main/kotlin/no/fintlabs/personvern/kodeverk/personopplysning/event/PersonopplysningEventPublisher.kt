package no.fintlabs.personvern.kodeverk.personopplysning.event

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventPublisher
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.logger
import no.fintlabs.personvern.kodeverk.personopplysning.PersonopplysningRepository
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Service
class PersonopplysningEventPublisher(
    adapterProperties: AdapterProperties,
    repository: PersonopplysningRepository,
    webClient: WebClient,
    objectMapper: ObjectMapper,
) : EventPublisher<PersonopplysningResource>("personopplysning", PersonopplysningResource::class.java, webClient, adapterProperties, repository, objectMapper) {

    private val log = logger<PersonopplysningEventPublisher>()

    @Scheduled(initialDelayString = "9000", fixedDelayString = "1000")
    override fun doCheckForNewEvents() = checkForNewEvents()

    override fun handleEvent(requestFintEvent: RequestFintEvent, resource: PersonopplysningResource) {
        val response = createResponse(requestFintEvent)
        try {
            val saved = repository.saveResources(resource, requestFintEvent)
            response.value = SyncPageEntry.of(saved.systemId.identifikatorverdi, saved)
        } catch (exception: Exception) {
            response.isFailed = true
            response.errorMessage = exception.message
            log.error("Error in repository.saveResources", exception)
        }
        submit(response)
    }
}
