package no.fintlabs

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fintlabs.personvern.samtykke.behandling.BehandlingEntity
import spock.lang.Specification

class BehandlingEntitySpec extends Specification {

    def "test toEntity method"() {
        given: "A BehandlingResource object"
        BehandlingResource behandlingResource = new BehandlingResource()
        behandlingResource.setAktiv(true)
        behandlingResource.setFormal("Test formal")

        Identifikator systemId = new Identifikator()
        systemId.setIdentifikatorverdi("12345")
        behandlingResource.setSystemId(systemId)

        when: "Calling the toEntity method"
        BehandlingEntity behandling = BehandlingEntity.toEntity(behandlingResource, "fintlabs.no")

        then: "Properties of the Behandling object should match the properties of the BehandlingResource object"
        behandlingResource.aktiv == behandling.getResource().getAktiv()
        behandlingResource.formal == behandling.getResource().getFormal()
        behandlingResource.systemId.identifikatorverdi == behandling.getResource().getSystemId().getIdentifikatorverdi()
    }

}
