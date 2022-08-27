package no.fint.personvern.handler.samtykke.tjeneste;

import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.personvern.service.Handler;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class TjenesteGetHandler implements Handler {
    private final TjenesteRepository repository;

    public TjenesteGetHandler(TjenesteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        getTjenesteResources(event);
    }

    private void getTjenesteResources(Event<FintLinks> event) {
        repository
                .findAll()
                .stream()
                .filter(tjeneste -> tjeneste.getOrgId().equals(event.getOrgId()))
                .map(tjeneste -> tjeneste.getResource())
                .forEach(event::addData);

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.GET_ALL_TJENESTE.name());
    }
}
