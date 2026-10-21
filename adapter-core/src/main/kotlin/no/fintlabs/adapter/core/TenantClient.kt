package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.TenantProperties
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.ConcurrentHashMap

/**
 * One credential, the orgs it serves, and the gateway client that authenticates as it.
 */
class TenantClient(
    /** The key under `fint.adapter.tenants`, for logs. */
    val name: String,
    val tenant: TenantProperties,
    val webClient: WebClient,
    /** The adapter id registered with the gateway; configured, or derived from the first org and the served domain. */
    val adapterId: String,
) {
    private val registeredOrgs: MutableSet<String> = ConcurrentHashMap.newKeySet()

    val orgs: List<String> get() = tenant.orgs
    val username: String get() = tenant.username

    fun isRegistered(orgId: String): Boolean = orgId in registeredOrgs

    fun setRegistered(
        orgId: String,
        registered: Boolean,
    ) {
        if (registered) registeredOrgs += orgId else registeredOrgs -= orgId
    }

    override fun toString(): String = "$name${tenant.orgs}"
}

class TenantRegistry(
    val tenants: List<TenantClient>,
) : Iterable<TenantClient> by tenants {
    fun byOrgId(orgId: String): TenantClient? = tenants.firstOrNull { orgId in it.orgs }
}
