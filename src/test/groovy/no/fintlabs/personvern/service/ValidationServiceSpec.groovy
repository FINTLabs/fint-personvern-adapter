package no.fintlabs.personvern.service

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import spock.lang.Specification

class ValidationServiceSpec extends Specification {

    ValidationService service = new ValidationService()

    def "Given valid resource returns empty list"() {
        given:
        def resource = newBehandlingResource(true)

        when:
        def problems = service.getProblems(resource)

        then:
        problems.size() == 0
    }

    def "Given invalid resource returns list of problems"() {
        given:
        def resource = newBehandlingResource(true)
        resource.setFormal(null)

        when:
        def problems = service.getProblems(resource)

        then:
        problems.size() == 1
    }

    def newBehandlingResource(boolean aktiv) {
        return new BehandlingResource(
                aktiv: aktiv,
                formal: 'formal',
                systemId: new Identifikator(identifikatorverdi: 'id')
        )
    }
}
