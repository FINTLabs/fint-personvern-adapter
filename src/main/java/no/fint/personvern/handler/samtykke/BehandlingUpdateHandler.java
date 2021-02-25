package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.service.Handler;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.repository.WrapperDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;

@Slf4j
@Component
public class BehandlingUpdateHandler implements Handler {
    private final WrapperDocumentRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BehandlingUpdateHandler(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        if (event.getData().size() != 1) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Invalid request");
            return;
        }

        BehandlingResource behandlingResource = objectMapper.convertValue(event.getData().get(0), BehandlingResource.class);

        switch (event.getOperation()) {
            case CREATE:
                createBehandlingResource(event, behandlingResource);
                break;
            case UPDATE:
                updateBehandlingResource(event, behandlingResource);
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + event.getOperation());
        }
    }

    private void createBehandlingResource(Event<FintLinks> event, BehandlingResource behandlingResource) {
        behandlingResource.setSystemId(FintUtilities.createUuidSystemId());

        repository.insert(WrapperDocument.builder()
                .id(behandlingResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(objectMapper.convertValue(behandlingResource, Object.class))
                .type(BehandlingResource.class.getCanonicalName())
                .build());

        event.setData(Collections.singletonList(behandlingResource));

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateBehandlingResource(Event<FintLinks> event, BehandlingResource behandlingResource) {
        String id = event.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, event.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        if (validate.negate().test(behandlingResource, wrapperDocument)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Updates to non-writeable attributes not allowed");
            event.setData(Collections.emptyList());
            return;
        }

        wrapperDocument.setValue(objectMapper.convertValue(behandlingResource, Object.class));

        repository.save(wrapperDocument);

        event.setData(Collections.singletonList(behandlingResource));

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_BEHANDLING.name());
    }

    private final BiPredicate<BehandlingResource, WrapperDocument> validate = (behandlingResource, wrapperDocument) -> {
        BehandlingResource value = objectMapper.convertValue(wrapperDocument.getValue(), BehandlingResource.class);

        return behandlingResource.getSystemId().equals(value.getSystemId()) && behandlingResource.getFormal().equals(value.getFormal());
    };
}
