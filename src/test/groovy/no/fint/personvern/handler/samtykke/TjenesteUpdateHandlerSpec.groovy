package no.fint.personvern.handler.samtykke

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.event.model.Event
import no.fint.event.model.Operation
import no.fint.event.model.ResponseStatus
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.TjenesteResource
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.repository.WrapperDocument
import no.fint.personvern.repository.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

@DataMongoTest
class TjenesteUpdateHandlerSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    TjenesteUpdateHandler handler

    void setup() {
        handler = new TjenesteUpdateHandler(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given create event a new TjenesteResource is created"() {
        given:
        def event = newTjenesteEvent('test.no', [newTjenesteResource('navn')], null, Operation.CREATE)

        when:
        handler.accept(event)

        then:
        def resources = repository.findByOrgIdAndType('test.no', TjenesteResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, TjenesteResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'navn'

        event.data.size() == 1
        def data = event.data.first() as TjenesteResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.navn == 'navn'
    }

    def "Given update event a TjenesteResource is updated"() {
        given:
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(TjenesteResource.canonicalName).value(newTjenesteResource('navn-1')).build())

        def event = newTjenesteEvent('test.no', [newTjenesteResource('navn-2')], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        def resources = repository.findByOrgIdAndType('test.no', TjenesteResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, TjenesteResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'navn-2'

        event.data.size() == 1
        def data = event.data.first() as TjenesteResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.navn == 'navn-2'
    }

    def "Given invalid update event error is returned"() {
        given:
        def resource = newTjenesteResource('navn')

        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(TjenesteResource.canonicalName).value(resource).build())

        resource.setSystemId(new Identifikator(identifikatorverdi: '123'))

        def event = newTjenesteEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        def resources = repository.findByOrgIdAndType('test.no', TjenesteResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, TjenesteResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'navn'

        event.data.size() == 0
        event.responseStatus == ResponseStatus.REJECTED
    }

    def "Given update event on non-existent TjenesteResource exception is thrown"() {
        given:
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(TjenesteResource.canonicalName).value(newTjenesteResource('navn')).build())

        def event = newTjenesteEvent('test.no', [newTjenesteResource('navn')], 'systemid/id-2', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        thrown(MongoCantFindDocumentException)
    }


    def newTjenesteEvent(String orgId, List<FintLinks> data, String query, Operation operation) {
        return new Event<FintLinks>(
                orgId: orgId,
                data: data,
                query: query,
                operation: operation
        )
    }

    def newTjenesteResource(String navn) {
        return new TjenesteResource(
                navn: navn,
                systemId: new Identifikator(identifikatorverdi: 'id')
        )
    }
}