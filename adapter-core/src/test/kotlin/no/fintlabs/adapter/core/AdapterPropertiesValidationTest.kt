package no.fintlabs.adapter.core

import jakarta.validation.Validation
import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.config.TenantProperties
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdapterPropertiesValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator
    private val tenant = TenantProperties(listOf("rogfk.no"), null, "user", "pass", "client", "secret")

    @Test
    fun `a complete configuration is valid`() {
        val properties = AdapterProperties(baseUrl = "http://gateway", tenants = mapOf("rogfk" to tenant))

        assertTrue(validator.validate(properties).isEmpty())
    }

    @Test
    fun `tenants are validated one by one`() {
        val properties = AdapterProperties(baseUrl = "http://gateway", tenants = mapOf("rogfk" to tenant, "broken" to tenant.copy(username = " ")))

        val violations = validator.validate(properties)

        assertEquals(listOf("tenants[broken].username"), violations.map { it.propertyPath.toString() })
    }

    @Test
    fun `a tenant needs at least one org`() {
        val violations = validator.validate(AdapterProperties(baseUrl = "http://gateway", tenants = mapOf("rogfk" to tenant.copy(orgs = emptyList()))))

        assertEquals(listOf("tenants[rogfk].orgs"), violations.map { it.propertyPath.toString() })
    }

    @Test
    fun `tenants are required`() {
        val violations = validator.validate(AdapterProperties(baseUrl = "http://gateway"))

        assertEquals(setOf("tenants"), violations.map { it.propertyPath.toString() }.toSet())
    }

    @Test
    fun `secrets stay out of toString`() {
        assertEquals("Tenant(orgs=[rogfk.no], username=user)", tenant.toString())
    }
}
