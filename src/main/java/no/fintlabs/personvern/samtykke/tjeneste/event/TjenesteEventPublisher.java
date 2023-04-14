package no.fintlabs.personvern.samtykke.tjeneste.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.events.EventPublisher;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.adapter.models.SyncPageEntry;
import no.fintlabs.personvern.samtykke.tjeneste.TjenesteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class TjenesteEventPublisher extends EventPublisher<TjenesteResource> {

    private final TjenesteResourceValidator validator;

    public TjenesteEventPublisher(AdapterProperties adapterProperties, TjenesteRepository repository, WebClient webClient, ObjectMapper objectMapper, TjenesteResourceValidator validator) {
        super("tjeneste", TjenesteResource.class, webClient, adapterProperties, repository, objectMapper);
        this.validator = validator;
    }

    @Override
    @Scheduled(initialDelayString = "9000", fixedDelayString = "60000")
    public void doCheckForNewEvents() {
        checkForNewEvents();
    }

    @Override
    protected void handleEvent(RequestFintEvent requestFintEvent, TjenesteResource tjenesteResource) {
        ResponseFintEvent<TjenesteResource> response = createResponse(requestFintEvent);

        if (resourceNotValid(tjenesteResource, response)) return;

        try {
            TjenesteResource updatedResource = repository.saveResources(tjenesteResource, requestFintEvent);
            response.setValue(createSyncPageEntry(updatedResource));
        } catch (Exception exception) {
            response.setFailed(true);
            response.setErrorMessage(exception.getMessage());
            log.error("Error in repository.saveResource", exception);
        }

        submit(response);
    }

    protected SyncPageEntry<TjenesteResource> createSyncPageEntry(TjenesteResource resource) {
        String identificationValue = resource.getSystemId().getIdentifikatorverdi();
        return SyncPageEntry.of(identificationValue, resource);
    }

    private boolean resourceNotValid(TjenesteResource tjenesteResource, ResponseFintEvent<TjenesteResource> response) {

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(tjenesteResource, "tjenesteResource");
        validator.validate(tjenesteResource, bindingResult);

        if (bindingResult.hasErrors()) {
            response.setRejected(true);
            response.setRejectReason(bindingResult.toString());
            submit(response);
        }

        return bindingResult.hasErrors();
    }
}
