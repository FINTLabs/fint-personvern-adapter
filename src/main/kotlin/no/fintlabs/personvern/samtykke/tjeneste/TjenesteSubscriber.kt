package no.fintlabs.personvern.samtykke.tjeneste

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.datasync.ResourceSubscriber
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.validator.ValidatorService
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class TjenesteSubscriber(
    webClient: WebClient,
    props: AdapterProperties,
    publisher: TjenestePublisher,
    validatorService: ValidatorService,
) : ResourceSubscriber<TjenesteResource, TjenestePublisher>(webClient, props, publisher, validatorService) {

    override fun getCapability(): AdapterCapability = adapterProperties.getCapabilityByResource("tjeneste")

    override fun createSyncPageEntry(resource: TjenesteResource): SyncPageEntry =
        SyncPageEntry.of(resource.systemId.identifikatorverdi, resource)
}
