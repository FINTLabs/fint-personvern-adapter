package no.fint.personvern.handler.samtykke.tjeneste;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fint.personvern.exception.IllegalOrganizationException;
import no.fint.personvern.exception.RowNotFoundException;
import no.fint.personvern.service.Handler;
import no.fint.personvern.service.ValidationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class TjenesteUpdateHandler implements Handler {
    private final TjenesteRepository repository;
    private final ValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TjenesteUpdateHandler(TjenesteRepository repository, ValidationService validationService) {
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
            case CREATE -> createTjenesteResource(event, tjenesteResource);
            case UPDATE -> updateTjenesteResource(event, tjenesteResource);
            default -> throw new IllegalArgumentException("Invalid operation: " + event.getOperation());
        }
    }

    private void createTjenesteResource(Event<FintLinks> event, TjenesteResource tjenesteResource) {
        //tjenesteResource.setSystemId(FintUtilities.createUuidSystemId());

        if (repository.existsById(tjenesteResource.getSystemId().getIdentifikatorverdi())) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Entry with same id already exists");
            return;
        }

        TjenesteEntity tjenesteEntity = TjenesteEntity.builder()
                .id(tjenesteResource.getSystemId().getIdentifikatorverdi())
                .orgId(event.getOrgId())
                .resource(tjenesteResource)
                .lastModifiedDate(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        repository.save(tjenesteEntity);

        event.setData(Collections.singletonList(tjenesteResource));
        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void updateTjenesteResource(Event<FintLinks> event, TjenesteResource updatedTjenesteResource) {
        String id = event.getQuery().split("/")[1];

        TjenesteEntity tjenesteEntity = repository.findById(id).orElseThrow(RowNotFoundException::new);
        if (!tjenesteEntity.getOrgId().equals(event.getOrgId())) throw new IllegalOrganizationException();

        TjenesteResource exsistingTjenesteResource = tjenesteEntity.getResource();

        if (hasInvalidUpdates(updatedTjenesteResource, exsistingTjenesteResource)) {
            event.setResponseStatus(ResponseStatus.REJECTED);
            event.setMessage("Payload contains updates to non-writeable attributes");
            return;
        }

        exsistingTjenesteResource.setNavn(updatedTjenesteResource.getNavn());

        tjenesteEntity.setResource(exsistingTjenesteResource);
        tjenesteEntity.setLastModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(tjenesteEntity);

        event.setData(Collections.singletonList(exsistingTjenesteResource));
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