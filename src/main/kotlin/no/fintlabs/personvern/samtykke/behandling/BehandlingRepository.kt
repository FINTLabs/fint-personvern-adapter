package no.fintlabs.personvern.samtykke.behandling

import no.fintlabs.adapter.events.WriteableResourceRepository
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.springframework.stereotype.Repository

@Repository
class BehandlingRepository(
    private val jpaRepository: BehandlingJpaRepository,
) : WriteableResourceRepository<BehandlingResource> {

    override fun saveResources(resource: BehandlingResource, requestFintEvent: RequestFintEvent): BehandlingResource =
        jpaRepository.save(BehandlingEntity.toEntity(resource, requestFintEvent.orgId)).resource

    override fun getResources(): List<BehandlingResource> = jpaRepository.findAllResources()

    override fun getUpdatedResources(): List<BehandlingResource> = emptyList()
}
