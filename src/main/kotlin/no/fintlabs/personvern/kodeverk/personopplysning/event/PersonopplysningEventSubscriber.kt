package no.fintlabs.personvern.kodeverk.personopplysning.event

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventSubscriber
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.logger
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PersonopplysningEventSubscriber(
    webClient: WebClient,
    adapterProperties: AdapterProperties,
    publisher: PersonopplysningEventPublisher,
) : EventSubscriber<PersonopplysningResource, PersonopplysningEventPublisher>(webClient, adapterProperties, publisher, "personopplysning") {

    private val log = logger<PersonopplysningEventSubscriber>()

    override fun responsePostingEvent(response: ResponseEntity<Void>, responseFintEvent: ResponseFintEvent) {
        log.info("Posting response for event {} returned {}.", responseFintEvent.corrId, response.statusCode.value())
    }
}
