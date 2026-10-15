package no.fintlabs.personvern

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer

/**
 * JPA slice against a real Postgres with the Flyway schema applied: the json column type has no H2 equivalent.
 * In production Hikari pins the connection to a schema named after the database user (created by the PGUser
 * operator); the test container has no such schema, so the connection is pinned to public instead.
 */
@DataJpaTest(
    properties = [
        "spring.datasource.hikari.schema=public",
        "fint.adapter.tenants[0].org-id=test.no",
    ],
)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class PostgresJpaTest {
    companion object {
        /**
         * Started once for the JVM rather than per test class: the Spring context (with Flyway already run) is
         * cached across test classes, so the database must outlive each class too.
         */
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16").apply { start() }
    }
}
