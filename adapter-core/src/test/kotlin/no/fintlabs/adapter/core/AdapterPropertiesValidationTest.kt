package no.fintlabs.adapter.core

import jakarta.validation.Validation
import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.config.TenantProperties
import no.fintlabs.adapter.models.AdapterCapability
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdapterPropertiesValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator
    private val capability = AdapterCapability("personvern", "samtykke", "behandling", 1, AdapterCapability.DeltaSyncInterval.IMMEDIATE)
    private val tenant = TenantProperties(listOf("rogfk.no"), "https://vigoiks.no/rogfk-no/personvern", "user", "pass", "client", "secret")

    @Test
    fun `a complete configuration is valid`() {
        val properties = AdapterProperties(baseUrl = "http://gateway", capabilities = mapOf("behandling" to capability), tenants = listOf(tenant))

        assertTrue(validator.validate(properties).isEmpty())
    }

    @Test
    fun `tenants are validated one by one`() {
        val properties = AdapterProperties(baseUrl = "http://gateway", capabilities = mapOf("behandling" to capability), tenants = listOf(tenant, tenant.copy(username = " ")))

        val violations = validator.validate(properties)

        assertEquals(listOf("tenants[1].username"), violations.map { it.propertyPath.toString() })
    }

    @Test
    fun `a tenant needs at least one org`() {
        val violations = validator.validate(AdapterProperties(baseUrl = "http://gateway", capabilities = mapOf("behandling" to capability), tenants = listOf(tenant.copy(orgs = emptyList()))))

        assertEquals(listOf("tenants[0].orgs"), violations.map { it.propertyPath.toString() })
    }

    @Test
    fun `capabilities and tenants are required`() {
        val violations = validator.validate(AdapterProperties(baseUrl = "http://gateway"))

        assertEquals(setOf("capabilities", "tenants"), violations.map { it.propertyPath.toString() }.toSet())
    }

    @Test
    fun `secrets stay out of toString`() {
        assertEquals("Tenant(orgs=[rogfk.no], id=https://vigoiks.no/rogfk-no/personvern, username=user)", tenant.toString())
    }
}
