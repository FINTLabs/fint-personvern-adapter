package no.fint.personvern.service

import no.fint.event.model.Event
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.wrapper.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

@DataMongoTest
class BehandlingServiceSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    BehandlingService behandlingService

    void setup() {
        behandlingService = new BehandlingService(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "When creating a new behandling the resource should be created"() {
        given:
        def event = newBehandlingEvent('test.no', [newBehandlingResource(true)], null)

        when:
        behandlingService.createBehandling(event)

        then:
        def resources = repository.findAll()
        resources.size() == 1
        def mongo = resources.first().value as BehandlingResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.formal == 'formal'
        mongo.aktiv

        event.data.size() == 1
        def data = event.data.first() as BehandlingResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.formal == 'formal'
        data.aktiv
    }

    def "When updating a behandling the resource should be updated"() {
        given:
        def createEvent = newBehandlingEvent('test.no', [newBehandlingResource(true)], null)
        behandlingService.createBehandling(createEvent)

        BehandlingResource behandlingResource = createEvent.data.first() as BehandlingResource
        def updateEvent = newBehandlingEvent('test.no', [newBehandlingResource(false)], 'systemid/' + behandlingResource.systemId.identifikatorverdi)

        when:
        behandlingService.updateBehandling(updateEvent)

        then:
        def resources = repository.findAll()
        resources.size() == 1
        def mongo = resources.first().value as BehandlingResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.formal == 'formal'
        !mongo.aktiv

        updateEvent.data.size() == 1
        def data = updateEvent.data.first() as BehandlingResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.formal == 'formal'
        !data.aktiv
    }

    def "When updating an object that does not exist and exception should be raised"() {
        given:
        def updateEvent = newBehandlingEvent('test.no', [newBehandlingResource(false)], 'systemid/id')

        when:
        behandlingService.updateBehandling(updateEvent)

        then:
        thrown(MongoCantFindDocumentException)
    }

    def "When get all Behandling the list should only contain Tjenester for the given orgId"() {
        given:
        def createEventOne = newBehandlingEvent('test1.no', [newBehandlingResource(true)], null)
        behandlingService.createBehandling(createEventOne)

        def createEventTwo = newBehandlingEvent('test2.no', [newBehandlingResource(true)], null)
        behandlingService.createBehandling(createEventTwo)

        def getEvent = newBehandlingEvent('test1.no', [], null)

        when:
        behandlingService.getAllBehandling(getEvent)

        then:
        def resources = repository.findAll()
        resources.size() == 2

        getEvent.data.size() == 1
        def resource = getEvent.data.first() as BehandlingResource
        resource.systemId.identifikatorverdi
        resource.formal == 'formal'
        resource.aktiv
    }

    def newBehandlingEvent(String orgId, List<FintLinks> data, String query) {
        return new Event<FintLinks>(
                orgId: orgId,
                data: data,
                query: query
        )
    }

    def newBehandlingResource(boolean aktiv) {
        return new BehandlingResource(
                aktiv: aktiv,
                formal: 'formal'
        )
    }
}