package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.repository.WrapperDocumentRepository;
import no.fint.personvern.service.Handler;
import no.fint.personvern.service.ValidationService;
import no.fint.personvern.utility.FintUtilities;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class TjenesteUpdateHandler implements Handler {
    private final WrapperDocumentRepository repository;
    private final ValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TjenesteUpdateHandler(WrapperDocumentRepository repository, ValidationService validationService) {
        this.repository = repository;
        this.validationService = validationService;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        if (event.getData().size() != 1) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload missing");
            return;
        }

        TjenesteResource tjenesteResource = objectMapper.convertValue(event.getData().get(0), TjenesteResource.class);

        List<Problem> problems = validationService.getProblems(tjenesteResource);

        if (problems.size() > 0) {
            event.setProblems(problems);
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload failed validation");
            return;
        }

        switch (event.getOperation()) {
            case CREATE:
                createTjenesteResource(event, tjenesteResource);
                break;
            case UPDATE:
                updateTjenesteResource(event, tjenesteResource);
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + event.getOperation());
        }
    }

    private void createTjenesteResource(Event<FintLinks> event, TjenesteResource tjenesteResource) {
        tjenesteResource.setSystemId(FintUtilities.createUuidSystemId());

        WrapperDocument wrapperDocument = WrapperDocument.builder()
                .id(tjenesteResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(objectMapper.convertValue(tjenesteResource, Object.class))
                .type(TjenesteResource.class.getCanonicalName())
                .build();

        repository.insert(wrapperDocument);

        event.setData(Collections.singletonList(tjenesteResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateTjenesteResource(Event<FintLinks> event, TjenesteResource tjenesteResource) {
        String id = event.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, event.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        TjenesteResource value = objectMapper.convertValue(wrapperDocument.getValue(), TjenesteResource.class);

        if (hasInvalidUpdates(tjenesteResource, value)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload contains updates to non-writeable attributes");
            return;
        }

        value.setNavn(tjenesteResource.getNavn());

        wrapperDocument.setValue(objectMapper.convertValue(tjenesteResource, Object.class));

        repository.save(wrapperDocument);

        event.setData(Collections.singletonList(value));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private boolean hasInvalidUpdates(TjenesteResource tjenesteResource, TjenesteResource value) {
        return !tjenesteResource.getSystemId().getIdentifikatorverdi().equals(value.getSystemId().getIdentifikatorverdi());
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_TJENESTE.name());
    }
}