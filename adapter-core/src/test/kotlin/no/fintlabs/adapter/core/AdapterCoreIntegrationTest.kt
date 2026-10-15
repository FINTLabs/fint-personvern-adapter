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
 * Two credentials against a stub gateway, one of them serving two orgs: a contract per org, a token per
 * credential, a full sync per org, and events polled once per credential and answered with the event's orgId.
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
        "fint.adapter.tenants[0].orgs=novari.no,side.novari.no",
        "fint.adapter.tenants[0].id=https://vigoiks.no/novari-no/personvern",
        "fint.adapter.tenants[0].username=novari-user",
        "fint.adapter.tenants[0].password=pw",
        "fint.adapter.tenants[0].client-id=c",
        "fint.adapter.tenants[0].client-secret=s",
        "fint.adapter.tenants[1].orgs=rogfk.no",
        "fint.adapter.tenants[1].id=https://vigoiks.no/rogfk-no/personvern",
        "fint.adapter.tenants[1].username=rogfk-user",
        "fint.adapter.tenants[1].password=pw",
        "fint.adapter.tenants[1].client-id=c",
        "fint.adapter.tenants[1].client-secret=s",
    ],
)
class AdapterCoreIntegrationTest(
    @Autowired private val behandlinger: BehandlingStub,
) {

    @Test
    fun `every org gets a contract and a sync, every credential polls once and answers as the event's org`() {
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            assertEquals(3, gateway.calls("POST", "/provider/register").size, "one contract per (credential, org)")
            assertEquals(6, gateway.calls("POST", "/provider/personvern/samtykke/behandling").size, "3 resources at page size 2 = 2 pages per org")
        }
        val registrations = gateway.calls("POST", "/provider/register")
        assertEquals(2, registrations.count { it.bearer == "token-for-novari-user" })
        assertTrue(registrations.any { it.body.contains("\"orgId\":\"side.novari.no\"") && it.body.contains("\"username\":\"novari-user\"") })
        assertTrue(registrations.any { it.body.contains("\"orgId\":\"rogfk.no\"") && it.bearer == "token-for-rogfk-user" })
        assertTrue(registrations.all { it.body.contains("\"eventCapabilities\":[{") && it.body.contains("\"resourceName\":\"behandling\"") && it.body.contains("\"CREATE\"") && !it.body.contains("\"DELETE\"") }, "handlers' operations become event capabilities")

        val pages = gateway.calls("POST", "/provider/personvern/samtykke/behandling")
        assertTrue(pages.any { it.bearer == "token-for-novari-user" && it.body.contains("\"orgId\":\"side.novari.no\"") && it.body.contains("side.novari.no-1") })
        assertTrue(pages.any { it.bearer == "token-for-rogfk-user" && it.body.contains("\"orgId\":\"rogfk.no\"") })
        assertEquals(4, pages.count { it.bearer == "token-for-novari-user" })
        assertTrue(pages.all { it.body.contains("\"totalPages\":2") && it.body.contains("\"totalSize\":3") })

        gateway.queueEvent("token-for-novari-user", """{"corrId":"corr-1","orgId":"side.novari.no","operationType":"CREATE","value":"\"new-one\""}""")
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertEquals(1, gateway.calls("POST", "/provider/event").size)
        }
        val answer = gateway.calls("POST", "/provider/event").single()
        assertTrue(answer.body.contains("\"corrId\":\"corr-1\""))
        assertTrue(answer.body.contains("\"orgId\":\"side.novari.no\""))
        assertTrue(answer.body.contains("\"identifier\":\"new-one\""))
        assertEquals("token-for-novari-user", answer.bearer)
        assertEquals(listOf("side.novari.no" to "new-one"), behandlinger.handled, "handled once although the credential serves two orgs")

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertTrue(gateway.calls("POST", "/provider/heartbeat").size >= 3)
        }
        assertEquals(2, gateway.tokenRequests.get(), "one token per credential, reused across orgs and calls")
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
