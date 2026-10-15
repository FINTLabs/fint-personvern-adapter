package no.fintlabs.personvern.samtykke

import no.fintlabs.personvern.ResourceKey
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BehandlingJpaRepository : JpaRepository<BehandlingEntity, ResourceKey> {
    @Query("select b.resource from Behandling b where b.key.orgId = :orgId")
    fun findAllResourcesByOrgId(orgId: String): List<BehandlingResource>
}

@Repository
interface SamtykkeJpaRepository : JpaRepository<SamtykkeEntity, ResourceKey> {
    @Query("select s.resource from Samtykke s where s.key.orgId = :orgId")
    fun findAllResourcesByOrgId(orgId: String): List<SamtykkeResource>
}

@Repository
interface TjenesteJpaRepository : JpaRepository<TjenesteEntity, ResourceKey> {
    @Query("select t.resource from Tjeneste t where t.key.orgId = :orgId")
    fun findAllResourcesByOrgId(orgId: String): List<TjenesteResource>
}
