package no.fintlabs.kodeverk

import no.fintlabs.personvern.kodeverk.behandlingsgrunnlag.BehandlingsgrunnlagRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class BehandlingsgrunnlagRepositoryTest {

    private val repository = BehandlingsgrunnlagRepository()

    @Test
    fun `serves the six GDPR legal bases with stable ids`() {
        val resources = repository.getResources()

        assertEquals(listOf("a", "b", "c", "d", "e", "f"), resources.map { it.systemId.identifikatorverdi })
        assertEquals("Samtykke", resources.first().navn)
        resources.forEach {
            assertEquals(it.systemId.identifikatorverdi, it.kode)
            assertFalse(it.passiv)
            assertNotNull(it.gyldighetsperiode.start)
        }
    }
}
