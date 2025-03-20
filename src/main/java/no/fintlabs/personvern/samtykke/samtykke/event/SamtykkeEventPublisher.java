package no.fintlabs.personvern.samtykke.samtykke.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.events.EventPublisher;
import no.fintlabs.adapter.models.OperationType;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.adapter.models.SyncPageEntry;
import no.fintlabs.personvern.samtykke.samtykke.SamtykkeRepository;
import no.fintlabs.ResourceVerifierService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@Slf4j
@Service
public class SamtykkeEventPublisher extends EventPublisher<SamtykkeResource> {

    private final ResourceVerifierService verifier;

    public SamtykkeEventPublisher(AdapterProperties adapterProperties, SamtykkeRepository repository, WebClient webClient, ObjectMapper objectMapper, ResourceVerifierService resourceVerifyerService) {
        super("samtykke", SamtykkeResource.class, webClient, adapterProperties, repository, objectMapper);
        this.verifier = resourceVerifyerService;
    }

    @Override
    @Scheduled(initialDelayString = "9000", fixedDelayString = "1000")
    public void doCheckForNewEvents() {
        checkForNewEvents();
    }

    @Override
    protected void handleEvent(RequestFintEvent requestFintEvent, SamtykkeResource samtykkeResource) {
        ResponseFintEvent<SamtykkeResource> response = createResponse(requestFintEvent);
        if (verifier.verifySamtykkeResource(samtykkeResource)) {
            try {
                if (requestFintEvent.getOperationType() == OperationType.CREATE) {
                    if (samtykkeResource.getOpprettet() == null) samtykkeResource.setOpprettet(new Date());
                }
                SamtykkeResource updatedResource = repository.saveResources(samtykkeResource, requestFintEvent);
                response.setValue(createSyncPageEntry(updatedResource));
            } catch (Exception exception) {
                response.setFailed(true);
                response.setErrorMessage(exception.getMessage());
                log.error("Error in repository.saveResource", exception);
            }
        } else {
            response.setRejected(true);
            response.setRejectReason("Fields in samtykkeResource cannot be null or empty");
            log.warn("SamtykkeResource not verified");
        }

        submit(response);
    }

    protected SyncPageEntry<SamtykkeResource> createSyncPageEntry(SamtykkeResource resource) {
        String identificationValue = resource.getSystemId().getIdentifikatorverdi();
        return SyncPageEntry.of(identificationValue, resource);
    }
}
