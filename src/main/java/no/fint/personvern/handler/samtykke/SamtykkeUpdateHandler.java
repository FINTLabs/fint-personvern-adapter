package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.service.Handler;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.repository.WrapperDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.function.BiPredicate;

@Component
public class SamtykkeUpdateHandler implements Handler {
    private final WrapperDocumentRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SamtykkeUpdateHandler(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        if (event.getData().size() != 1) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Invalid request");
            return;
        }

        SamtykkeResource samtykkeResource = objectMapper.convertValue(event.getData().get(0), SamtykkeResource.class);

        switch (event.getOperation()) {
            case CREATE:
                createSamtykkeResource(event, samtykkeResource);
                break;
            case UPDATE:
                updateSamtykkeResource(event, samtykkeResource);
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + event.getOperation());
        }
    }

    private void createSamtykkeResource(Event<FintLinks> event, SamtykkeResource samtykkeResource) {
        samtykkeResource.setSystemId(FintUtilities.createUuidSystemId());
        samtykkeResource.setOpprettet(new Date());

        repository.insert(WrapperDocument.builder()
                .id(samtykkeResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(objectMapper.convertValue(samtykkeResource, Object.class))
                .type(SamtykkeResource.class.getCanonicalName())
                .build());

        event.setData(Collections.singletonList(samtykkeResource));

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateSamtykkeResource(Event<FintLinks> event, SamtykkeResource samtykkeResource) {
        String id = event.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, event.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        if (validate().negate().test(samtykkeResource, wrapperDocument)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Updates to non-writeable attributes not allowed");
            event.setData(Collections.emptyList());
            return;
        }

        wrapperDocument.setValue(objectMapper.convertValue(samtykkeResource, Object.class));

        repository.save(wrapperDocument);

        event.setData(Collections.singletonList(samtykkeResource));

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_SAMTYKKE.name());
    }

    private BiPredicate<SamtykkeResource, WrapperDocument> validate() {
        return (samtykkeResource, wrapperDocument) -> {
            SamtykkeResource value = objectMapper.convertValue(wrapperDocument.getValue(), SamtykkeResource.class);

            return samtykkeResource.getSystemId().equals(value.getSystemId()) &&
                    samtykkeResource.getOpprettet().equals(value.getOpprettet());
        };
    }
}
