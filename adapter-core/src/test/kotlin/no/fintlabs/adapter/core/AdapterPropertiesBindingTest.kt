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
        "fint.adapter.capabilities.behandling.full-sync-interval-in-days" to "7",
        "fint.adapter.heartbeat-interval" to "2m",
    )

    private val yamlTenant = mapOf(
        "fint.adapter.tenants.default.orgs" to "fintlabs.no",
        "fint.adapter.tenants.default.username" to "default-user",
        "fint.adapter.tenants.default.password" to "pw",
        "fint.adapter.tenants.default.client-id" to "c",
        "fint.adapter.tenants.default.client-secret" to "s",
    )

    private fun bind(vararg environment: Pair<String, String>, yaml: Map<String, String> = emptyMap()): AdapterProperties {
        val env = StandardEnvironment()
        // Boot applies environment-variable name mapping only to sources named like the real one.
        env.propertySources.addFirst(SystemEnvironmentPropertySource("test-systemEnvironment", environment.toMap()))
        env.propertySources.addLast(MapPropertySource("defaults", defaults + yaml))
        return Binder.get(env).bind("fint.adapter", Bindable.of(AdapterProperties::class.java)).get()
    }

    @Test
    fun `durations and a tenant in yaml bind from relaxed names`() {
        val properties = bind(yaml = yamlTenant)

        assertEquals(2, properties.heartbeatIntervalInMinutes)
        assertEquals(mapOf("default" to listOf("fintlabs.no")), properties.tenants.mapValues { it.value.orgs })
        assertEquals(7, properties.capabilities.getValue("behandling").fullSyncIntervalInDays)
    }

    @Test
    fun `tenants in the environment bind by name`() {
        // Dashes are dropped in environment variable names (client-id becomes CLIENTID); orgs bind from a comma-separated value.
        val properties = bind(
            "FINT_ADAPTER_TENANTS_NOVARI_ORGS" to "novari.no,side.novari.no,test.novari.no",
            "FINT_ADAPTER_TENANTS_NOVARI_USERNAME" to "novari-user",
            "FINT_ADAPTER_TENANTS_NOVARI_PASSWORD" to "pw",
            "FINT_ADAPTER_TENANTS_NOVARI_CLIENTID" to "c",
            "FINT_ADAPTER_TENANTS_NOVARI_CLIENTSECRET" to "s",
            "FINT_ADAPTER_TENANTS_AGDERFK_ORGS" to "agderfk.no",
            "FINT_ADAPTER_TENANTS_AGDERFK_USERNAME" to "agderfk-user",
            "FINT_ADAPTER_TENANTS_AGDERFK_PASSWORD" to "pw",
            "FINT_ADAPTER_TENANTS_AGDERFK_CLIENTID" to "c",
            "FINT_ADAPTER_TENANTS_AGDERFK_CLIENTSECRET" to "s",
        )

        assertEquals(setOf("novari", "agderfk"), properties.tenants.keys, "map keys from the environment are lowercased")
        assertEquals(listOf("novari.no", "side.novari.no", "test.novari.no"), properties.tenants.getValue("novari").orgs)
        assertEquals(listOf("novari-user", "agderfk-user"), properties.tenants.values.map { it.username })
        assertEquals(listOf("novari.no", "side.novari.no", "test.novari.no", "agderfk.no"), properties.orgs)
    }

    @Test
    fun `tenants in the environment also bind from dotted property names`() {
        // The form the 1Password item uses: the same style as fint.core.oauth2.client-id in existing secrets.
        val properties = bind(
            "fint.adapter.tenants.rogfk.orgs" to "rogfk.no",
            "fint.adapter.tenants.rogfk.username" to "rogfk-user",
            "fint.adapter.tenants.rogfk.password" to "pw",
            "fint.adapter.tenants.rogfk.client-id" to "c",
            "fint.adapter.tenants.rogfk.client-secret" to "s",
            "fint.adapter.tenants.trondelagfylke.orgs" to "trondelagfylke.no",
            "fint.adapter.tenants.trondelagfylke.username" to "trondelag-user",
            "fint.adapter.tenants.trondelagfylke.password" to "pw",
            "fint.adapter.tenants.trondelagfylke.client-id" to "c",
            "fint.adapter.tenants.trondelagfylke.client-secret" to "s",
        )

        assertEquals(setOf("rogfk", "trondelagfylke"), properties.tenants.keys)
        assertEquals("c", properties.tenants.getValue("rogfk").clientId)
        assertEquals(listOf("trondelagfylke.no"), properties.tenants.getValue("trondelagfylke").orgs)
    }

    @Test
    fun `a tenant in yaml is merged with, not replaced by, tenants from the environment`() {
        val properties = bind(
            "FINT_ADAPTER_TENANTS_NOVARI_ORGS" to "novari.no",
            "FINT_ADAPTER_TENANTS_NOVARI_USERNAME" to "novari-user",
            "FINT_ADAPTER_TENANTS_NOVARI_PASSWORD" to "pw",
            "FINT_ADAPTER_TENANTS_NOVARI_CLIENTID" to "c",
            "FINT_ADAPTER_TENANTS_NOVARI_CLIENTSECRET" to "s",
            yaml = yamlTenant,
        )

        assertEquals(setOf("default", "novari"), properties.tenants.keys, "which is why application.yml must not define a placeholder tenant")
    }
}
