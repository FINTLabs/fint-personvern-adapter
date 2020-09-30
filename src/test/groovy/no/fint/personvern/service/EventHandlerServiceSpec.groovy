package no.fint.personvern.service

import no.fint.adapter.event.EventResponseService
import no.fint.adapter.event.EventStatusService
import no.fint.event.model.DefaultActions
import no.fint.event.model.Event
import spock.lang.Specification

class EventHandlerServiceSpec extends Specification {
    private EventHandlerService eventHandlerService
    private EventStatusService eventStatusService
    private EventResponseService eventResponseService
    private KodeverkService kodeverkService;
    private SamtykkeService samtykkeService;
    private BehandlingService behandlingService;
    private TjenesteService tjenesteService;
    private MongoService mongoService;

    void setup() {
        eventStatusService = Mock(EventStatusService)
        eventResponseService = Mock(EventResponseService)
        kodeverkService = Mock(KodeverkService)
        samtykkeService = Mock(SamtykkeService)
        mongoService = Mock(MongoService)
        eventHandlerService = new EventHandlerService(eventResponseService, eventStatusService, kodeverkService, samtykkeService, behandlingService, tjenesteService, mongoService)
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
