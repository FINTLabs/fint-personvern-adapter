package no.fint.personvern.handler.samtykke.behandling;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.exception.IllegalOrganizationException;
import no.fint.personvern.exception.RowNotFoundException;
import no.fint.personvern.service.ValidationService;
import no.fint.personvern.service.Handler;
import no.fint.personvern.utility.FintUtilities;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Component
public class BehandlingUpdateHandler implements Handler {
    private final BehandlingRepository repository;
    private final ValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BehandlingUpdateHandler(BehandlingRepository repository, ValidationService validationService) {
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

        if (problems.size() > 0) {
            event.setProblems(problems);
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload failed validation");
            return;
        }

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

        BehandlingEntity behandlingEntity = BehandlingEntity.builder()
                .id(behandlingResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(behandlingResource)
                .lastModifiedDate(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        repository.save(behandlingEntity);

        event.setData(Collections.singletonList(behandlingResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateBehandlingResource(Event<FintLinks> event, BehandlingResource updatedBehandlingResource) {
        String id = event.getQuery().split("/")[1];

        BehandlingEntity behandlingEntity = repository.findById(id).orElseThrow(RowNotFoundException::new);//gId(id, event.getOrgId());
        if (!behandlingEntity.getOrgId().equals(event.getOrgId())) throw new IllegalOrganizationException();

        BehandlingResource existingBehandlingResource = behandlingEntity.getValue();

        if (hasInvalidUpdates(updatedBehandlingResource, existingBehandlingResource)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload contains updates to non-writeable attributes");
            return;
        }

        existingBehandlingResource.setAktiv(updatedBehandlingResource.getAktiv());

        behandlingEntity.setValue(existingBehandlingResource);
        behandlingEntity.setLastModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(behandlingEntity);

        event.setData(Collections.singletonList(existingBehandlingResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private boolean hasInvalidUpdates(BehandlingResource behandlingResource, BehandlingResource value) {
        return !(behandlingResource.getSystemId().getIdentifikatorverdi().equals(value.getSystemId().getIdentifikatorverdi()) &&
                behandlingResource.getFormal().equals(value.getFormal()));
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_BEHANDLING.name());
    }
}