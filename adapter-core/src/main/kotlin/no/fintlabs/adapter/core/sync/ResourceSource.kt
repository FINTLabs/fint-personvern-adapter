package no.fintlabs.adapter.core.sync

/**
 * What the runtime needs to know about one resource, shared by [ResourceSource] and
 * [no.fintlabs.adapter.core.event.EventHandler].
 */
interface ResourceDefinition<T : Any> {
    /** Key into `fint.adapter.capabilities`. */
    val resourceName: String

    val resourceType: Class<T>

    /** The identifier a sync page entry or event answer is keyed by, normally the systemId value. */
    fun identifier(resource: T): String
}

/**
 * Where full-sync data for one resource comes from. Implement one bean per configured capability.
 */
interface ResourceSource<T : Any> : ResourceDefinition<T> {
    fun resources(orgId: String): List<T>
}
