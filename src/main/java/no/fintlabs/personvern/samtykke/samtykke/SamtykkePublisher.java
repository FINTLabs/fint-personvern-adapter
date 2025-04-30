package no.fintlabs.personvern.samtykke.samtykke;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.ResourcePublisher;
import no.fintlabs.adapter.datasync.ResourceRepository;
import no.fintlabs.adapter.datasync.SyncData;
import no.fintlabs.adapter.models.AdapterCapability;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class SamtykkePublisher extends ResourcePublisher<SamtykkeResource, ResourceRepository<SamtykkeResource>> {

    public SamtykkePublisher(SamtykkeRepository repository, AdapterProperties adapterProperties) {
        super(repository, adapterProperties);
    }

    @Override
    @Scheduled(initialDelayString = "10000", fixedRateString = "500000")
    @PostConstruct
    public void doFullSync() {
        log.info("Start full sync for resource {}", getCapability().getEntityUri());
        submit(SyncData.ofPostData(repository.getResources()));
    }

    // Not in use
    @Override
    public void doDeltaSync() {
        log.info("Start delta sync for resource {}", getCapability().getEntityUri());
        submit(SyncData.ofPatchData(repository.getUpdatedResources()));
    }

    @Override
    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilityByResource("samtykke");
    }
}
