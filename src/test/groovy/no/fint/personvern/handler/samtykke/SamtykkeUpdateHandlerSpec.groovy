package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.event.model.Operation
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.repository.WrapperDocument
import no.fint.personvern.repository.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

import java.time.Instant

@DataMongoTest
class SamtykkeUpdateHandlerSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    SamtykkeUpdateHandler handler

    void setup() {
        handler = new SamtykkeUpdateHandler(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given create event a new SamtykkeResource is created"() {
        given:
        def event = newSamtykkeEvent('test.no', [newSamtykkeResource('2021-02-01T00:00:00.00Z')], null, Operation.CREATE)

        when:
        handler.accept(event)

        then:
        def resources = repository.findByOrgIdAndType('test.no', SamtykkeResource.canonicalName)
        resources.size() == 1
        def mongo = resources.first().value as SamtykkeResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        mongo.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))

        event.data.size() == 1
        def data = event.data.first() as SamtykkeResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        data.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))
    }

    def "Given update event a SamtykkeResource is updated"() {
        given:
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(SamtykkeResource.canonicalName).value(newSamtykkeResource('2021-02-01T00:00:00.00Z')).build())

        def event = newSamtykkeEvent('test.no', [newSamtykkeResource('2021-03-01T00:00:00.00Z')], 'systemid/id', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
        def resources = repository.findByOrgIdAndType('test.no', SamtykkeResource.canonicalName)
        resources.size() == 1
        def mongo = resources.first().value as SamtykkeResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        mongo.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-03-01T00:00:00.00Z'))

        event.data.size() == 1
        def data = event.data.first() as SamtykkeResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        data.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-03-01T00:00:00.00Z'))
    }

    def "Given update event on non-existent SamtykkeResource exception is thrown"() {
        given:
        repository.save(WrapperDocument.builder().id('id').orgId('test.no').type(SamtykkeResource.canonicalName).value(newSamtykkeResource('2021-02-01T00:00:00.00Z')).build())

        def event = newSamtykkeEvent('test.no', [newSamtykkeResource('2021-03-01T00:00:00.00Z')], 'systemid/id-2', Operation.UPDATE)

        when:
        handler.accept(event)

        then:
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
