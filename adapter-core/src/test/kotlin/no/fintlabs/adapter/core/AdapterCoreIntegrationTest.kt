package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.event.RequestFintEvent
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Two tenants against a stub gateway: each registers with its own token, gets its own full sync, and answers
 * events with the event's orgId.
 */
@SpringBootTest(
    properties = [
        "fint.adapter.heartbeat-interval=1s",
        "fint.adapter.event-poll-interval=200ms",
        "fint.adapter.full-sync-initial-delay=200ms",
        "fint.adapter.full-sync-interval=1h",
        "fint.adapter.page-size=2",
        "fint.adapter.capabilities.behandling.domain-name=personvern",
        "fint.adapter.capabilities.behandling.package-name=samtykke",
        "fint.adapter.capabilities.behandling.resource-name=behandling",
        "fint.adapter.capabilities.behandling.full-sync-interval-in-days=1",
        "fint.adapter.capabilities.behandling.delta-sync-interval=IMMEDIATE",
        "fint.adapter.tenants[0].org-id=rogfk.no",
        "fint.adapter.tenants[0].id=https://vigoiks.no/rogfk-no/personvern",
        "fint.adapter.tenants[0].username=rogfk-user",
        "fint.adapter.tenants[0].password=pw",
        "fint.adapter.tenants[0].client-id=c",
        "fint.adapter.tenants[0].client-secret=s",
        "fint.adapter.tenants[1].org-id=agderfk.no",
        "fint.adapter.tenants[1].id=https://vigoiks.no/agderfk-no/personvern",
        "fint.adapter.tenants[1].username=agderfk-user",
        "fint.adapter.tenants[1].password=pw",
        "fint.adapter.tenants[1].client-id=c",
        "fint.adapter.tenants[1].client-secret=s",
    ],
)
class AdapterCoreIntegrationTest(
    @Autowired private val behandlinger: BehandlingStub,
) {

    @Test
    fun `every tenant registers, syncs and answers events as itself`() {
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            assertEquals(2, gateway.calls("POST", "/provider/register").size)
            assertEquals(4, gateway.calls("POST", "/provider/personvern/samtykke/behandling").size, "3 resources at page size 2 = 2 pages per tenant")
        }
        val registrations = gateway.calls("POST", "/provider/register")
        assertEquals(setOf("token-for-rogfk-user", "token-for-agderfk-user"), registrations.map { it.bearer }.toSet())
        assertTrue(registrations.any { it.body.contains("\"orgId\":\"rogfk.no\"") && it.body.contains("\"username\":\"rogfk-user\"") })

        val pages = gateway.calls("POST", "/provider/personvern/samtykke/behandling")
        assertTrue(pages.any { it.bearer == "token-for-rogfk-user" && it.body.contains("\"orgId\":\"rogfk.no\"") && it.body.contains("rogfk.no-1") })
        assertTrue(pages.any { it.bearer == "token-for-agderfk-user" && it.body.contains("\"orgId\":\"agderfk.no\"") })
        assertEquals(2, pages.count { it.bearer == "token-for-rogfk-user" })
        assertTrue(pages.all { it.body.contains("\"totalPages\":2") && it.body.contains("\"totalSize\":3") })

        gateway.queueEvent("""{"corrId":"corr-1","orgId":"rogfk.no","operationType":"CREATE","value":"\"new-one\""}""")
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertEquals(1, gateway.calls("POST", "/provider/event").size)
        }
        val answer = gateway.calls("POST", "/provider/event").single()
        assertTrue(answer.body.contains("\"corrId\":\"corr-1\""))
        assertTrue(answer.body.contains("\"orgId\":\"rogfk.no\""))
        assertTrue(answer.body.contains("\"identifier\":\"new-one\""))
        assertEquals(listOf("rogfk.no" to "new-one"), behandlinger.handled)

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertTrue(gateway.calls("POST", "/provider/heartbeat").size >= 2)
        }
        assertEquals(2, gateway.tokenRequests.get(), "one token per tenant, reused across calls")
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    class TestApp {
        @Bean
        fun behandlingStub() = BehandlingStub()
    }

    class BehandlingStub : ResourceSource<String>, EventHandler<String> {
        val handled = CopyOnWriteArrayList<Pair<String, String>>()
        override val resourceName = "behandling"
        override val resourceType = String::class.java
        override fun identifier(resource: String) = resource
        override fun resources(orgId: String) = listOf("$orgId-1", "$orgId-2", "$orgId-3")
        override fun handle(event: RequestFintEvent, resource: String): EventResult<String> {
            handled += event.orgId to resource
            return EventResult.Saved(resource)
        }
    }

    companion object {
        private val gateway = StubGateway()

        @DynamicPropertySource
        @JvmStatic
        fun gateway(registry: DynamicPropertyRegistry) {
            registry.add("fint.adapter.base-url") { gateway.baseUrl }
            registry.add("fint.adapter.token-uri") { gateway.tokenUri }
        }

        @AfterAll
        @JvmStatic
        fun stop() = gateway.close()
    }
}
