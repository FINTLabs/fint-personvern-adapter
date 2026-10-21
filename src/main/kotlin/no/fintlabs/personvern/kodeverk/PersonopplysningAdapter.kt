package no.fintlabs.personvern.kodeverk

import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.fintlabs.personvern.ResourceKey
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.springframework.stereotype.Component

@Component
class PersonopplysningAdapter(
    private val repository: PersonopplysningJpaRepository,
) : ResourceSource<PersonopplysningResource>,
    EventHandler<PersonopplysningResource> {
    override fun resources(orgId: String): List<PersonopplysningResource> = repository.findAllByKeyOrgId(orgId).map { it.toResource() }

    /** An update of a personopplysning this org does not have is rejected; a create upserts. */
    override fun handle(
        event: RequestFintEvent,
        resource: PersonopplysningResource,
    ): EventResult<PersonopplysningResource> {
        val key = ResourceKey(event.orgId, identifier(resource))
        if (event.operationType == OperationType.UPDATE && !repository.existsById(key)) {
            return EventResult.Rejected("Can not update resource that is not in the database")
        }
        return EventResult.Saved(repository.save(PersonopplysningEntity.toEntity(resource, event.orgId)).toResource())
    }
}
