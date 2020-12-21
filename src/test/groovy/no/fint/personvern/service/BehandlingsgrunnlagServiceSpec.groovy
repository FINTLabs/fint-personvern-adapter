package no.fint.personvern.service

import no.fint.event.model.Event
import no.fint.model.resource.FintLinks
import spock.lang.Specification

class BehandlingsgrunnlagServiceSpec extends Specification {

    BehandlingsgrunnlagService behandlingsgrunnlagService = new BehandlingsgrunnlagService()

    def "handleGetAllBehandlingsgrunnlag adds all BehandlingsgrunnlagResource to Event"() {
        given:
        def event = newBehandlingsgrunnlagEvent('test.no', [])

        when:
        behandlingsgrunnlagService.getAllBehandlingsgrunnlag(event)

        then:
        event.data.size() == 6
    }

    def newBehandlingsgrunnlagEvent(String orgId, List<FintLinks> data) {
        return new Event<FintLinks>(
                orgId: orgId,
                data: data
        )
    }
}