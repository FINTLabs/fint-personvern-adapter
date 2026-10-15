package no.fintlabs.personvern.samtykke.tjeneste

import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TjenesteJpaRepository : JpaRepository<TjenesteEntity, String> {

    @Query("SELECT b.resource FROM Tjeneste b")
    fun findAllResources(): List<TjenesteResource>
}
