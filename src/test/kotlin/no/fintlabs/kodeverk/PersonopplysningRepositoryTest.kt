package no.fintlabs.kodeverk

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.fintlabs.personvern.kodeverk.personopplysning.NotInDatabaseException
import no.fintlabs.personvern.kodeverk.personopplysning.PersonopplysningJpaRepository
import no.fintlabs.personvern.kodeverk.personopplysning.PersonopplysningRepository
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.resource.personvern.kodeverk.PersonopplysningResource
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
import kotlin.test.assertFailsWith

@DataJpaTest(properties = ["spring.datasource.hikari.schema=public"])
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PersonopplysningRepositoryTest(
    @Autowired private val jpaRepository: PersonopplysningJpaRepository,
) {
    private val repository by lazy { PersonopplysningRepository(jpaRepository) }

    private fun resource(id: String, navn: String) = PersonopplysningResource().apply {
        systemId = Identifikator().apply { identifikatorverdi = id }
        kode = id
        this.navn = navn
    }

    private fun event(type: OperationType) = RequestFintEvent().apply {
        orgId = "fintlabs.no"
        operationType = type
    }

    @Test
    fun `create then update is persisted`() {
        repository.saveResources(resource("elev.navn", "Navn"), event(OperationType.CREATE))
        repository.saveResources(resource("elev.navn", "Fullt navn"), event(OperationType.UPDATE))

        val stored = repository.getResources().single { it.systemId.identifikatorverdi == "elev.navn" }
        assertEquals("Fullt navn", stored.navn)
    }

    @Test
    fun `update of an unknown resource is refused`() {
        assertFailsWith<NotInDatabaseException> {
            repository.saveResources(resource("ukjent", "Ukjent"), event(OperationType.UPDATE))
        }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
