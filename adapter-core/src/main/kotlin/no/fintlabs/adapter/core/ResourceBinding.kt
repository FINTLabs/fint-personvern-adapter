package no.fintlabs.adapter.core

import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.config.CapabilityProperties
import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.EventCapability
import no.novari.fint.model.resource.FintResource
import java.time.Duration

/**
 * One resource the adapter serves: its source, optional event handler, and the capability registered for it.
 */
class ResourceBinding<T : FintResource>(
    val ref: ResourceRef,
    val source: ResourceSource<T>,
    val handler: EventHandler<T>?,
    val fullSyncInterval: Duration,
    val capability: AdapterCapability,
) {
    val resourceType: Class<T> get() = source.resourceType

    fun identifier(resource: T): String = source.identifier(resource)

    val eventCapability: EventCapability?
        get() =
            handler?.let {
                EventCapability
                    .builder()
                    .domainName(ref.domainName)
                    .packageName(ref.packageName)
                    .resourceName(ref.resourceName)
                    .operations(it.operations.toHashSet())
                    .build()
            }

    override fun toString(): String = ref.toString()
}

/**
 * Pairs every [ResourceSource] bean with its [EventHandler] and configuration, and fails startup on anything
 * inconsistent: a handler without a source, two sources for one resource, a resource the FINT model does not
 * know, or a capability override for a resource no bean serves.
 */
class ResourceBindings(
    val bindings: List<ResourceBinding<*>>,
) : Iterable<ResourceBinding<*>> by bindings {
    /** `<prefix>/<org with dots as dashes>/<domain(s) served>`, e.g. `https://vigoiks.no/novari-no/personvern`. */
    fun defaultAdapterId(
        prefix: String,
        orgId: String,
    ): String =
        "${prefix.trimEnd('/')}/${orgId.replace('.', '-')}/${
            bindings.map { it.ref.domainName }.distinct().sorted().joinToString("-")
        }"

    val capabilities: Set<AdapterCapability> get() = bindings.map { it.capability }.toHashSet()
    val eventCapabilities: Set<EventCapability> get() = bindings.mapNotNull { it.eventCapability }.toHashSet()

    companion object {
        fun of(
            properties: AdapterProperties,
            sources: List<ResourceSource<*>>,
            handlers: List<EventHandler<*>>,
        ): ResourceBindings {
            val sourceRefs = sources.map { it.ref }
            val duplicates =
                sourceRefs
                    .groupingBy { it }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
            require(duplicates.isEmpty()) { "More than one ResourceSource bean for: $duplicates" }

            val unknown = (sourceRefs + handlers.map { it.ref }).filterNot { it.isKnown }.toSet()
            require(unknown.isEmpty()) { "Resources the FINT information model does not know: $unknown" }

            val orphans = handlers.map { it.ref }.filterNot { it in sourceRefs }
            require(orphans.isEmpty()) { "EventHandler beans without a ResourceSource for the same resource: $orphans" }

            val names = sourceRefs.map { it.resourceName }
            val unmatched = properties.capabilities.keys.filterNot { it in names }
            require(unmatched.isEmpty()) { "fint.adapter.capabilities entries for resources no bean serves: $unmatched" }

            val bindings = sources.map { source -> bind(properties, source, handlers) }
            return ResourceBindings(bindings)
        }

        private fun <T : FintResource> bind(
            properties: AdapterProperties,
            source: ResourceSource<T>,
            handlers: List<EventHandler<*>>,
        ): ResourceBinding<T> {
            val ref = source.ref
            val matching = handlers.filter { it.ref == ref }
            require(matching.size <= 1) { "More than one EventHandler bean for $ref" }
            @Suppress("UNCHECKED_CAST")
            val handler = matching.firstOrNull() as EventHandler<T>?
            val settings = properties.capabilities[ref.resourceName] ?: CapabilityProperties()
            val capability =
                AdapterCapability
                    .builder()
                    .domainName(ref.domainName)
                    .packageName(ref.packageName)
                    .resourceName(ref.resourceName)
                    .fullSyncIntervalInDays(settings.fullSyncIntervalInDays)
                    .deltaSyncInterval(settings.deltaSyncInterval)
                    .build()
            return ResourceBinding(ref, source, handler, properties.fullSyncIntervalFor(settings), capability)
        }
    }
}
