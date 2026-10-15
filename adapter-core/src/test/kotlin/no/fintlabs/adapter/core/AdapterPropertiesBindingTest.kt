package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.AdapterProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import kotlin.test.assertEquals

/**
 * Pins down how the tenant list binds, because deployments supply it through environment variables.
 */
class AdapterPropertiesBindingTest {

    private val defaults = mapOf(
        "fint.adapter.base-url" to "http://gateway",
        "fint.adapter.capabilities.behandling.domain-name" to "personvern",
        "fint.adapter.capabilities.behandling.package-name" to "samtykke",
        "fint.adapter.capabilities.behandling.resource-name" to "behandling",
        "fint.adapter.capabilities.behandling.full-sync-interval-in-days" to "1",
        "fint.adapter.capabilities.behandling.delta-sync-interval" to "IMMEDIATE",
        "fint.adapter.heartbeat-interval" to "2m",
        "fint.adapter.tenants[0].org-id" to "fintlabs.no",
        "fint.adapter.tenants[0].id" to "https://vigoiks.no/fintlabs-no/personvern",
        "fint.adapter.tenants[0].username" to "default-user",
        "fint.adapter.tenants[0].password" to "pw",
        "fint.adapter.tenants[0].client-id" to "c",
        "fint.adapter.tenants[0].client-secret" to "s",
    )

    private fun bind(vararg environment: Pair<String, String>): AdapterProperties {
        val env = StandardEnvironment()
        // Boot applies environment-variable name mapping only to sources named like the real one.
        env.propertySources.addFirst(SystemEnvironmentPropertySource("test-systemEnvironment", environment.toMap()))
        env.propertySources.addLast(MapPropertySource("defaults", defaults))
        return Binder.get(env).bind("fint.adapter", Bindable.of(AdapterProperties::class.java)).get()
    }

    @Test
    fun `durations and the default tenant bind from relaxed names`() {
        val properties = bind()

        assertEquals(2, properties.heartbeatIntervalInMinutes)
        assertEquals(listOf("fintlabs.no"), properties.tenants.map { it.orgId })
        assertEquals("/personvern/samtykke/behandling", properties.capability("behandling").entityUri)
    }

    @Test
    fun `a tenant list in the environment replaces the default list entirely`() {
        // Dashes are dropped in environment variable names: org-id becomes ORGID.
        val properties = bind(
            "FINT_ADAPTER_TENANTS_0_ORGID" to "rogfk.no",
            "FINT_ADAPTER_TENANTS_0_ID" to "https://vigoiks.no/rogfk-no/personvern",
            "FINT_ADAPTER_TENANTS_0_USERNAME" to "rogfk-user",
            "FINT_ADAPTER_TENANTS_0_PASSWORD" to "pw",
            "FINT_ADAPTER_TENANTS_0_CLIENTID" to "c",
            "FINT_ADAPTER_TENANTS_0_CLIENTSECRET" to "s",
            "FINT_ADAPTER_TENANTS_1_ORGID" to "agderfk.no",
            "FINT_ADAPTER_TENANTS_1_ID" to "https://vigoiks.no/agderfk-no/personvern",
            "FINT_ADAPTER_TENANTS_1_USERNAME" to "agderfk-user",
            "FINT_ADAPTER_TENANTS_1_PASSWORD" to "pw",
            "FINT_ADAPTER_TENANTS_1_CLIENTID" to "c",
            "FINT_ADAPTER_TENANTS_1_CLIENTSECRET" to "s",
        )

        assertEquals(listOf("rogfk.no", "agderfk.no"), properties.tenants.map { it.orgId })
        assertEquals(listOf("rogfk-user", "agderfk-user"), properties.tenants.map { it.username })
    }
}
