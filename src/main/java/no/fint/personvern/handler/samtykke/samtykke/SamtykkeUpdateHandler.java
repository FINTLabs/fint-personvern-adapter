package no.fint.personvern.handler.samtykke.samtykke;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.exception.IllegalOrganizationException;
import no.fint.personvern.exception.RowNotFoundException;
import no.fint.personvern.service.Handler;
import no.fint.personvern.service.ValidationService;
import no.fint.personvern.utility.FintUtilities;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class SamtykkeUpdateHandler implements Handler {
    private final SamtykkeRepository repository;
    private final ValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SamtykkeUpdateHandler(SamtykkeRepository repository, ValidationService validationService) {
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

        if (problems.size() > 0) {
            event.setProblems(problems);
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload failed validation");
            return;
        }

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

        SamtykkeEntity samtykkeEntity = SamtykkeEntity.builder()
                .id(samtykkeResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .value(samtykkeResource)
                .lastModifiedDate(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        repository.save(samtykkeEntity);

        event.setData(Collections.singletonList(samtykkeResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateSamtykkeResource(Event<FintLinks> event, SamtykkeResource updatedSamtykkeResource) {
        String id = event.getQuery().split("/")[1];

        SamtykkeEntity samtykkeEntity = repository.findById(id).orElseThrow(RowNotFoundException::new);
        if (!samtykkeEntity.getOrgId().equals(event.getOrgId())) throw new IllegalOrganizationException();

        SamtykkeResource exsistingSamtykkeResource = samtykkeEntity.getValue();

        if (hasInvalidUpdates(updatedSamtykkeResource, exsistingSamtykkeResource)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload contains updates to non-writeable attributes");
            return;
        }

        exsistingSamtykkeResource.setGyldighetsperiode(updatedSamtykkeResource.getGyldighetsperiode());

        samtykkeEntity.setValue(exsistingSamtykkeResource);
        samtykkeEntity.setLastModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(samtykkeEntity);

        event.setData(Collections.singletonList(exsistingSamtykkeResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private boolean hasInvalidUpdates(SamtykkeResource samtykkeResource, SamtykkeResource value) {
        return !(samtykkeResource.getSystemId().getIdentifikatorverdi().equals(value.getSystemId().getIdentifikatorverdi()) &&
                getInstant(samtykkeResource.getOpprettet()).equals(getInstant(value.getOpprettet())));
    }

    public Instant getInstant(Date date) {
        return date.toInstant().truncatedTo(ChronoUnit.SECONDS);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamtykkeActions.UPDATE_SAMTYKKE.name());
    }
}
