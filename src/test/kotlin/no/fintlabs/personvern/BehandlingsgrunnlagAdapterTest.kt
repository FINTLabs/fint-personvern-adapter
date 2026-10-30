package no.fintlabs.personvern

import no.fintlabs.personvern.kodeverk.BehandlingsgrunnlagAdapter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class BehandlingsgrunnlagAdapterTest {

    private val adapter = BehandlingsgrunnlagAdapter()

    @Test
    fun `serves the six GDPR legal bases with stable ids for any org`() {
        val resources = adapter.resources("rogfk.no")

        assertEquals(listOf("a", "b", "c", "d", "e", "f"), resources.map { adapter.identifier(it) })
        assertEquals("Samtykke", resources.first().navn)
        resources.forEach {
            assertEquals(it.systemId.identifikatorverdi, it.kode)
            assertFalse(it.passiv)
            assertNotNull(it.gyldighetsperiode.start)
        }
        assertEquals(resources.map { it.navn }, adapter.resources("agderfk.no").map { it.navn })
    }
}
