package no.fintlabs.personvern.samtykke.behandling.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.events.EventPublisher;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.adapter.models.SyncPageEntry;
import no.fintlabs.personvern.samtykke.behandling.BehandlingRepository;
import no.fintlabs.ResourceVerifierService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class BehandlingEventPublisher extends EventPublisher<BehandlingResource> {

    private final ResourceVerifierService resourceVerifyerService;

    public BehandlingEventPublisher(AdapterProperties adapterProperties, BehandlingRepository repository, WebClient webClient, ObjectMapper objectMapper, ResourceVerifierService resourceVerifyerService) {
        super("behandling", BehandlingResource.class, webClient, adapterProperties, repository, objectMapper);
        this.resourceVerifyerService = resourceVerifyerService;
    }

    @Override
    @Scheduled(initialDelayString = "9000", fixedDelayString = "1000")
    public void doCheckForNewEvents() {
        checkForNewEvents();
    }

    @Override
    protected void handleEvent(RequestFintEvent requestFintEvent, BehandlingResource behandlingResource) {
        ResponseFintEvent<BehandlingResource> response = createResponse(requestFintEvent);
        if (resourceVerifyerService.verifyBehandlingResource(behandlingResource)){
        try {
            BehandlingResource updatedResource = repository.saveResources(behandlingResource, requestFintEvent);
            response.setValue(createSyncPageEntry(updatedResource));
        } catch (Exception exception) {
            response.setFailed(true);
            response.setErrorMessage(exception.getMessage());
            log.error("Error in repository.saveResource", exception);
        }
        } else {
            response.setRejected(true);
            response.setRejectReason("Fields in behandlingResource cannot be null or empty");
            log.warn("BehandlingsResourve not veryfied");
        }

        submit(response);
    }

    protected SyncPageEntry<BehandlingResource> createSyncPageEntry(BehandlingResource resource) {
        String identificationValue = resource.getSystemId().getIdentifikatorverdi();
        return SyncPageEntry.of(identificationValue, resource);
    }
}
