package no.fint.personvern.service

import no.fint.adapter.event.EventResponseService
import no.fint.adapter.event.EventStatusService
import no.fint.event.model.DefaultActions
import no.fint.event.model.Event
import spock.lang.Specification

class EventHandlerServiceSpec extends Specification {
    EventStatusService eventStatusService = Mock()
    EventResponseService eventResponseService = Mock()
    SamtykkeService samtykkeService = Mock()
    BehandlingService behandlingService = Mock()
    TjenesteService tjenesteService = Mock()
    BehandlingsgrunnlagService behandlingsgrunnlagService = Mock()
    PersonopplysningService personopplysningService = Mock()
    MongoService mongoService = Mock()

    EventHandlerService eventHandlerService

    void setup() {
        eventHandlerService = new EventHandlerService(eventResponseService, eventStatusService, samtykkeService, behandlingService, tjenesteService, behandlingsgrunnlagService, personopplysningService, mongoService)
    }

    def "Post response on health check"() {
        given:
        def event = new Event('rogfk.no', 'test', DefaultActions.HEALTH, 'test')
        def component = 'test'

        when:
        eventHandlerService.handleEvent(component, event)

        then:
        1 * eventResponseService.postResponse(component, _ as Event)
    }
}