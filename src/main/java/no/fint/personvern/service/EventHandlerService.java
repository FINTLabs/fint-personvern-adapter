package no.fint.personvern.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.adapter.event.EventResponseService;
import no.fint.adapter.event.EventStatusService;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
import no.fint.event.model.ResponseStatus;
import no.fint.event.model.Status;
import no.fint.event.model.health.Health;
import no.fint.event.model.health.HealthStatus;
import no.fint.model.personvern.kodeverk.KodeverkActions;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventHandlerService {
    private final EventResponseService eventResponseService;
    private final EventStatusService eventStatusService;
    private final KodeverkService kodeverkService;
    private final SamtykkeService samtykkeService;
    private final BehandlingService behandlingService;
    private final TjenesteService tjenesteService;
    private final MongoService mongoService;

    public EventHandlerService(
            EventResponseService eventResponseService,
            EventStatusService eventStatusService,
            KodeverkService kodeverkService,
            SamtykkeService samtykkeService,
            BehandlingService behandlingService,
            TjenesteService tjenesteService, MongoService mongoService) {
        this.eventResponseService = eventResponseService;
        this.eventStatusService = eventStatusService;
        this.kodeverkService = kodeverkService;
        this.samtykkeService = samtykkeService;
        this.behandlingService = behandlingService;
        this.tjenesteService = tjenesteService;
        this.mongoService = mongoService;
    }

    public void handleEvent(String component, Event event) {
        if (event.isHealthCheck()) {
            postHealthCheckResponse(component, event);
        } else {
            if (eventStatusService.verifyEvent(component, event)) {
                Event<FintLinks> responseEvent = new Event<>(event);
                try {
                    if (KodeverkActions.getActions().contains(event.getAction())) {
                        switch (KodeverkActions.valueOf(event.getAction())) {
                            case GET_ALL_BEHANDLINGSGRUNNLAG:
                                kodeverkService.getAllBehandlingsgrunnlag(responseEvent);
                                break;
                            case GET_ALL_PERSONOPPLYSNING:
                                kodeverkService.getAllPersonopplysning(responseEvent);
                                break;
                        }
                    } else if (SamtykkeActions.getActions().contains(event.getAction())) {
                        switch (SamtykkeActions.valueOf(event.getAction())) {
                            case GET_ALL_SAMTYKKE:
                                samtykkeService.getAllSamtykke(responseEvent);
                                break;
                            case UPDATE_SAMTYKKE:
                                if (responseEvent.getOperation() == Operation.CREATE) {
                                    samtykkeService.createSamtykke(responseEvent);
                                } else if (responseEvent.getOperation() == Operation.UPDATE) {
                                    samtykkeService.updateSamtykke(responseEvent);
                                } else {
                                    throw new IllegalArgumentException("Invalid operation: " + responseEvent.getOperation());
                                }
                                break;
                            case GET_ALL_BEHANDLING:
                                behandlingService.getAllBehandling(responseEvent);
                                break;
                            case UPDATE_BEHANDLING:
                                if (responseEvent.getOperation() == Operation.CREATE) {
                                    behandlingService.createBehandling(responseEvent);
                                } else if (responseEvent.getOperation() == Operation.UPDATE) {
                                    behandlingService.updateBehandling(responseEvent);
                                } else {
                                    throw new IllegalArgumentException("Invalid operation: " + responseEvent.getOperation());
                                }
                                break;
                            case GET_ALL_TJENESTE:
                                tjenesteService.getAllTjeneste(responseEvent);
                                break;
                            case UPDATE_TJENESTE:
                                if (responseEvent.getOperation() == Operation.CREATE) {
                                    tjenesteService.createTjeneste(responseEvent);
                                } else if (responseEvent.getOperation() == Operation.UPDATE) {
                                    tjenesteService.updateTjeneste(responseEvent);
                                } else {
                                    throw new IllegalArgumentException("Invalid operation: " + responseEvent.getOperation());
                                }
                                break;
                        }
                    }
                    responseEvent.setResponseStatus(ResponseStatus.ACCEPTED);
                } catch (MongoEntryExistsException e) {
                    log.error("Error handling event MongoEntryExistsException {}{}", event, e);
                    responseEvent.setResponseStatus(ResponseStatus.CONFLICT);
                    responseEvent.setMessage(e.getMessage());
                } catch (MongoCantFindDocumentException e) {
                    log.error("Error handling event MongoCantFindDocumentException {}", event, e);
                    responseEvent.setResponseStatus(ResponseStatus.REJECTED);
                    responseEvent.setStatusCode("NOT_FOUND");
                    responseEvent.setMessage(e.getMessage());
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

    private boolean healthCheck() {
        return mongoService.ping();
    }
}
