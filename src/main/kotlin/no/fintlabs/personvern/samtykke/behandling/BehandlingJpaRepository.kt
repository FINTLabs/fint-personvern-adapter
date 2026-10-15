package no.fintlabs.personvern.samtykke.behandling

import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BehandlingJpaRepository : JpaRepository<BehandlingEntity, String> {

    @Query("SELECT b.resource FROM Behandling b")
    fun findAllResources(): List<BehandlingResource>
}
