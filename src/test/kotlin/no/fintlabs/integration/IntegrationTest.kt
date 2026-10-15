package no.fintlabs.integration

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Verifies that the full application context starts against a real Postgres (Flyway + JPA + adapter beans).
 * Sync and event flows are asynchronous and are not asserted here.
 */
@SpringBootTest
@Testcontainers
class IntegrationTest {

    @Test
    fun contextLoads() {
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("fint.database.url") { postgres.jdbcUrl }
            registry.add("fint.database.username") { postgres.username }
            registry.add("fint.database.password") { postgres.password }
            registry.add("spring.datasource.hikari.schema") { "public" }
            // Keep the test off the network: registration and token requests fail fast against localhost.
            registry.add("fint.adapter.base-url") { "http://localhost:1" }
            registry.add("spring.security.oauth2.client.provider.fint-idp.token-uri") { "http://localhost:1/token" }
        }
    }
}
