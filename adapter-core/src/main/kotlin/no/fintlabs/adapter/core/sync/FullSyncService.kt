package no.fintlabs.adapter.core.sync

import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.config.AdapterProperties
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.adapter.models.sync.SyncType
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Pushes everything a [ResourceSource] has for one tenant to the gateway as a full sync.
 */
class FullSyncService(
    private val properties: AdapterProperties,
    private val validator: SyncPageValidator,
) {
    private val log = logger<FullSyncService>()

    fun <T : Any> sync(client: TenantClient, source: ResourceSource<T>) {
        val capability = properties.capability(source.resourceName)
        val resources = source.resources(client.orgId)
        val pages = pages(client, capability, source, resources, SyncType.FULL)
        if (properties.debug) validator.validate(pages, resources.size)

        log.info("Syncing {} {} for {} in {} pages", resources.size, capability.entityUri, client.orgId, pages.size)
        val start = Instant.now()
        Flux.fromIterable(pages)
            .concatMap { page -> send(client, capability, page) }
            .blockLast()
        log.info("Synced {} {} for {} in {}", resources.size, capability.entityUri, client.orgId, format(Duration.between(start, Instant.now())))
    }

    fun <T : Any> pages(
        client: TenantClient,
        capability: AdapterCapability,
        definition: ResourceDefinition<T>,
        resources: List<T>,
        syncType: SyncType,
    ): List<SyncPage> {
        val corrId = UUID.randomUUID().toString()
        val chunks = resources.chunked(properties.pageSize)
        return chunks.mapIndexed { index, chunk ->
            val entries = chunk.map { SyncPageEntry.of(definition.identifier(it), it) }
            val metadata = SyncPageMetadata.builder()
                .orgId(client.orgId)
                .adapterId(client.adapterId)
                .corrId(corrId)
                .totalPages(chunks.size.toLong())
                .totalSize(resources.size.toLong())
                .pageSize(entries.size.toLong())
                .page(index + 1L)
                .uriRef(capability.entityUri)
                .time(System.currentTimeMillis())
                .build()
            SyncPage.builder().metadata(metadata).resources(entries).syncType(syncType).build()
        }
    }

    private fun send(client: TenantClient, capability: AdapterCapability, page: SyncPage): Mono<ResponseEntity<Void>> =
        client.webClient
            .method(page.syncType.httpMethod)
            .uri("/provider${capability.entityUri}")
            .bodyValue(page)
            .retrieve()
            .toBodilessEntity()
            .doOnNext { log.info("Page {}/{} of {} for {} returned {}", page.metadata.page, page.metadata.totalPages, capability.entityUri, client.orgId, it.statusCode) }
            .doOnError { log.error("Page {}/{} of {} for {} failed: {}", page.metadata.page, page.metadata.totalPages, capability.entityUri, client.orgId, it.describe()) }

    private fun format(duration: Duration) = "%02d:%02d:%02d".format(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart())
}
