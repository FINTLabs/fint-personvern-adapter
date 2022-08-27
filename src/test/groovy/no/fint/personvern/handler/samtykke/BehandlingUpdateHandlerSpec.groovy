package no.fint.personvern.handler.samtykke

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.event.model.Event
import no.fint.event.model.Operation
import no.fint.event.model.Problem
import no.fint.event.model.ResponseStatus
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.personvern.exception.RowNotFoundException
import no.fint.personvern.handler.samtykke.behandling.BehandlingRepository
import no.fint.personvern.handler.samtykke.behandling.BehandlingUpdateHandler
import no.fint.personvern.service.ValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@DirtiesContext
class BehandlingUpdateHandlerSpec extends Specification {

    @Autowired
    BehandlingRepository repository

    ValidationService validationService = Mock()

    BehandlingUpdateHandler handler

    void setup() {
        handler = new BehandlingUpdateHandler(repository, validationService)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given create event new BehandlingResource is created"() {
        given:
        def resource = newBehandlingResource(true)

        def event = newBehandlingEvent('test.no', [resource], null, Operation.CREATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findByOrgIdAndType('test.no', BehandlingResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, BehandlingResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.formal == 'formal'
        mongo.aktiv

        event.data.size() == 1
        def data = event.data.first() as BehandlingResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.formal == 'formal'
        data.aktiv
    }

    def "Given update event BehandlingResource is updated"() {
        given:
        def resource = newBehandlingResource(true)
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(BehandlingResource.canonicalName).value(resource).build())

        resource.aktiv = false
        def event = newBehandlingEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findByOrgIdAndType('test.no', BehandlingResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, BehandlingResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.formal == 'formal'
        !mongo.aktiv

        event.data.size() == 1
        def data = event.data.first() as BehandlingResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.formal == 'formal'
        !data.aktiv
    }

    def "Given update event with non-writable attribute error is returned"() {
        given:
        def resource = newBehandlingResource(true)
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(BehandlingResource.canonicalName).value(resource).build())

        resource.formal = 'formal2'
        def event = newBehandlingEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findByOrgIdAndType('test.no', BehandlingResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, BehandlingResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.formal == 'formal'
        mongo.aktiv

        event.responseStatus == ResponseStatus.REJECTED
    }

    def "Given update event with invalid payload error is returned"() {
        given:
        def resource = newBehandlingResource(true)
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(BehandlingResource.canonicalName).value(resource).build())

        resource.formal = null
        def event = newBehandlingEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> [new Problem()]

        def resources = repository.findByOrgIdAndType('test.no', BehandlingResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, BehandlingResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.formal == 'formal'
        mongo.aktiv

        event.responseStatus == ResponseStatus.REJECTED
    }

    def "Given update event on non-existent BehandlingsResource exception is thrown"() {
        given:
        def resource = newBehandlingResource(true)
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(BehandlingResource.canonicalName).value(resource).build())

        def event = newBehandlingEvent('test.no', [resource], 'systemid/id-2', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        thrown(RowNotFoundException)
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