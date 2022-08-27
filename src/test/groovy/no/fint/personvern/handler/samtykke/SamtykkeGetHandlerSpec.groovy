package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.personvern.handler.samtykke.samtykke.SamtykkeEntity
import no.fint.personvern.handler.samtykke.samtykke.SamtykkeGetHandler
import no.fint.personvern.handler.samtykke.samtykke.SamtykkeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

import java.time.Instant

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@DirtiesContext
class SamtykkeGetHandlerSpec extends Specification {

    @Autowired
    SamtykkeRepository repository

    SamtykkeGetHandler handler

    void setup() {
        handler = new SamtykkeGetHandler(repository)
    }

//    void cleanup() {
//        repository.deleteAll()
//    }

    def "Given orgId response contains SamtykkeResource for the given orgId"() {
        given:

        when:
        handler.accept(event)
        repository.save(
                new SamtykkeEntity(
                       'id-1',
                        newSamtykkeResource('id-1'),
                        'test-1.no',
                       Date.from(Instant.parse('2021-01-01T00:00:00.00Z')))
        )


                repository.save(
                        SamtykkeEntity
                                .builder()
                                .id('id-2')
                                .orgId('test-2.no')
                                .value(newSamtykkeResource('id-2'))
                                .lastModifiedDate(Date.from(Instant.parse('2021-01-01T00:00:00.00Z')))
                                .build())

        def event = newSamtykkeEvent('test-1.no')

        then:
        event.data.size() == 1
        def resource = event.data.first() as SamtykkeResource
        resource.systemId.identifikatorverdi == 'id-1'
        resource.opprettet == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        resource.gyldighetsperiode.start == Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        resource.gyldighetsperiode.slutt == Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))
    }

    def newSamtykkeEvent(String orgId) {
        return new Event<FintLinks>(orgId: orgId)
    }

    def newSamtykkeResource(String id) {
        return new SamtykkeResource(
                systemId: new Identifikator(identifikatorverdi: id),
                opprettet: Date.from(Instant.parse('2021-01-01T00:00:00.00Z')),
                gyldighetsperiode: new Periode(start: Date.from(Instant.parse('2021-01-01T00:00:00.00Z')), slutt: Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))))
    }
}
