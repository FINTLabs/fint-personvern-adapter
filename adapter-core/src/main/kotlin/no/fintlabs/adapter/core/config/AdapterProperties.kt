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
 * The resources served come from the ResourceSource and EventHandler beans, not from here; `capabilities` only
 * tunes a resource's sync interval. Every tenant registers the same contract and is synced for the same
 * resources. Timing comes from here, not from annotations in the adapter.
 */
@Validated
@ConfigurationProperties("fint.adapter")
data class AdapterProperties(
    /** Base URL of the FINT adapter gateway, e.g. `https://api.felleskomponent.no`. */
    @field:NotBlank
    val baseUrl: String,

    /** OAuth2 token endpoint used for the password grant. */
    @field:NotBlank
    val tokenUri: String = "https://idp.felleskomponent.no/nidp/oauth/nam/token?resourceServer=fint-api",

    /** Number of resources per sync page. */
    @field:Min(1)
    val pageSize: Int = 100,

    val heartbeatInterval: Duration = Duration.ofMinutes(1),

    val eventPollInterval: Duration = Duration.ofSeconds(15),

    /** Delay from startup to the first full sync. */
    val fullSyncInitialDelay: Duration = Duration.ofSeconds(10),

    /** When set, overrides `fullSyncIntervalInDays` of every capability. Meant for local development. */
    val fullSyncInterval: Duration? = null,

    /** Validate sync pages before sending them and log any problems. */
    val debug: Boolean = false,

    /** Threads shared by heartbeat, full sync and event polling. */
    @field:Min(1)
    val schedulerPoolSize: Int = 8,

    /** Optional per-resource settings keyed by resource name, e.g. `behandling`. Each key must match a ResourceSource bean. */
    val capabilities: Map<String, @Valid CapabilityProperties> = emptyMap(),

    /** Prefix of a tenant's default adapter id: `<id-prefix>/<first org, dots as dashes>/<domain>`. */
    @field:NotBlank
    val idPrefix: String = "https://vigoiks.no",

    /** One entry per adapter credential, keyed by a free name used in logs (e.g. `novari`), each serving one or more orgs. */
    @field:NotEmpty
    val tenants: Map<String, @Valid TenantProperties> = emptyMap(),
) {
    val heartbeatIntervalInMinutes: Int
        get() = heartbeatInterval.toMinutes().toInt().coerceAtLeast(1)

    val enabledTenants: Map<String, TenantProperties>
        get() = tenants.filterValues { it.enabled }

    /** Every org served, across tenants; duplicates mean two credentials would register for the same org. */
    val orgs: List<String>
        get() = enabledTenants.values.flatMap { it.orgs }

    fun fullSyncIntervalFor(settings: CapabilityProperties): Duration =
        fullSyncInterval ?: Duration.ofDays(settings.fullSyncIntervalInDays.toLong())
}

/**
 * Optional per-resource settings under `fint.adapter.capabilities.<resource-name>`.
 */
data class CapabilityProperties(
    /** How often the resource is full-synced, and what the contract promises. */
    @field:Min(1)
    val fullSyncIntervalInDays: Int = 1,

    /** Reported in the contract only; delta sync is not implemented. */
    val deltaSyncInterval: AdapterCapability.DeltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE,
)

data class TenantProperties(
    /** Orgs this credential serves, e.g. `[novari.no, side.novari.no]`. Each gets its own contract and sync. */
    @field:NotEmpty
    val orgs: List<@NotBlank String>,

    /** Adapter id registered with the gateway. Defaults to `<id-prefix>/<first org>/<domain>`, e.g. `https://vigoiks.no/novari-no/personvern`. */
    val id: String? = null,

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
    override fun toString(): String = "Tenant(orgs=$orgs, username=$username)"
}
