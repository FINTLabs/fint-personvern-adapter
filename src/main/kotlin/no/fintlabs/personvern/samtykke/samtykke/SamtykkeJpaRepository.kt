package no.fintlabs.personvern.samtykke.samtykke

import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SamtykkeJpaRepository : JpaRepository<SamtykkeEntity, String> {

    @Query("SELECT b.resource FROM Samtykke b")
    fun findAllResources(): List<SamtykkeResource>
}
