package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
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

        Operation operation = event.getOperation();

        BehandlingResource behandlingResource = objectMapper.convertValue(event.getData().get(0), BehandlingResource.class);

        if (operation == Operation.CREATE) {
            createBehandlingResource(event, behandlingResource);
        } else if (operation == Operation.UPDATE){
            updateBehandlingResource(event, behandlingResource);
        } else {
            throw new IllegalArgumentException("Invalid operation: " + operation);
        }
    }

    private void createBehandlingResource(Event<FintLinks> event, BehandlingResource behandlingResource) {
        behandlingResource.setSystemId(FintUtilities.createUuidSystemId());

        repository.insert(WrapperDocument.builder()
                .id(behandlingResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(behandlingResource)
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

        behandlingResource.setSystemId(((BehandlingResource) wrapperDocument.getValue()).getSystemId());

        wrapperDocument.setValue(behandlingResource);

        repository.save(wrapperDocument);

        event.setData(Collections.singletonList(behandlingResource));

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_BEHANDLING.name());
    }
}
