package no.fintlabs.personvern.kodeverk.personopplysning

import no.fintlabs.adapter.events.WriteableResourceRepository
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.fintlabs.logger
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.springframework.stereotype.Repository

@Repository
class PersonopplysningRepository(
    private val jpaRepository: PersonopplysningJpaRepository,
) : WriteableResourceRepository<PersonopplysningResource> {

    private val log = logger<PersonopplysningRepository>()

    override fun getResources(): List<PersonopplysningResource> = jpaRepository.findAll().map { it.toResource() }

    override fun getUpdatedResources(): List<PersonopplysningResource> = emptyList()

    override fun saveResources(resource: PersonopplysningResource, requestFintEvent: RequestFintEvent): PersonopplysningResource {
        val id = resource.systemId.identifikatorverdi
        if (requestFintEvent.operationType == OperationType.UPDATE && !jpaRepository.existsById(id)) {
            throw NotInDatabaseException("Can not update resource that is not in the database")
        }
        log.info("Save personopplysning {}", resource.navn)
        jpaRepository.save(PersonopplysningEntity.toEntity(resource))
        return resource
    }
}
