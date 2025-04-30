package no.fintlabs.personvern.samtykke.behandling.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.events.EventSubscriber;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class BehandlingEventSubscriber extends EventSubscriber<BehandlingResource, BehandlingEventPublisher> {

    protected BehandlingEventSubscriber(WebClient webClient, AdapterProperties adapterProperties, BehandlingEventPublisher publisher) {
        super(webClient, adapterProperties, publisher, "behandling");
    }

    @Override
    protected void responsePostingEvent(ResponseEntity<Void> response, ResponseFintEvent responseFintEvent) {
        log.info("Posting response for event {} returned {}.", responseFintEvent.getCorrId(), response.getStatusCode());
    }

}
