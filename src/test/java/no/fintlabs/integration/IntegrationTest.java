package no.fintlabs.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("fint.database.url", postgres::getJdbcUrl);
        registry.add("fint.database.username", postgres::getUsername);
        registry.add("fint.database.password", postgres::getPassword);
        // Keep the test off the network: registration and token requests fail fast against localhost.
        registry.add("fint.adapter.base-url", () -> "http://localhost:1");
        registry.add("spring.security.oauth2.client.provider.fint-idp.token-uri", () -> "http://localhost:1/token");
    }

    /**
     * Verifies that the full application context starts against a real Postgres (Flyway + JPA + adapter beans).
     * Sync and event flows are asynchronous and are not asserted here.
     */
    @Test
    void contextLoads() {
    }

}
