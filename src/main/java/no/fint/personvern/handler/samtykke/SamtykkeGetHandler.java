package no.fint.personvern.handler.samtykke;

import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.service.Handler;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.repository.WrapperDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class SamtykkeGetHandler implements Handler {
    private final WrapperDocumentRepository repository;

    public SamtykkeGetHandler(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        getSamtykkeResources(event);
    }

    private void getSamtykkeResources(Event<FintLinks> event) {
        repository
                .findByOrgIdAndType(event.getOrgId(), SamtykkeResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(SamtykkeResource.class::cast)
                .forEach(event::addData);

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.GET_ALL_SAMTYKKE.name());
    }
}
