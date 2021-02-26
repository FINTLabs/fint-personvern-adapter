package no.fint.personvern.handler.samtykke

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.event.model.Event
import no.fint.event.model.Operation
import no.fint.event.model.Problem
import no.fint.event.model.ResponseStatus
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.repository.WrapperDocument
import no.fint.personvern.repository.WrapperDocumentRepository
import no.fint.personvern.service.ValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

import java.time.Instant

@DataMongoTest
class SamtykkeUpdateHandlerSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    ValidationService validationService = Mock()

    SamtykkeUpdateHandler handler

    void setup() {
        handler = new SamtykkeUpdateHandler(repository, validationService)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given create event new SamtykkeResource is created"() {
        given:
        def resource = newSamtykkeResource('2021-02-01T00:00:00.00Z')

        def event = newSamtykkeEvent('test.no', [resource], null, Operation.CREATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findByOrgIdAndType('test.no', SamtykkeResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, SamtykkeResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        mongo.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))

        event.data.size() == 1
        def data = event.data.first() as SamtykkeResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        data.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))
    }

    def "Given update event SamtykkeResource is updated"() {
        given:
        def resource = newSamtykkeResource('2021-02-01T00:00:00.00Z')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(SamtykkeResource.canonicalName).value(resource).build())

        resource.gyldighetsperiode.slutt = Date.from(Instant.parse('2021-03-01T00:00:00.00Z'))
        def event = newSamtykkeEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findByOrgIdAndType('test.no', SamtykkeResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, SamtykkeResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        mongo.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-03-01T00:00:00.00Z'))

        event.data.size() == 1
        def data = event.data.first() as SamtykkeResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        data.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-03-01T00:00:00.00Z'))
    }

    def "Given update event with non-writable attribute error is returned"() {
        given:
        def resource = newSamtykkeResource('2021-02-01T00:00:00.00Z')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(SamtykkeResource.canonicalName).value(resource).build())

        resource.setOpprettet(Date.from(Instant.parse('2021-02-01T00:00:00.00Z')))
        def event = newSamtykkeEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        def resources = repository.findByOrgIdAndType('test.no', SamtykkeResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, SamtykkeResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        mongo.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))

        event.responseStatus == ResponseStatus.REJECTED
    }

    def "Given update event with invalid payload error is returned"() {
        given:
        def resource = newSamtykkeResource('2021-02-01T00:00:00.00Z')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(SamtykkeResource.canonicalName).value(resource).build())

        resource.setOpprettet(null)
        def event = newSamtykkeEvent('test.no', [resource], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> [new Problem()]

        def resources = repository.findByOrgIdAndType('test.no', SamtykkeResource.canonicalName)
        resources.size() == 1
        def mongo = new ObjectMapper().convertValue(resources.first().value, SamtykkeResource.class)
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        mongo.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))

        event.responseStatus == ResponseStatus.REJECTED
    }

    def "Given update event on non-existent SamtykkeResource exception is thrown"() {
        given:
        def resource = newSamtykkeResource('2021-02-01T00:00:00.00Z')
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(SamtykkeResource.canonicalName).value(resource).build())

        resource.gyldighetsperiode.slutt = Date.from(Instant.parse('2021-03-01T00:00:00.00Z'))
        def event = newSamtykkeEvent('test.no', [resource], 'systemid/id-2', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        1 * validationService.getProblems(resource) >> []

        thrown(MongoCantFindDocumentException)
    }

    def newSamtykkeEvent(String orgId, List<FintLinks> data, String query, Operation operation) {
        return new Event<FintLinks>(
                orgId: orgId,
                data: data,
                query: query,
                operation: operation
        )
    }

    def newSamtykkeResource(String slutt) {
        return new SamtykkeResource(
                systemId: new Identifikator(identifikatorverdi: 'id'),
                opprettet: Date.from(Instant.parse('2021-01-01T00:00:00.00Z')),
                gyldighetsperiode: new Periode(start: Date.from(Instant.parse('2021-01-01T00:00:00.00Z')), slutt: Date.from(Instant.parse(slutt)))
        )
    }
}