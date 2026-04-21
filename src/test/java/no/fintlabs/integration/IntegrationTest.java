package no.fintlabs.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled("Disabled until further development due to limited time")
@SpringBootTest
@Testcontainers
public class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("fint.database.username", postgres::getUsername);
        registry.add("fint.database.password", postgres::getPassword);

    }


    /**
     * This simple test gives some coverage by verifying that the application and the beans can start up correctly.
     * However, no assertions are made to verify that the data is actually processed correctly, so ideally we should
     * have more specific tests for that.
     * This has proven difficult to implement due to the asynchronous nature of the data processing which makes the
     * tests fail when trying to use @SpyBean and ArgumentCaptor to verify interactions with the publishers.
     * -
     * When using @SpyBean on a Publisher(like we do in utdanning-larling-adapter), the test fails with
     * 'Cannot invoke "java.util.concurrent.locks.ReentrantLock.lock()" because "lock" is null'.
     * When following the flow with the debugger, it seems that the constructor of BehandlingPublisher is called
     * on a different instance than the one subscribe() is called on, causing the lock to be null when subscribe() is
     * called.
     */
    @Test
    void contextLoads() {
    }

}
