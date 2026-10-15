package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
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
        "fint.adapter.tenants.novari.orgs=novari.no,side.novari.no",
        "fint.adapter.tenants.novari.username=novari-user",
        "fint.adapter.tenants.novari.password=pw",
        "fint.adapter.tenants.novari.client-id=c",
        "fint.adapter.tenants.novari.client-secret=s",
        "fint.adapter.tenants.rogfk.orgs=rogfk.no",
        "fint.adapter.tenants.rogfk.username=rogfk-user",
        "fint.adapter.tenants.rogfk.password=pw",
        "fint.adapter.tenants.rogfk.client-id=c",
        "fint.adapter.tenants.rogfk.client-secret=s",
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
        assertTrue(registrations.any { it.body.contains("\"adapterId\":\"https://vigoiks.no/novari-no/personvern\"") }, "adapter id derived from the first org and the domain")
        assertTrue(registrations.all { it.body.contains("\"eventCapabilities\":[{") && it.body.contains("\"resourceName\":\"behandling\"") && it.body.contains("\"CREATE\"") && !it.body.contains("\"DELETE\"") }, "handlers' operations become event capabilities")

        val pages = gateway.calls("POST", "/provider/personvern/samtykke/behandling")
        assertTrue(pages.any { it.bearer == "token-for-novari-user" && it.body.contains("\"orgId\":\"side.novari.no\"") && it.body.contains("side.novari.no-1") })
        assertTrue(pages.any { it.bearer == "token-for-rogfk-user" && it.body.contains("\"orgId\":\"rogfk.no\"") })
        assertEquals(4, pages.count { it.bearer == "token-for-novari-user" })
        assertTrue(pages.all { it.body.contains("\"totalPages\":2") && it.body.contains("\"totalSize\":3") })

        gateway.queueEvent("token-for-novari-user", """{"corrId":"corr-1","orgId":"side.novari.no","operationType":"CREATE","value":"{\"systemId\":{\"identifikatorverdi\":\"new-one\"}}"}""")
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

    /** Only the type parameter says which resource this is; ref, type and identifier are derived. */
    class BehandlingStub : ResourceSource<BehandlingResource>, EventHandler<BehandlingResource> {
        val handled = CopyOnWriteArrayList<Pair<String, String>>()
        override fun resources(orgId: String) = (1..3).map { behandling("$orgId-$it") }
        override fun handle(event: RequestFintEvent, resource: BehandlingResource): EventResult<BehandlingResource> {
            handled += event.orgId to resource.systemId.identifikatorverdi
            return EventResult.Saved(resource)
        }

        private fun behandling(id: String) = BehandlingResource().apply { systemId = Identifikator().apply { identifikatorverdi = id } }
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
