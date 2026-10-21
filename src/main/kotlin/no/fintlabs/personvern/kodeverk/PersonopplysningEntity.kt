package no.fintlabs.personvern.kodeverk

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import no.fintlabs.personvern.ResourceKey
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import java.util.Date

/**
 * Personopplysning is stored column by column (unlike the samtykke resources, which are stored as JSON),
 * matching the table the kodeverk adapter used.
 */
@Entity(name = "Personopplysning")
class PersonopplysningEntity(
    @EmbeddedId
    var key: ResourceKey,
    @Column(nullable = false)
    var navn: String,
    var kode: String? = null,
    var passiv: Boolean = false,
    var startGyldighetsdato: Date? = null,
    var endGyldighetsdato: Date? = null,
    var beskrivelseGyldighetsPeriode: String? = null,
    var startIdentifikatorDato: Date? = null,
    var endIdentifikatorDato: Date? = null,
) {
    fun toResource(): PersonopplysningResource =
        PersonopplysningResource().apply {
            systemId = Identifikator().apply { identifikatorverdi = key.id }
            kode = this@PersonopplysningEntity.kode
            navn = this@PersonopplysningEntity.navn
            passiv = this@PersonopplysningEntity.passiv
            gyldighetsperiode =
                Periode().apply {
                    start = startGyldighetsdato
                    slutt = endGyldighetsdato
                    beskrivelse = beskrivelseGyldighetsPeriode
                }
        }

    companion object {
        fun toEntity(
            resource: PersonopplysningResource,
            orgId: String,
        ) = PersonopplysningEntity(
            key = ResourceKey(orgId, resource.systemId.identifikatorverdi),
            navn = resource.navn,
            kode = resource.kode,
            passiv = resource.passiv ?: false,
            startGyldighetsdato = resource.gyldighetsperiode?.start,
            endGyldighetsdato = resource.gyldighetsperiode?.slutt,
            beskrivelseGyldighetsPeriode = resource.gyldighetsperiode?.beskrivelse,
        )
    }
}
