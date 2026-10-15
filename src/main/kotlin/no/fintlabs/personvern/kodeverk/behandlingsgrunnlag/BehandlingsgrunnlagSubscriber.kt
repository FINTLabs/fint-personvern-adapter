package no.fintlabs.personvern.kodeverk.behandlingsgrunnlag

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.datasync.ResourceSubscriber
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.validator.ValidatorService
import no.novari.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BehandlingsgrunnlagSubscriber(
    webClient: WebClient,
    props: AdapterProperties,
    publisher: BehandlingsgrunnlagPublisher,
    validatorService: ValidatorService,
) : ResourceSubscriber<BehandlingsgrunnlagResource, BehandlingsgrunnlagPublisher>(webClient, props, publisher, validatorService) {

    override fun getCapability(): AdapterCapability = adapterProperties.getCapabilityByResource("behandlingsgrunnlag")

    override fun createSyncPageEntry(resource: BehandlingsgrunnlagResource): SyncPageEntry =
        SyncPageEntry.of(resource.systemId.identifikatorverdi, resource)
}
