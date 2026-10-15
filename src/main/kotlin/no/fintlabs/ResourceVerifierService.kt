package no.fintlabs

import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.springframework.stereotype.Service

/**
 * Checks that an incoming resource carries the fields the adapter needs before it is persisted.
 */
@Service
class ResourceVerifierService {

    fun verifySamtykkeResource(resource: SamtykkeResource?): Boolean =
        resource != null &&
            resource.gyldighetsperiode?.start != null &&
            resource.opprettet != null &&
            hasSystemId(resource.systemId) &&
            hasLinks(listOf(resource.behandling, resource.person))

    fun verifyBehandlingResource(resource: BehandlingResource?): Boolean =
        resource != null &&
            resource.aktiv != null &&
            !resource.formal.isNullOrBlank() &&
            hasSystemId(resource.systemId) &&
            hasLinks(listOf(resource.behandlingsgrunnlag, resource.tjeneste, resource.personopplysning))

    fun verifyTjenesteResource(resource: TjenesteResource?): Boolean =
        resource != null &&
            !resource.navn.isNullOrBlank() &&
            hasSystemId(resource.systemId)

    private fun hasLinks(relations: List<List<Link>?>): Boolean =
        relations.isNotEmpty() && relations.all { links -> links != null && links.any { !it?.href.isNullOrBlank() } }

    private fun hasSystemId(systemId: Identifikator?): Boolean = !systemId?.identifikatorverdi.isNullOrBlank()
}
