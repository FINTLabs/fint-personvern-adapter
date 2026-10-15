package no.fintlabs

import no.fintlabs.personvern.samtykke.behandling.BehandlingEntity
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BehandlingEntityTest {

    @Test
    fun `toEntity keeps the resource and uses systemId as id`() {
        val resource = BehandlingResource().apply {
            aktiv = true
            formal = "Test formal"
            systemId = Identifikator().apply { identifikatorverdi = "12345" }
        }

        val entity = BehandlingEntity.toEntity(resource, "fintlabs.no")

        assertEquals("12345", entity.id)
        assertEquals("fintlabs.no", entity.orgId)
        assertEquals(resource.aktiv, entity.resource.aktiv)
        assertEquals(resource.formal, entity.resource.formal)
        assertEquals(resource.systemId.identifikatorverdi, entity.resource.systemId.identifikatorverdi)
    }
}
