package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.TenantProperties
import org.springframework.web.reactive.function.client.WebClient

/**
 * One tenant and the gateway client that authenticates as it.
 */
class TenantClient(
    val tenant: TenantProperties,
    val webClient: WebClient,
) {
    @Volatile
    var registered: Boolean = false

    val orgId: String get() = tenant.orgId
    val adapterId: String get() = tenant.id
    val username: String get() = tenant.username

    override fun toString(): String = tenant.toString()
}

class TenantRegistry(
    val tenants: List<TenantClient>,
) : Iterable<TenantClient> by tenants {
    fun byOrgId(orgId: String): TenantClient? = tenants.firstOrNull { it.orgId == orgId }
}
