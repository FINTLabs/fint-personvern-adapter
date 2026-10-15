package no.fintlabs.personvern.samtykke.behandling.event

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventSubscriber
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.logger
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BehandlingEventSubscriber(
    webClient: WebClient,
    adapterProperties: AdapterProperties,
    publisher: BehandlingEventPublisher,
) : EventSubscriber<BehandlingResource, BehandlingEventPublisher>(webClient, adapterProperties, publisher, "behandling") {

    private val log = logger<BehandlingEventSubscriber>()

    override fun responsePostingEvent(response: ResponseEntity<Void>, responseFintEvent: ResponseFintEvent) {
        log.info("Posting response for event {} returned {}.", responseFintEvent.corrId, response.statusCode.value())
    }
}
