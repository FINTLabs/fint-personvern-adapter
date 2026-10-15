package no.fintlabs

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.personvern.samtykke.behandling.BehandlingJpaRepository
import no.fintlabs.personvern.samtykke.behandling.BehandlingRepository
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

/**
 * Runs against a real Postgres with the Flyway schema applied: the json column type has no H2 equivalent.
 * In production Hikari pins the connection to a schema named after the database user (created by the PGUser
 * operator); the test container has no such schema, so the connection is pinned to public instead.
 */
@DataJpaTest(properties = ["spring.datasource.hikari.schema=public"])
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BehandlingRepositoryTest(
    @Autowired private val jpaRepository: BehandlingJpaRepository,
) {

    @Test
    fun `saveResources persists the resource and returns it`() {
        val resource = BehandlingResource().apply {
            aktiv = true
            formal = "Test formal"
            systemId = Identifikator().apply { identifikatorverdi = "12345" }
        }
        val event = RequestFintEvent().apply { orgId = "fintlabs.no" }
        val repository = BehandlingRepository(jpaRepository)

        val saved = repository.saveResources(resource, event)

        assertEquals(resource.formal, saved.formal)
        assertEquals(1, repository.getResources().size)
        assertEquals("12345", repository.getResources().single().systemId.identifikatorverdi)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
