package no.fintlabs.personvern.kodeverk.personopplysning

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonopplysningJpaRepository : JpaRepository<PersonopplysningEntity, String>
