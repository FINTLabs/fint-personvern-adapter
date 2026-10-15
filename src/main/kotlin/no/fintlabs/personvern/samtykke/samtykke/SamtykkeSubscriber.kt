package no.fintlabs.personvern.samtykke.samtykke

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.datasync.ResourceSubscriber
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.validator.ValidatorService
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SamtykkeSubscriber(
    webClient: WebClient,
    props: AdapterProperties,
    publisher: SamtykkePublisher,
    validatorService: ValidatorService,
) : ResourceSubscriber<SamtykkeResource, SamtykkePublisher>(webClient, props, publisher, validatorService) {

    override fun getCapability(): AdapterCapability = adapterProperties.getCapabilityByResource("samtykke")

    override fun createSyncPageEntry(resource: SamtykkeResource): SyncPageEntry =
        SyncPageEntry.of(resource.systemId.identifikatorverdi, resource)
}
