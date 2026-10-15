package no.fintlabs.personvern.samtykke.behandling

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.datasync.ResourceSubscriber
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.validator.ValidatorService
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BehandlingSubscriber(
    webClient: WebClient,
    props: AdapterProperties,
    publisher: BehandlingPublisher,
    validatorService: ValidatorService,
) : ResourceSubscriber<BehandlingResource, BehandlingPublisher>(webClient, props, publisher, validatorService) {

    override fun getCapability(): AdapterCapability = adapterProperties.getCapabilityByResource("behandling")

    override fun createSyncPageEntry(resource: BehandlingResource): SyncPageEntry =
        SyncPageEntry.of(resource.systemId.identifikatorverdi, resource)
}
