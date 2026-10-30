package no.fintlabs

import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceVerifierServiceTest {

    private val verifier = ResourceVerifierService()

    private fun identifikator(value: String) = Identifikator().apply { identifikatorverdi = value }

    private fun behandling(aktiv: Boolean? = true, formal: String? = "test", systemId: String? = "id") =
        BehandlingResource().apply {
            this.aktiv = aktiv
            this.formal = formal
            systemId?.let { this.systemId = identifikator(it) }
        }

    @Test
    fun `behandling fails when aktiv is null`() {
        assertFalse(verifier.verifyBehandlingResource(behandling(aktiv = null, systemId = null)))
    }

    @Test
    fun `behandling fails when formal is empty`() {
        val resource = behandling(formal = "").apply { links = null }
        assertFalse(verifier.verifyBehandlingResource(resource))
    }

    @Test
    fun `behandling passes when all fields and links are present`() {
        val resource = behandling().apply {
            addBehandlingsgrunnlag(Link("api.felleskomponent.no"))
            addTjeneste(Link("api.felleskomponent.no"))
            addPersonopplysning(Link("api.felleskomponent.no"))
        }
        assertTrue(verifier.verifyBehandlingResource(resource))
    }

    @Test
    fun `behandling fails when links are empty`() {
        val resource = behandling().apply {
            addBehandlingsgrunnlag(Link(""))
            addTjeneste(Link(""))
        }
        assertFalse(verifier.verifyBehandlingResource(resource))
    }

    @Test
    fun `behandling fails when links have no href`() {
        val resource = behandling().apply {
            addBehandlingsgrunnlag(Link())
            addTjeneste(Link())
            addPersonopplysning(Link())
        }
        assertFalse(verifier.verifyBehandlingResource(resource))
    }

    @Test
    fun `samtykke passes when all fields are present`() {
        val resource = SamtykkeResource().apply {
            systemId = identifikator("id")
            opprettet = Date()
            gyldighetsperiode = Periode().apply { start = Date() }
            addBehandling(Link("api.felleskomponent.no"))
            addPerson(Link("api.felleskomponent.no"))
        }
        assertTrue(verifier.verifySamtykkeResource(resource))
    }

    @Test
    fun `samtykke fails when gyldighetsperiode is null`() {
        val resource = SamtykkeResource().apply {
            systemId = identifikator("id")
            opprettet = Date()
            gyldighetsperiode = null
            addBehandling(Link("api.felleskomponent.no"))
            addPerson(Link("api.felleskomponent.no"))
        }
        assertFalse(verifier.verifySamtykkeResource(resource))
    }

    @Test
    fun `tjeneste passes when all fields are present`() {
        val resource = TjenesteResource().apply {
            systemId = identifikator("id")
            navn = "testName"
            addBehandling(Link("api.felleskomponent.no"))
        }
        assertTrue(verifier.verifyTjenesteResource(resource))
    }

    @Test
    fun `tjeneste fails when navn is empty`() {
        val resource = TjenesteResource().apply {
            systemId = identifikator("id")
            navn = ""
            addBehandling(Link("api.felleskomponent.no"))
        }
        assertFalse(verifier.verifyTjenesteResource(resource))
    }
}
