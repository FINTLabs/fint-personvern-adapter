package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.personvern.repository.WrapperDocument
import no.fint.personvern.repository.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

import java.time.Instant

@DataMongoTest
class SamtykkeGetHandlerSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    SamtykkeGetHandler handler

    void setup() {
        handler = new SamtykkeGetHandler(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given orgId response contains SamtykkeResource for the given orgId"() {
        given:
        repository.save(WrapperDocument.builder().id('id-1').orgId('test-1.no').type(SamtykkeResource.canonicalName).value(newSamtykkeResource('id-1')).build())
        repository.save(WrapperDocument.builder().id('id-2').orgId('test-2.no').type(SamtykkeResource.canonicalName).value(newSamtykkeResource('id-2')).build())

        def event = newSamtykkeEvent('test-1.no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 1
        def resource = event.data.first() as SamtykkeResource
        resource.systemId.identifikatorverdi == 'id-1'
        resource.opprettet == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        resource.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        resource.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))
    }

    def newSamtykkeEvent(String orgId) {
        return new Event<FintLinks>(
                orgId: orgId
        )
    }

    def newSamtykkeResource(String id) {
        return new SamtykkeResource(
                systemId: new Identifikator(identifikatorverdi: id),
                opprettet: Date.from(Instant.parse('2021-01-01T00:00:00.00Z')),
                gyldighetsperiode: new Periode(start: Date.from(Instant.parse('2021-01-01T00:00:00.00Z')), slutt: Date.from(Instant.parse('2021-02-01T00:00:00.00Z')))
        )
    }
}
