package no.fintlabs.personvern.samtykke.samtykke.event

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventSubscriber
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.logger
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SamtykkeEventSubscriber(
    webClient: WebClient,
    adapterProperties: AdapterProperties,
    publisher: SamtykkeEventPublisher,
) : EventSubscriber<SamtykkeResource, SamtykkeEventPublisher>(webClient, adapterProperties, publisher, "samtykke") {

    private val log = logger<SamtykkeEventSubscriber>()

    override fun responsePostingEvent(response: ResponseEntity<Void>, responseFintEvent: ResponseFintEvent) {
        log.info("Posting response for event {} returned {}.", responseFintEvent.corrId, response.statusCode.value())
    }
}
