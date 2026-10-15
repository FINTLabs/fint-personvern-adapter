package no.fintlabs.kodeverk

import no.fintlabs.personvern.kodeverk.personopplysning.PersonopplysningEntity
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.test.assertEquals

class PersonopplysningEntityTest {

    @Test
    fun `resource survives a round trip through the entity`() {
        val start = Date(1_000_000)
        val slutt = Date(2_000_000)
        val resource = PersonopplysningResource().apply {
            systemId = Identifikator().apply { identifikatorverdi = "elev.brukernavn" }
            kode = "brukernavn"
            navn = "Brukernavn"
            passiv = true
            gyldighetsperiode = Periode().apply {
                this.start = start
                this.slutt = slutt
                beskrivelse = "gyldig"
            }
        }

        val back = PersonopplysningEntity.toEntity(resource).toResource()

        assertEquals("elev.brukernavn", back.systemId.identifikatorverdi)
        assertEquals("brukernavn", back.kode)
        assertEquals("Brukernavn", back.navn)
        assertEquals(true, back.passiv)
        assertEquals(start, back.gyldighetsperiode.start)
        assertEquals(slutt, back.gyldighetsperiode.slutt)
        assertEquals("gyldig", back.gyldighetsperiode.beskrivelse)
    }

    @Test
    fun `missing passiv and period default safely`() {
        val resource = PersonopplysningResource().apply {
            systemId = Identifikator().apply { identifikatorverdi = "id" }
            navn = "Navn"
        }

        val entity = PersonopplysningEntity.toEntity(resource)

        assertEquals(false, entity.passiv)
        assertEquals(null, entity.startGyldighetsdato)
    }
}
