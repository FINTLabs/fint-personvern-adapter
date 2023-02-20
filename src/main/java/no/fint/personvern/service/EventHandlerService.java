package no.fint.personvern.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fint.adapter.event.EventResponseService;
import no.fint.adapter.event.EventStatusService;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.event.model.Status;
import no.fint.event.model.health.Health;
import no.fint.event.model.health.HealthStatus;
import no.fint.model.resource.FintLinks;
import no.fint.personvern.SupportedActions;
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@AllArgsConstructor
@Slf4j
@Service
public class EventHandlerService {

    private EventResponseService eventResponseService;
    private EventStatusService eventStatusService;
    private SupportedActions supportedActions;
    private Collection<Handler> handlers;
    private TjenesteRepository tjenesteRepository;
    private Map<String, Handler> actionsHandlerMap;
    private Executor executor;

    public void handleEvent(String component, Event event) {
        if (event.isHealthCheck()) {
            postHealthCheckResponse(component, event);
        } else {
            if (eventStatusService.verifyEvent(component, event)) {
                executor.execute(() -> handleResponse(component, event.getAction(), new Event<>(event)));
            }
        }
    }

    private void handleResponse(String component, String action, Event<FintLinks> response) {
        try {
            actionsHandlerMap.getOrDefault(action, e -> {
                log.warn("No handler found for {}", action);
                e.setStatus(Status.ADAPTER_REJECTED);
                e.setResponseStatus(ResponseStatus.REJECTED);
                e.setMessage("Unsupported action: " + action);
            }).accept(response);
        } catch (Exception e) {
            response.setResponseStatus(ResponseStatus.ERROR);
            response.setMessage(ExceptionUtils.getStackTrace(e));
        } finally {
            if (response.getData() != null) {
                log.info("{}: Response for {}: {}, {} items", component, response.getAction(), response.getResponseStatus(), response.getData().size());
                log.trace("Event data: {}", response.getData());
            } else {
                log.info("{}: Response for {}: {}", component, response.getAction(), response.getResponseStatus());
            }
            eventResponseService.postResponse(component, response);
        }
    }

    public void postHealthCheckResponse(String component, Event event) {
        Event<Health> healthCheckEvent = new Event<>(event);
        healthCheckEvent.setStatus(Status.TEMP_UPSTREAM_QUEUE);

        if (healthCheck()) {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_HEALTHY));
        } else {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_UNHEALTHY));
            healthCheckEvent.setMessage("The adapter is unable to communicate with the database.");
        }

        eventResponseService.postResponse(component, healthCheckEvent);
    }

    private boolean healthCheck() {
        try {
            tjenesteRepository.count();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /*
    private boolean healthCheck() {
        return handlers.stream().allMatch(Handler::health);
    }
     */

    @PostConstruct
    void init() {
        executor = Executors.newSingleThreadExecutor(); // TODO Can we use more threads?

        actionsHandlerMap = new HashMap<>();
        handlers.forEach(h -> h.actions().forEach(a -> {
            actionsHandlerMap.put(a, h);
            supportedActions.add(a);
        }));

        log.info("Registered {} handlers, supporting actions: {}", handlers.size(), supportedActions.getActions());
    }
}