package no.fint.personvern.handler.kodeverk

import no.fint.event.model.Event
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource
import spock.lang.Specification

class BehandlingsgrunnlagHandlerSpec extends Specification {

    BehandlingsgrunnlagHandler handler = new BehandlingsgrunnlagHandler()

    def "Returns BehandlingsgrunnlagResource"() {
        given:
        def event = new Event<FintLinks>(orgId: 'test.no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 6
        def data = event.data.first() as BehandlingsgrunnlagResource
        data.systemId
        data.navn
        data.gyldighetsperiode
        !data.passiv
    }
}
