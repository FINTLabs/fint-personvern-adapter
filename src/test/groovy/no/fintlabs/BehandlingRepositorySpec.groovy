package no.fintlabs

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fintlabs.adapter.models.RequestFintEvent
import no.fintlabs.personvern.samtykke.behandling.BehandlingJpaRepository
import no.fintlabs.personvern.samtykke.behandling.BehandlingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration
@DataJpaTest
class BehandlingRepositorySpec extends Specification {

    @Autowired
    BehandlingJpaRepository behandlingJpaRepository

    def setup() {
        // Clean up the repository before each test
        behandlingJpaRepository.deleteAll()
    }

    def "test saveResources method"() {
        given: "a BehandlingResource and a RequestFintEvent"
        BehandlingResource behandlingResource = new BehandlingResource()
        RequestFintEvent requestFintEvent = new RequestFintEvent()

        requestFintEvent.setOrgId("fintlabs.no")
        behandlingResource.setAktiv(true)
        behandlingResource.setFormal("Test formal")

        Identifikator systemId = new Identifikator()
        systemId.setIdentifikatorverdi("12345")
        behandlingResource.setSystemId(systemId)

        and: "a BehandlingRepository instance"
        def behandlingRepository = new BehandlingRepository(behandlingJpaRepository)

        when: "saveResources is called"
        def savedResource = behandlingRepository.saveResources(behandlingResource, requestFintEvent)

        then: "the resulting BehandlingResource should have the same values"
        savedResource == behandlingResource
    }

}
