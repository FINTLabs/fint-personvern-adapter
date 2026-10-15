package no.fintlabs.adapter.core.config

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import no.fintlabs.adapter.models.AdapterCapability
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * Everything the adapter runtime needs, bound from `fint.adapter`.
 *
 * Capabilities are shared by all tenants: every tenant registers the same contract and is synced for the same
 * resources. Timing comes from here, not from annotations in the adapter: full sync follows each capability's
 * `fullSyncIntervalInDays`, heartbeat and event polling follow the intervals below.
 */
@Validated
@ConfigurationProperties("fint.adapter")
data class AdapterProperties(
    /** Base URL of the FINT adapter gateway, e.g. `https://api.felleskomponent.no`. */
    @field:NotBlank
    val baseUrl: String,

    /** OAuth2 token endpoint used for the password grant. */
    @field:NotBlank
    val tokenUri: String = "https://idp.felleskomponent.no/nidp/oauth/nam/token",

    /** Number of resources per sync page. */
    @field:Min(1)
    val pageSize: Int = 100,

    val heartbeatInterval: Duration = Duration.ofMinutes(1),

    val eventPollInterval: Duration = Duration.ofSeconds(1),

    /** Delay from startup to the first full sync. */
    val fullSyncInitialDelay: Duration = Duration.ofSeconds(10),

    /** When set, overrides `fullSyncIntervalInDays` of every capability. Meant for local development. */
    val fullSyncInterval: Duration? = null,

    /** Validate sync pages before sending them and log any problems. */
    val debug: Boolean = false,

    /** Threads shared by heartbeat, full sync and event polling. */
    @field:Min(1)
    val schedulerPoolSize: Int = 8,

    /** Resources this adapter delivers, keyed by resource name. Every key needs a [no.fintlabs.adapter.core.sync.ResourceSource] bean. */
    @field:NotEmpty
    val capabilities: Map<String, AdapterCapability> = emptyMap(),

    /** One entry per adapter credential. Each tenant registers its own contract with the gateway. */
    @field:NotEmpty
    val tenants: List<@Valid TenantProperties> = emptyList(),
) {
    val heartbeatIntervalInMinutes: Int
        get() = heartbeatInterval.toMinutes().toInt().coerceAtLeast(1)

    val enabledTenants: List<TenantProperties>
        get() = tenants.filter { it.enabled }

    fun capability(resourceName: String): AdapterCapability =
        capabilities[resourceName] ?: throw IllegalStateException("No capability configured for resource '$resourceName' under fint.adapter.capabilities")

    fun fullSyncIntervalFor(capability: AdapterCapability): Duration =
        fullSyncInterval ?: Duration.ofDays(capability.fullSyncIntervalInDays.toLong().coerceAtLeast(1))
}

data class TenantProperties(
    /** The org this credential serves, e.g. `rogfk.no`. Goes into contracts, sync pages and event answers. */
    @field:NotBlank
    val orgId: String,

    /** Adapter id registered with the gateway, e.g. `https://vigoiks.no/rogfk-no/personvern`. */
    @field:NotBlank
    val id: String,

    @field:NotBlank
    val username: String,

    @field:NotBlank
    val password: String,

    @field:NotBlank
    val clientId: String,

    @field:NotBlank
    val clientSecret: String,

    val scope: String = "fint-adapter",

    /** A disabled tenant is kept in config but neither registered nor synced. */
    val enabled: Boolean = true,
) {
    override fun toString(): String = "Tenant(orgId=$orgId, id=$id, username=$username)"
}
