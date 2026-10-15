package no.fintlabs.personvern.samtykke

import no.fintlabs.ResourceVerifierService
import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.springframework.stereotype.Component

@Component
class BehandlingAdapter(
    private val repository: BehandlingJpaRepository,
    private val verifier: ResourceVerifierService,
) : ResourceSource<BehandlingResource>, EventHandler<BehandlingResource> {

    override val resourceName = "behandling"
    override val resourceType = BehandlingResource::class.java

    override fun identifier(resource: BehandlingResource): String = resource.systemId.identifikatorverdi

    override fun resources(orgId: String): List<BehandlingResource> = repository.findAllResourcesByOrgId(orgId)

    override fun handle(event: RequestFintEvent, resource: BehandlingResource): EventResult<BehandlingResource> {
        if (!verifier.verifyBehandlingResource(resource)) return EventResult.Rejected("Fields in behandlingResource cannot be null or empty")
        return EventResult.Saved(repository.save(BehandlingEntity.toEntity(resource, event.orgId)).resource)
    }
}
