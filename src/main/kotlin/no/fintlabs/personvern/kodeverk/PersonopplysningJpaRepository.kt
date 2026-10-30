package no.fintlabs.personvern.kodeverk

import no.fintlabs.personvern.ResourceKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonopplysningJpaRepository : JpaRepository<PersonopplysningEntity, ResourceKey> {
    fun findAllByKeyOrgId(orgId: String): List<PersonopplysningEntity>
}
