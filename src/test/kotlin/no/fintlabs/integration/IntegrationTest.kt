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
 * tenants, and that every configured capability has a source. Gateway traffic is pointed at a closed port.
 */
@SpringBootTest
@Testcontainers
class IntegrationTest(
    @Autowired private val registry: TenantRegistry,
) {

    @Test
    fun `context starts with both tenants`() {
        assertEquals(listOf("rogfk.no", "agderfk.no"), registry.tenants.map { it.orgId })
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
            listOf("rogfk.no", "agderfk.no").forEachIndexed { i, org ->
                registry.add("fint.adapter.tenants[$i].org-id") { org }
                registry.add("fint.adapter.tenants[$i].id") { "https://vigoiks.no/${org.replace('.', '-')}/personvern" }
                registry.add("fint.adapter.tenants[$i].username") { "$org-user" }
                registry.add("fint.adapter.tenants[$i].password") { "pw" }
                registry.add("fint.adapter.tenants[$i].client-id") { "c" }
                registry.add("fint.adapter.tenants[$i].client-secret") { "s" }
            }
        }
    }
}
