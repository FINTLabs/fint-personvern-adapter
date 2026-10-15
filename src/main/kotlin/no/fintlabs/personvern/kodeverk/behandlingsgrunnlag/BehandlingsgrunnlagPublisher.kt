package no.fintlabs.personvern.kodeverk.behandlingsgrunnlag

import jakarta.annotation.PostConstruct
import no.fintlabs.adapter.config.AdapterProperties
import no.fintlabs.adapter.datasync.ResourcePublisher
import no.fintlabs.adapter.datasync.ResourceRepository
import no.fintlabs.adapter.datasync.SyncData
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.logger
import no.novari.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class BehandlingsgrunnlagPublisher(
    repository: BehandlingsgrunnlagRepository,
    adapterProperties: AdapterProperties,
) : ResourcePublisher<BehandlingsgrunnlagResource, ResourceRepository<BehandlingsgrunnlagResource>>(repository, adapterProperties) {

    private val log = logger<BehandlingsgrunnlagPublisher>()

    @PostConstruct
    @Scheduled(initialDelayString = "10000", fixedRateString = "500000")
    override fun doFullSync() {
        log.info("Start full sync for resource {}", getCapability().entityUri)
        submit(SyncData.ofPostData(repository.resources))
    }

    /** Delta sync is not in use; the adapter always sends full syncs. */
    override fun doDeltaSync() {
        log.info("Start delta sync for resource {}", getCapability().entityUri)
        submit(SyncData.ofPatchData(repository.updatedResources))
    }

    override fun getCapability(): AdapterCapability = adapterProperties.getCapabilityByResource("behandlingsgrunnlag")
}
