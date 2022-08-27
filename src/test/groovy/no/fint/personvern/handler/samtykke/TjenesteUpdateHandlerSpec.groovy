package no.fint.personvern.handler.samtykke

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.event.model.Event
import no.fint.event.model.Operation
import no.fint.event.model.Problem
import no.fint.event.model.ResponseStatus
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.TjenesteResource
import no.fint.personvern.exception.RowNotFoundException
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteRepository
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteUpdateHandler
import no.fint.personvern.service.ValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@DirtiesContext
class TjenesteUpdateHandlerSpec extends Specification {

    @Autowired
    TjenesteRepository repository

    ValidationService validationService = Mock()

    TjenesteUpdateHandler handler

    void setup() {
        handler = new TjenesteUpdateHandler(repository, validationService)
    }

    void cleanup() {
        //repository.deleteAll()
    }

    def "Given create event new TjenesteResource is created"() {
        given:
        def resource = newTjenesteResource('navn')
        def event = newTjenesteEvent('test.no', [resource], null, Operation.CREATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findAll()
        resources.size() == 1
        def mongo = resources.first().getValue()
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'navn'

        event.data.size() == 1
        def data = event.data.first() as TjenesteResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.navn == 'navn'
    }

    def "Given update event TjenesteResource is updated"() {
        given:
        def resource = newTjenesteResource('navn-1')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(TjenesteResource.canonicalName).value(resource).build())

        resource.navn = 'navn-2'
        def event = newTjenesteEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

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

    def "Given update event with non-writable attribute error is returned"() {
        given:
        def resource = newTjenesteResource('navn')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(TjenesteResource.canonicalName).value(resource).build())

        resource.systemId.identifikatorverdi = '123'
        def event = newTjenesteEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findByOrgIdAndType('test.no', TjenesteResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, TjenesteResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'navn'

        event.responseStatus == ResponseStatus.REJECTED
    }

    def "Given update event with invalid payload error is returned"() {
        given:
        def resource = newTjenesteResource('navn')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(TjenesteResource.canonicalName).value(resource).build())

        resource.navn = null
        def event = newTjenesteEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> [new Problem()]

        def resources = repository.findByOrgIdAndType('test.no', TjenesteResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, TjenesteResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'navn'

        event.responseStatus == ResponseStatus.REJECTED
    }

    def "Given update event on non-existent TjenesteResource exception is thrown"() {
        given:
        def resource = newTjenesteResource('navn')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(TjenesteResource.canonicalName).value(resource).build())

        def event = newTjenesteEvent('test.no', [resource], 'systemid/id-2', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        thrown(RowNotFoundException)
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