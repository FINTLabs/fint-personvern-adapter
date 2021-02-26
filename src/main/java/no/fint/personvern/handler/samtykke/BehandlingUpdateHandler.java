package no.fint.personvern.handler.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.service.ValidationService;
import no.fint.personvern.repository.WrapperDocument;
import no.fint.personvern.service.Handler;
import no.fint.personvern.repository.WrapperDocumentRepository;
import no.fint.personvern.utility.FintUtilities;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class BehandlingUpdateHandler implements Handler {
    private final WrapperDocumentRepository repository;
    private final ValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BehandlingUpdateHandler(WrapperDocumentRepository repository, ValidationService validationService) {
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

        BehandlingResource behandlingResource = objectMapper.convertValue(event.getData().get(0), BehandlingResource.class);

        List<Problem> problems = validationService.getProblems(behandlingResource);

        if (problems.isEmpty()) {
            switch (event.getOperation()) {
                case CREATE:
                    createBehandlingResource(event, behandlingResource);
                    return;
                case UPDATE:
                    updateBehandlingResource(event, behandlingResource);
                    return;
                default:
                    throw new IllegalArgumentException("Invalid operation: " + event.getOperation());
            }
        }

        event.setProblems(problems);
        event.setResponseStatus(ResponseStatus.REJECTED);
        event.setMessage("Payload failed validation");
    }

    private void createBehandlingResource(Event<FintLinks> event, BehandlingResource behandlingResource) {
        behandlingResource.setSystemId(FintUtilities.createUuidSystemId());

        WrapperDocument wrapperDocument = WrapperDocument.builder()
                .id(behandlingResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(objectMapper.convertValue(behandlingResource, Object.class))
                .type(BehandlingResource.class.getCanonicalName())
                .build();

        repository.insert(wrapperDocument);

        event.setData(Collections.singletonList(behandlingResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateBehandlingResource(Event<FintLinks> event, BehandlingResource behandlingResource) {
        String id = event.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, event.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        BehandlingResource value = objectMapper.convertValue(wrapperDocument.getValue(), BehandlingResource.class);

        if (hasInvalidUpdates(behandlingResource, value)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload contains updates to non-writeable attributes");
            return;
        }

        if (hasValidUpdates(behandlingResource, value)) {
            value.setAktiv(behandlingResource.getAktiv());

            wrapperDocument.setValue(objectMapper.convertValue(value, Object.class));

            repository.save(wrapperDocument);
        }

        event.setData(Collections.singletonList(value));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private boolean hasInvalidUpdates(BehandlingResource behandlingResource, BehandlingResource value) {
        return !(Objects.equals(behandlingResource.getSystemId(), value.getSystemId()) && behandlingResource.getFormal().equals(value.getFormal()));
    }

    private boolean hasValidUpdates(BehandlingResource behandlingResource, BehandlingResource value) {
        return !behandlingResource.getAktiv().equals(value.getAktiv());
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_BEHANDLING.name());
    }
}