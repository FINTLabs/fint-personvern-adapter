package no.fintlabs.personvern.samtykke

import no.fintlabs.ResourceVerifierService
import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.stereotype.Component

@Component
class TjenesteAdapter(
    private val repository: TjenesteJpaRepository,
    private val verifier: ResourceVerifierService,
) : ResourceSource<TjenesteResource>, EventHandler<TjenesteResource> {

    override fun resources(orgId: String): List<TjenesteResource> = repository.findAllResourcesByOrgId(orgId)

    override fun handle(event: RequestFintEvent, resource: TjenesteResource): EventResult<TjenesteResource> {
        if (!verifier.verifyTjenesteResource(resource)) return EventResult.Rejected("Fields in tjenesteResource cannot be null or empty")
        return EventResult.Saved(repository.save(TjenesteEntity.toEntity(resource, event.orgId)).resource)
    }
}
