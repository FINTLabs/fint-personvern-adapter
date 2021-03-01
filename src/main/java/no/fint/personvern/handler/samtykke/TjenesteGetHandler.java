package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.repository.WrapperDocumentRepository;
import no.fint.personvern.service.Handler;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class TjenesteGetHandler implements Handler {
    private final WrapperDocumentRepository repository;

    public TjenesteGetHandler(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        getTjenesteResources(event);
    }

    private void getTjenesteResources(Event<FintLinks> event) {
        repository
                .findByOrgIdAndType(event.getOrgId(), TjenesteResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(value -> new ObjectMapper().convertValue(value, TjenesteResource.class))
                .forEach(event::addData);

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.GET_ALL_TJENESTE.name());
    }
}
