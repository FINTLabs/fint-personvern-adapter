package no.fintlabs.personvern.samtykke

import no.fintlabs.ResourceVerifierService
import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.springframework.stereotype.Component
import java.util.Date

@Component
class SamtykkeAdapter(
    private val repository: SamtykkeJpaRepository,
    private val verifier: ResourceVerifierService,
) : ResourceSource<SamtykkeResource>, EventHandler<SamtykkeResource> {

    override fun resources(orgId: String): List<SamtykkeResource> = repository.findAllResourcesByOrgId(orgId)

    /** A create is stored as-is (with `opprettet` defaulted to now); updates must pass verification. */
    override fun handle(event: RequestFintEvent, resource: SamtykkeResource): EventResult<SamtykkeResource> {
        val isCreate = event.operationType == OperationType.CREATE
        if (!isCreate && !verifier.verifySamtykkeResource(resource)) return EventResult.Rejected("Fields in samtykkeResource cannot be null or empty")
        if (isCreate && resource.opprettet == null) resource.opprettet = Date()
        return EventResult.Saved(repository.save(SamtykkeEntity.toEntity(resource, event.orgId)).resource)
    }
}
