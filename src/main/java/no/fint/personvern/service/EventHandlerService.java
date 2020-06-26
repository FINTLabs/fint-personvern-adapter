package no.fint.personvern.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.adapter.event.EventResponseService;
import no.fint.adapter.event.EventStatusService;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.event.model.Status;
import no.fint.event.model.health.Health;
import no.fint.event.model.health.HealthStatus;
import no.fint.model.personvern.kodeverk.KodeverkActions;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import org.springframework.stereotype.Service;

/**
 * The EventHandlerService receives the <code>event</code> from SSE endpoint (provider) in the {@link #handleEvent(String, Event)} method.
 */
@Slf4j
@Service
public class EventHandlerService {
    private final EventResponseService eventResponseService;
    private final EventStatusService eventStatusService;
    private final KodeverkService kodeverkService;
    private final SamtykkeService samtykkeService;

    public EventHandlerService(EventResponseService eventResponseService, EventStatusService eventStatusService, KodeverkService kodeverkService, SamtykkeService samtykkeService) {
        this.eventResponseService = eventResponseService;
        this.eventStatusService = eventStatusService;
        this.kodeverkService = kodeverkService;
        this.samtykkeService = samtykkeService;
    }

    public void handleEvent(String component, Event event) {
        if (event.isHealthCheck()) {
            postHealthCheckResponse(component, event);
        } else {
            if (eventStatusService.verifyEvent(component, event)) {
                Event<FintLinks> responseEvent = new Event<>(event);
                try {
                    if (KodeverkActions.getActions().contains(event.getAction())) {
                        if (KodeverkActions.valueOf(event.getAction()) == KodeverkActions.GET_ALL_BEHANDLINGSGRUNNLAG) {
                            kodeverkService.getAllBehandlingsgrunnlag(responseEvent);
                        }
                    } else if (SamtykkeActions.getActions().contains(event.getAction())) {
                        switch (SamtykkeActions.valueOf(event.getAction())) {
                            case GET_ALL_SAMTYKKE:
                                samtykkeService.getAllSamtykke(responseEvent);
                                break;
                            case UPDATE_SAMTYKKE:
                                samtykkeService.updateSamtykke(responseEvent);
                                break;
                            case GET_ALL_BEHANDLING:
                                samtykkeService.getAllBehandling(responseEvent);
                                break;
                            case UPDATE_BEHANDLING:
                                samtykkeService.updateBehandling(responseEvent);
                                break;
                            case GET_ALL_TJENESTE:
                                samtykkeService.getAllTjeneste(responseEvent);
                                break;
                            case UPDATE_TJENESTE:
                                samtykkeService.updateTjeneste(responseEvent);
                                break;
                        }
                    }
                    responseEvent.setResponseStatus(ResponseStatus.ACCEPTED);
                } catch (Exception e) {
                    log.error("Error handling event {}", event, e);
                    responseEvent.setResponseStatus(ResponseStatus.ERROR);
                    responseEvent.setMessage(e.getMessage());
                } finally {
                    log.info("{}: Response for {}: {}, {} items", component, responseEvent.getAction(), responseEvent.getResponseStatus(), responseEvent.getData().size());
                    responseEvent.setStatus(Status.ADAPTER_RESPONSE);
                    eventResponseService.postResponse(component, responseEvent);
                }
            }
        }
    }

    /**
     * Checks if the application is healthy and updates the event object.
     *
     * @param event The event object
     */
    public void postHealthCheckResponse(String component, Event event) {
        Event<Health> healthCheckEvent = new Event<>(event);
        healthCheckEvent.setStatus(Status.TEMP_UPSTREAM_QUEUE);

        if (healthCheck()) {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_HEALTHY));
        } else {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_UNHEALTHY));
            healthCheckEvent.setMessage("The adapter is unable to communicate with the application.");
        }

        eventResponseService.postResponse(component, healthCheckEvent);
    }

    /**
     * TODO
     * This is where we implement the health check code
     *
     * @return {@code true} if health is ok, else {@code false}
     */
    private boolean healthCheck() {
        /*
         * Check application connectivity etc.
         */
        return true;
    }
}
