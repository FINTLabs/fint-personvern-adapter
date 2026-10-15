package no.fintlabs.personvern.samtykke.samtykke

import no.fintlabs.adapter.events.WriteableResourceRepository
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.springframework.stereotype.Repository

@Repository
class SamtykkeRepository(
    private val jpaRepository: SamtykkeJpaRepository,
) : WriteableResourceRepository<SamtykkeResource> {

    override fun saveResources(resource: SamtykkeResource, requestFintEvent: RequestFintEvent): SamtykkeResource =
        jpaRepository.save(SamtykkeEntity.toEntity(resource, requestFintEvent.orgId)).resource

    override fun getResources(): List<SamtykkeResource> = jpaRepository.findAllResources()

    override fun getUpdatedResources(): List<SamtykkeResource> = emptyList()
}
