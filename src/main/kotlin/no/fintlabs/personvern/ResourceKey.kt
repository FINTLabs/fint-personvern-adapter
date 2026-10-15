package no.fintlabs.personvern

import jakarta.persistence.Embeddable
import java.io.Serializable

/**
 * Primary key of every stored resource: the tenant plus the resource's systemId. The same systemId can exist
 * for several orgs.
 */
@Embeddable
data class ResourceKey(
    var orgId: String = "",
    var id: String = "",
) : Serializable
