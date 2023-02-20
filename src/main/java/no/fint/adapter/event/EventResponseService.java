package no.fint.adapter.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.adapter.FintAdapterEndpoints;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Handles responses back to the provider status endpoint.
 */
@Slf4j
@Service
public class EventResponseService {

    @Autowired
    private FintAdapterEndpoints endpoints;

    private final RestTemplate restTemplate;

    public EventResponseService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Method for posting back the response to the provider.
     *
     * @param component Name of component
     * @param event     Event to post back
     */
    public void postResponse(String component, Event event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HeaderConstants.ORG_ID, event.getOrgId());
            headers.add(HeaderConstants.CLIENT, event.getClient());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            String url = endpoints.getProviders().get(component) + endpoints.getResponse();
            log.info("{}: Posting response for {} ...", component, event.getAction());
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(event, headers), Void.class);
            log.info("{}: Provider POST response: {}", component, response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            log.warn("{}: Provider POST response error: {}", component, e.getStatusCode());
        }
    }
}