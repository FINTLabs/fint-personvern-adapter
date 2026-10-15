package no.fintlabs.personvern.samtykke.tjeneste

import no.fintlabs.adapter.events.WriteableResourceRepository
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.stereotype.Repository

@Repository
class TjenesteRepository(
    private val jpaRepository: TjenesteJpaRepository,
) : WriteableResourceRepository<TjenesteResource> {

    override fun saveResources(resource: TjenesteResource, requestFintEvent: RequestFintEvent): TjenesteResource =
        jpaRepository.save(TjenesteEntity.toEntity(resource, requestFintEvent.orgId)).resource

    override fun getResources(): List<TjenesteResource> = jpaRepository.findAllResources()

    override fun getUpdatedResources(): List<TjenesteResource> = emptyList()
}
