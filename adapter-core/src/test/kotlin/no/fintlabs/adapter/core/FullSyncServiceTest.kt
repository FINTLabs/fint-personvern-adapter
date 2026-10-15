package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.config.CapabilityProperties
import no.fintlabs.adapter.core.config.TenantProperties
import no.fintlabs.adapter.core.sync.FullSyncService
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.core.sync.SyncPageValidator
import no.fintlabs.adapter.models.sync.SyncType
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullSyncServiceTest {

    private val tenant = TenantProperties(listOf("rogfk.no"), "https://vigoiks.no/rogfk-no/personvern", "user", "pass", "client", "secret")
    private val properties = AdapterProperties(baseUrl = "http://gateway", pageSize = 3, tenants = listOf(tenant))
    private val client = TenantClient(tenant, WebClient.create())
    private val service = FullSyncService(properties, SyncPageValidator())

    private val source = object : ResourceSource<BehandlingResource> {
        override fun resources(orgId: String) = emptyList<BehandlingResource>()
    }
    private val binding = ResourceBindings.of(properties, listOf(source), emptyList()).single()

    private fun behandling(id: String) = BehandlingResource().apply { systemId = Identifikator().apply { identifikatorverdi = id } }

    @Test
    fun `splits resources into pages with consistent metadata`() {
        val resources = listOf("a", "b", "c", "d", "e", "f", "g").map(::behandling)

        @Suppress("UNCHECKED_CAST")
        val pages = service.pages(client, "rogfk.no", binding as ResourceBinding<BehandlingResource>, resources, SyncType.FULL)

        assertEquals(3, pages.size)
        assertEquals(listOf(3, 3, 1), pages.map { it.resources.size })
        assertEquals(listOf(1L, 2L, 3L), pages.map { it.metadata.page })
        assertTrue(pages.all { it.metadata.totalPages == 3L && it.metadata.totalSize == 7L })
        assertTrue(pages.all { it.metadata.orgId == "rogfk.no" && it.metadata.adapterId == tenant.id })
        assertTrue(pages.all { it.metadata.uriRef == "/personvern/samtykke/behandling" })
        assertEquals(1, pages.map { it.metadata.corrId }.toSet().size)
        assertEquals("g", pages.last().resources.single().identifier, "identifier defaults to the systemId value")
        assertTrue(SyncPageValidator().validate(pages, 7).none { it.contains("size") })
    }

    @Test
    fun `no resources means no pages`() {
        @Suppress("UNCHECKED_CAST")
        assertEquals(0, service.pages(client, "rogfk.no", binding as ResourceBinding<BehandlingResource>, emptyList(), SyncType.FULL).size)
    }

    @Test
    fun `full sync interval comes from the capability settings unless overridden`() {
        assertEquals(Duration.ofDays(1), binding.fullSyncInterval)
        assertEquals(Duration.ofDays(7), properties.copy(capabilities = mapOf("behandling" to CapabilityProperties(fullSyncIntervalInDays = 7))).let { ResourceBindings.of(it, listOf(source), emptyList()).single().fullSyncInterval })
        assertEquals(Duration.ofMinutes(5), properties.copy(fullSyncInterval = Duration.ofMinutes(5)).let { ResourceBindings.of(it, listOf(source), emptyList()).single().fullSyncInterval })
    }
}
