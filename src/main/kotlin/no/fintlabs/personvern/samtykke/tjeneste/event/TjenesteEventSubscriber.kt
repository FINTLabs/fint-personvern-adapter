package no.fintlabs.personvern.samtykke.tjeneste.event

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.events.EventSubscriber
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.logger
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class TjenesteEventSubscriber(
    webClient: WebClient,
    adapterProperties: AdapterProperties,
    publisher: TjenesteEventPublisher,
) : EventSubscriber<TjenesteResource, TjenesteEventPublisher>(webClient, adapterProperties, publisher, "tjeneste") {

    private val log = logger<TjenesteEventSubscriber>()

    override fun responsePostingEvent(response: ResponseEntity<Void>, responseFintEvent: ResponseFintEvent) {
        log.info("Posting response for event {} returned {}.", responseFintEvent.corrId, response.statusCode.value())
    }
}
