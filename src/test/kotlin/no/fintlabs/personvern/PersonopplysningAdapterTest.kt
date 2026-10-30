package no.fintlabs.personvern

import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.fintlabs.personvern.kodeverk.PersonopplysningAdapter
import no.fintlabs.personvern.kodeverk.PersonopplysningJpaRepository
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PersonopplysningAdapterTest(
    @Autowired private val repository: PersonopplysningJpaRepository,
) : PostgresJpaTest() {

    private val adapter by lazy { PersonopplysningAdapter(repository) }

    private fun resource(id: String, navn: String) = PersonopplysningResource().apply {
        systemId = Identifikator().apply { identifikatorverdi = id }
        kode = id
        this.navn = navn
        passiv = true
        gyldighetsperiode = Periode().apply {
            start = Date(1_000_000)
            slutt = Date(2_000_000)
            beskrivelse = "gyldig"
        }
    }

    private fun event(orgId: String, type: OperationType) = RequestFintEvent().apply {
        this.orgId = orgId
        operationType = type
    }

    @Test
    fun `create then update is persisted per org and survives the round trip`() {
        adapter.handle(event("rogfk.no", OperationType.CREATE), resource("elev.navn", "Navn"))
        adapter.handle(event("rogfk.no", OperationType.UPDATE), resource("elev.navn", "Fullt navn"))
        adapter.handle(event("agderfk.no", OperationType.CREATE), resource("elev.navn", "Agder-navn"))

        val stored = adapter.resources("rogfk.no").single()
        assertEquals("Fullt navn", stored.navn)
        assertEquals("elev.navn", stored.kode)
        assertEquals(true, stored.passiv)
        assertEquals(Date(1_000_000), stored.gyldighetsperiode.start)
        assertEquals(Date(2_000_000), stored.gyldighetsperiode.slutt)
        assertEquals("gyldig", stored.gyldighetsperiode.beskrivelse)
        assertEquals("Agder-navn", adapter.resources("agderfk.no").single().navn)
    }

    @Test
    fun `update of a personopplysning the org does not have is rejected`() {
        adapter.handle(event("agderfk.no", OperationType.CREATE), resource("ukjent", "Ukjent"))

        val result = adapter.handle(event("rogfk.no", OperationType.UPDATE), resource("ukjent", "Ukjent"))

        assertIs<EventResult.Rejected>(result)
        assertEquals(emptyList(), adapter.resources("rogfk.no"))
    }
}
