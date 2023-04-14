package no.fintlabs.personvern.samtykke.samtykke.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.events.EventPublisher;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.adapter.models.SyncPageEntry;
import no.fintlabs.personvern.samtykke.samtykke.SamtykkeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class SamtykkeEventPublisher extends EventPublisher<SamtykkeResource> {

    public SamtykkeEventPublisher(AdapterProperties adapterProperties, SamtykkeRepository repository, WebClient webClient, ObjectMapper objectMapper) {
        super("samtykke", SamtykkeResource.class, webClient, adapterProperties, repository, objectMapper);
    }

    @Override
    @Scheduled(initialDelayString = "9000", fixedDelayString = "60000")
    public void doCheckForNewEvents() {
        checkForNewEvents();
    }

    @Override
    protected void handleEvent(RequestFintEvent requestFintEvent, SamtykkeResource samtykkeResource) {
        ResponseFintEvent<SamtykkeResource> response = createResponse(requestFintEvent);

        try {
            SamtykkeResource updatedResource = repository.saveResources(samtykkeResource, requestFintEvent);
            response.setValue(createSyncPageEntry(updatedResource));
        } catch (Exception exception) {
            response.setFailed(true);
            response.setErrorMessage(exception.getMessage());
            log.error("Error in repository.saveResource", exception);
        }

        submit(response);
    }

    protected SyncPageEntry<SamtykkeResource> createSyncPageEntry(SamtykkeResource resource) {
        String identificationValue = resource.getSystemId().getIdentifikatorverdi();
        return SyncPageEntry.of(identificationValue, resource);
    }
}
