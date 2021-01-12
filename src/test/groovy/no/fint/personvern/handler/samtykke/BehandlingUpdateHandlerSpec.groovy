package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.event.model.Operation
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.repository.WrapperDocument
import no.fint.personvern.repository.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

@DataMongoTest
class BehandlingUpdateHandlerSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    BehandlingUpdateHandler handler

    void setup() {
        handler = new BehandlingUpdateHandler(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given create event a new BehandlingResource is created"() {
        given:
        def event = newBehandlingEvent('test.no', [newBehandlingResource(true)], null, Operation.CREATE)

        when:
        handler.accept(event)

        then:
        def resources = repository.findByOrgIdAndType('test.no', BehandlingResource.canonicalName)
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

    def "Given update event a BehandlingResource is updated"() {
        given:
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(BehandlingResource.canonicalName).value(newBehandlingResource(true)).build())

        def event = newBehandlingEvent('test.no', [newBehandlingResource(false)], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        def resources = repository.findByOrgIdAndType('test.no', BehandlingResource.canonicalName)
        resources.size() == 1
        def mongo = resources.first().value as BehandlingResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.formal == 'formal'
        !mongo.aktiv

        event.data.size() == 1
        def data = event.data.first() as BehandlingResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.formal == 'formal'
        !data.aktiv
    }

    def "Given update event on non-existent BehandlingsResource exception is thrown"() {
        given:
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(BehandlingResource.canonicalName).value(newBehandlingResource(true)).build())

        def event = newBehandlingEvent('test.no', [newBehandlingResource(false)], 'systemid/id-2', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        thrown(MongoCantFindDocumentException)
    }

    def newBehandlingEvent(String orgId, List<FintLinks> data, String query, Operation operation) {
        return new Event<FintLinks>(
                orgId: orgId,
                data: data,
                query: query,
                operation: operation
        )
    }

    def newBehandlingResource(boolean aktiv) {
        return new BehandlingResource(
                aktiv: aktiv,
                formal: 'formal',
                systemId: new Identifikator(identifikatorverdi: 'id')
        )
    }
}