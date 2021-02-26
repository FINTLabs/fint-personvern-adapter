package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.service.Handler;
import no.fint.personvern.service.ValidationService;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.repository.WrapperDocumentRepository;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class SamtykkeUpdateHandler implements Handler {
    private final WrapperDocumentRepository repository;
    private final ValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SamtykkeUpdateHandler(WrapperDocumentRepository repository, ValidationService validationService) {
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

        SamtykkeResource samtykkeResource = objectMapper.convertValue(event.getData().get(0), SamtykkeResource.class);

        List<Problem> problems = validationService.getProblems(samtykkeResource);

        if (problems.isEmpty()) {
            switch (event.getOperation()) {
                case CREATE:
                    createSamtykkeResource(event, samtykkeResource);
                    return;
                case UPDATE:
                    updateSamtykkeResource(event, samtykkeResource);
                    return;
                default:
                    throw new IllegalArgumentException("Invalid operation: " + event.getOperation());
            }
        }

        event.setProblems(problems);
        event.setResponseStatus(ResponseStatus.REJECTED);
        event.setMessage("Payload failed validation");
    }

    private void createSamtykkeResource(Event<FintLinks> event, SamtykkeResource samtykkeResource) {
        samtykkeResource.setSystemId(FintUtilities.createUuidSystemId());
        samtykkeResource.setOpprettet(new Date());

        WrapperDocument wrapperDocument = WrapperDocument.builder()
                .id(samtykkeResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(objectMapper.convertValue(samtykkeResource, Object.class))
                .type(SamtykkeResource.class.getCanonicalName())
                .build();

        repository.insert(wrapperDocument);

        event.setData(Collections.singletonList(samtykkeResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateSamtykkeResource(Event<FintLinks> event, SamtykkeResource samtykkeResource) {
        String id = event.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, event.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        SamtykkeResource value = objectMapper.convertValue(wrapperDocument.getValue(), SamtykkeResource.class);

        if (hasInvalidUpdates(samtykkeResource, value)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload contains updates to non-writeable attributes");
            return;
        }

        if (hasValidUpdates(samtykkeResource, value)) {
            value.setGyldighetsperiode(samtykkeResource.getGyldighetsperiode());

            wrapperDocument.setValue(objectMapper.convertValue(samtykkeResource, Object.class));

            repository.save(wrapperDocument);
        }

        event.setData(Collections.singletonList(value));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private boolean hasInvalidUpdates(SamtykkeResource samtykkeResource, SamtykkeResource value) {
        return !(Objects.equals(samtykkeResource.getSystemId(), value.getSystemId()) &&
                samtykkeResource.getOpprettet().toInstant().equals(value.getOpprettet().toInstant().truncatedTo(ChronoUnit.SECONDS)));
    }

    private boolean hasValidUpdates(SamtykkeResource samtykkeResource, SamtykkeResource value) {
        return !Objects.equals(samtykkeResource.getGyldighetsperiode(), value.getGyldighetsperiode());
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_SAMTYKKE.name());
    }
}
