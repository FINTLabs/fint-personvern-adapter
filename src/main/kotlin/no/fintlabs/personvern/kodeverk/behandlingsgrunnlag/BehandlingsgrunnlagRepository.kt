package no.fintlabs.personvern.kodeverk.behandlingsgrunnlag

import no.fintlabs.adapter.datasync.ResourceRepository
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import no.novari.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Date

/**
 * Behandlingsgrunnlag is a fixed code list (GDPR article 6 legal bases), so it is served from code rather than a table.
 */
@Repository
class BehandlingsgrunnlagRepository : ResourceRepository<BehandlingsgrunnlagResource> {

    override fun getResources(): List<BehandlingsgrunnlagResource> = BEHANDLINGSGRUNNLAG.map { (kode, navn) ->
        BehandlingsgrunnlagResource().apply {
            systemId = Identifikator().apply { identifikatorverdi = kode }
            this.kode = kode
            this.navn = navn
            passiv = false
            gyldighetsperiode = Periode().apply { start = Date.from(VALID_FROM) }
        }
    }

    override fun getUpdatedResources(): List<BehandlingsgrunnlagResource> = emptyList()

    companion object {
        private val VALID_FROM: Instant = Instant.parse("2018-07-20T00:00:00.00Z")

        private val BEHANDLINGSGRUNNLAG = linkedMapOf(
            "a" to "Samtykke",
            "b" to "Nødvendig for å oppfylle en avtale",
            "c" to "Nødvendig for å oppfylle en rettslig plikt",
            "d" to "Nødvendig for å beskytte vitale interesser",
            "e" to "Nødvendig for å utføre en oppgave i offentlig interesse eller utøve offentlig myndighet",
            "f" to "Nødvendig for å ivareta legitime interesser - interesseavveiing",
        )
    }
}
