package no.fint.personvern.handler.samtykke.behandling;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.service.Handler;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class BehandlingGetHandler implements Handler {
    private final BehandlingRepository repository;

    public BehandlingGetHandler(BehandlingRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        getBehandlingResources(event);
    }

    private void getBehandlingResources(Event<FintLinks> event) {
        repository
                .findAll()
                .stream()
                .filter(behandling -> behandling.getOrgId().equals(event.getOrgId()))
                .map(behandling -> behandling.getValue())
                .forEach(event::addData);

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.GET_ALL_BEHANDLING.name());
    }
}