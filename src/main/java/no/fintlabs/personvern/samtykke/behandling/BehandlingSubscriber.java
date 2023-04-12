package no.fintlabs.personvern.samtykke.behandling;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.ResourceSubscriber;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.SyncPageEntry;
import no.fintlabs.adapter.validator.ValidatorService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class BehandlingSubscriber extends ResourceSubscriber<BehandlingResource, BehandlingPublisher> {

    protected BehandlingSubscriber(WebClient webClient, AdapterProperties props, BehandlingPublisher publisher, ValidatorService validatorService) {
        super(webClient, props, publisher, validatorService);
    }

    @Override
    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilities().get("behandling");
    }

    @Override
    protected SyncPageEntry<BehandlingResource> createSyncPageEntry(BehandlingResource resource) {
        String identificationValue = resource.getSystemId().getIdentifikatorverdi();
        return SyncPageEntry.of(identificationValue, resource);
    }
}
