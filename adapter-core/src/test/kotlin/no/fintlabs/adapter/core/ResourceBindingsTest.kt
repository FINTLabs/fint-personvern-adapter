package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.config.CapabilityProperties
import no.fintlabs.adapter.core.config.TenantProperties
import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.novari.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourceBindingsTest {

    private val properties = AdapterProperties(baseUrl = "http://gateway", tenants = listOf(TenantProperties(listOf("rogfk.no"), "id", "u", "p", "c", "s")))

    /** A bean that is both source and handler, declaring nothing but the type parameter. */
    class BehandlingAdapter : ResourceSource<BehandlingResource>, EventHandler<BehandlingResource> {
        override fun resources(orgId: String) = emptyList<BehandlingResource>()
        override fun handle(event: RequestFintEvent, resource: BehandlingResource) = EventResult.Saved(resource)
    }

    class BehandlingsgrunnlagSource : ResourceSource<BehandlingsgrunnlagResource> {
        override fun resources(orgId: String) = emptyList<BehandlingsgrunnlagResource>()
    }

    class NotGenerated : BehandlingResource()

    class NotGeneratedSource : ResourceSource<NotGenerated> {
        override fun resources(orgId: String) = emptyList<NotGenerated>()
    }

    class SamtykkeDeleter : ResourceSource<SamtykkeResource>, EventHandler<SamtykkeResource> {
        override val operations = setOf(OperationType.CREATE, OperationType.UPDATE, OperationType.DELETE)
        override fun resources(orgId: String) = emptyList<SamtykkeResource>()
        override fun handle(event: RequestFintEvent, resource: SamtykkeResource) = EventResult.Saved(resource)
    }

    @Test
    fun `ref, type and capabilities are derived from the resource class`() {
        val adapter = BehandlingAdapter()
        val bindings = ResourceBindings.of(properties, listOf(adapter, BehandlingsgrunnlagSource()), listOf(adapter))

        assertEquals(BehandlingResource::class.java, adapter.resourceType)
        assertEquals(ResourceRef("personvern", "samtykke", "behandling"), adapter.ref)
        assertEquals(listOf("personvern/samtykke/behandling", "personvern/kodeverk/behandlingsgrunnlag"), bindings.map { it.ref.toString() })

        val contractCapabilities = bindings.capabilities.map { it.entityUri }.toSet()
        assertEquals(setOf("/personvern/samtykke/behandling", "/personvern/kodeverk/behandlingsgrunnlag"), contractCapabilities)
        val eventCapability = bindings.eventCapabilities.single()
        assertEquals("behandling", eventCapability.resourceName)
        assertEquals(setOf(OperationType.CREATE, OperationType.UPDATE), eventCapability.operations)
        assertNull(bindings.single { it.ref.resourceName == "behandlingsgrunnlag" }.handler)
    }

    @Test
    fun `a handler can widen the operations it answers`() {
        val deleter = SamtykkeDeleter()
        val bindings = ResourceBindings.of(properties, listOf(deleter), listOf(deleter))

        assertTrue(OperationType.DELETE in bindings.eventCapabilities.single().operations)
    }

    @Test
    fun `capability settings must match a served resource`() {
        val adapter = BehandlingAdapter()
        val withTypo = properties.copy(capabilities = mapOf("behandlig" to CapabilityProperties(7)))

        val error = assertFailsWith<IllegalArgumentException> { ResourceBindings.of(withTypo, listOf(adapter), listOf(adapter)) }
        assertTrue(error.message!!.contains("behandlig"))
    }

    @Test
    fun `a handler needs a source for the same resource`() {
        assertFailsWith<IllegalArgumentException> { ResourceBindings.of(properties, listOf(BehandlingsgrunnlagSource()), listOf(BehandlingAdapter())) }
    }

    @Test
    fun `two sources for one resource are refused`() {
        assertFailsWith<IllegalArgumentException> { ResourceBindings.of(properties, listOf(BehandlingAdapter(), BehandlingAdapter()), emptyList()) }
    }

    @Test
    fun `a class outside the generated model needs an explicit ref`() {
        val error = assertFailsWith<IllegalArgumentException> { NotGeneratedSource().ref }
        assertTrue(error.message!!.contains("override ref"))
    }

    @Test
    fun `identifier needs a systemId`() {
        val error = assertFailsWith<IllegalArgumentException> { BehandlingAdapter().identifier(BehandlingResource()) }
        assertTrue(error.message!!.contains("systemId"))
    }
}
