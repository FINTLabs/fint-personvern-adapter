package no.fintlabs.personvern.kodeverk.personopplysning

import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.datasync.ResourceSubscriber
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.validator.ValidatorService
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PersonopplysningSubscriber(
    webClient: WebClient,
    props: AdapterProperties,
    publisher: PersonopplysningPublisher,
    validatorService: ValidatorService,
) : ResourceSubscriber<PersonopplysningResource, PersonopplysningPublisher>(webClient, props, publisher, validatorService) {

    override fun getCapability(): AdapterCapability = adapterProperties.getCapabilityByResource("personopplysning")

    override fun createSyncPageEntry(resource: PersonopplysningResource): SyncPageEntry =
        SyncPageEntry.of(resource.systemId.identifikatorverdi, resource)
}
