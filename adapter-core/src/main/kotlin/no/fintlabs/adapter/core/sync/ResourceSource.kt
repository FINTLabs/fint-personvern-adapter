package no.fintlabs.adapter.core.sync

import no.fintlabs.adapter.core.ResourceRef
import no.novari.fint.model.resource.FintResource
import org.springframework.core.GenericTypeResolver

/**
 * What the runtime needs to know about one resource, shared by [ResourceSource] and
 * [no.fintlabs.adapter.core.event.EventHandler]. Everything has a default derived from the resource type:
 * the type itself from the generic parameter, the FINT reference (domain, package, name) from the model
 * class's package and name, and the identifier from the resource's `systemId`. Override only when a
 * resource breaks the convention, e.g. one identified by something other than systemId.
 */
interface ResourceDefinition<T : FintResource> {
    val resourceType: Class<T>
        get() {
            @Suppress("UNCHECKED_CAST")
            return GenericTypeResolver.resolveTypeArgument(javaClass, ResourceDefinition::class.java) as Class<T>?
                ?: throw IllegalStateException(
                    "${javaClass.name} must bind the type parameter of ResourceDefinition, or override resourceType",
                )
        }

    /** The FINT resource this bean serves, e.g. personvern/samtykke/behandling. */
    val ref: ResourceRef
        get() = ResourceRef.of(resourceType)

    /** The identifier a sync page entry or event answer is keyed by: the systemId value unless overridden. */
    fun identifier(resource: T): String =
        resource.identifikators
            ?.get(ID_FIELD)
            ?.identifikatorverdi
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException(
                "${resourceType.simpleName} has no $ID_FIELD; override identifier() for resources identified differently",
            )

    companion object {
        const val ID_FIELD = "systemId"
    }
}

/**
 * Where full-sync data for one resource comes from. One bean per resource the adapter delivers.
 */
interface ResourceSource<T : FintResource> : ResourceDefinition<T> {
    fun resources(orgId: String): List<T>
}
