package no.fintlabs.integration

import no.fintlabs.adapter.core.TenantRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

/**
 * Verifies that the full application starts against a real Postgres (Flyway + JPA + adapter runtime) with two
 * credentials, one serving two orgs, and that every configured capability has a source. Gateway traffic is
 * pointed at a closed port.
 */
@SpringBootTest
@Testcontainers
class IntegrationTest(
    @Autowired private val registry: TenantRegistry,
) {

    @Test
    fun `context starts with both credentials and all their orgs`() {
        assertEquals(listOf(listOf("novari.no", "side.novari.no"), listOf("rogfk.no")), registry.tenants.map { it.orgs })
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("fint.database.url") { postgres.jdbcUrl }
            registry.add("fint.database.username") { postgres.username }
            registry.add("fint.database.password") { postgres.password }
            registry.add("spring.datasource.hikari.schema") { "public" }
            registry.add("fint.adapter.base-url") { "http://localhost:1" }
            registry.add("fint.adapter.token-uri") { "http://localhost:1/token" }
            listOf("novari.no,side.novari.no", "rogfk.no").forEachIndexed { i, orgs ->
                val main = orgs.substringBefore(',')
                registry.add("fint.adapter.tenants[$i].orgs") { orgs }
                registry.add("fint.adapter.tenants[$i].id") { "https://vigoiks.no/${main.replace('.', '-')}/personvern" }
                registry.add("fint.adapter.tenants[$i].username") { "$main-user" }
                registry.add("fint.adapter.tenants[$i].password") { "pw" }
                registry.add("fint.adapter.tenants[$i].client-id") { "c" }
                registry.add("fint.adapter.tenants[$i].client-secret") { "s" }
            }
        }
    }
}
