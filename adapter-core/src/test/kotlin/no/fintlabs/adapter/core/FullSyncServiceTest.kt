package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.config.TenantProperties
import no.fintlabs.adapter.core.sync.FullSyncService
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.core.sync.SyncPageValidator
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.sync.SyncType
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullSyncServiceTest {

    private val capability = AdapterCapability("personvern", "samtykke", "behandling", 1, AdapterCapability.DeltaSyncInterval.IMMEDIATE)
    private val tenant = TenantProperties(listOf("rogfk.no"), "https://vigoiks.no/rogfk-no/personvern", "user", "pass", "client", "secret")
    private val properties = AdapterProperties(baseUrl = "http://gateway", pageSize = 3, capabilities = mapOf("behandling" to capability), tenants = listOf(tenant))
    private val client = TenantClient(tenant, WebClient.create())
    private val service = FullSyncService(properties, SyncPageValidator())

    private val source = object : ResourceSource<String> {
        override val resourceName = "behandling"
        override val resourceType = String::class.java
        override fun identifier(resource: String) = resource
        override fun resources(orgId: String) = emptyList<String>()
    }

    @Test
    fun `splits resources into pages with consistent metadata`() {
        val pages = service.pages(client, "rogfk.no", capability, source, listOf("a", "b", "c", "d", "e", "f", "g"), SyncType.FULL)

        assertEquals(3, pages.size)
        assertEquals(listOf(3, 3, 1), pages.map { it.resources.size })
        assertEquals(listOf(1L, 2L, 3L), pages.map { it.metadata.page })
        assertTrue(pages.all { it.metadata.totalPages == 3L && it.metadata.totalSize == 7L })
        assertTrue(pages.all { it.metadata.orgId == "rogfk.no" && it.metadata.adapterId == tenant.id })
        assertTrue(pages.all { it.metadata.uriRef == "/personvern/samtykke/behandling" })
        assertEquals(1, pages.map { it.metadata.corrId }.toSet().size)
        assertEquals("g", pages.last().resources.single().identifier)
        assertTrue(SyncPageValidator().validate(pages, 7).none { it.contains("size") })
    }

    @Test
    fun `no resources means no pages`() {
        assertEquals(0, service.pages(client, "rogfk.no", capability, source, emptyList(), SyncType.FULL).size)
    }

    @Test
    fun `full sync interval comes from the capability unless overridden`() {
        assertEquals(java.time.Duration.ofDays(1), properties.fullSyncIntervalFor(capability))
        val overridden = properties.copy(fullSyncInterval = java.time.Duration.ofMinutes(5))
        assertEquals(java.time.Duration.ofMinutes(5), overridden.fullSyncIntervalFor(capability))
    }
}
