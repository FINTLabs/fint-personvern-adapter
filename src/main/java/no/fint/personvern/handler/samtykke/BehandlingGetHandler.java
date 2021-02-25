package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.service.Handler;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.repository.WrapperDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class BehandlingGetHandler implements Handler {
    private final WrapperDocumentRepository repository;

    public BehandlingGetHandler(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        getBehandlingResources(event);
    }

    private void getBehandlingResources(Event<FintLinks> event) {
        repository
                .findByOrgIdAndType(event.getOrgId(), BehandlingResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(value -> new ObjectMapper().convertValue(value, BehandlingResource.class))
                .forEach(event::addData);

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.GET_ALL_BEHANDLING.name());
    }
}