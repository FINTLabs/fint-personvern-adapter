package no.fintlabs.personvern.samtykke.tjeneste.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fintlabs.ResourceVerifierService;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.events.EventPublisher;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.adapter.models.sync.SyncPageEntry;
import no.fintlabs.personvern.samtykke.tjeneste.TjenesteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class TjenesteEventPublisher extends EventPublisher<TjenesteResource> {

    private final ResourceVerifierService verifierService;

    public TjenesteEventPublisher(AdapterProperties adapterProperties, TjenesteRepository repository, WebClient webClient, ObjectMapper objectMapper, ResourceVerifierService verifierService) {
        super("tjeneste", TjenesteResource.class, webClient, adapterProperties, repository, objectMapper);
        this.verifierService = verifierService;
    }

    @Override
    @Scheduled(initialDelayString = "9000", fixedDelayString = "1000")
    public void doCheckForNewEvents() {
        checkForNewEvents();
    }

    @Override
    protected void handleEvent(RequestFintEvent requestFintEvent, TjenesteResource tjenesteResource) {
        ResponseFintEvent response = createResponse(requestFintEvent);
        if (verifierService.verifyTjenesteResource(tjenesteResource)) {
            try {
                TjenesteResource updatedResource = repository.saveResources(tjenesteResource, requestFintEvent);
                response.setValue(createSyncPageEntry(updatedResource));
            } catch (Exception exception) {
                response.setFailed(true);
                response.setErrorMessage(exception.getMessage());
                log.error("Error in repository.saveResource", exception);
            }
        } else {
            response.setRejected(true);
            response.setRejectReason("Fields in tjenesteResource cannot be null or empty");
            log.error("Fields in tjenesteResource cannot be null or empty");
        }

        submit(response);
    }

    protected SyncPageEntry createSyncPageEntry(TjenesteResource resource) {
        String identificationValue = resource.getSystemId().getIdentifikatorverdi();
        return SyncPageEntry.of(identificationValue, resource);
    }
}
