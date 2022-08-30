package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.personvern.handler.samtykke.behandling.BehandlingGetHandler
import no.fint.personvern.handler.samtykke.samtykke.SamtykkeEntity
import no.fint.personvern.handler.samtykke.samtykke.SamtykkeGetHandler
import no.fint.personvern.handler.samtykke.samtykke.SamtykkeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDateTime

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class SamtykkeGetHandlerSpec extends Specification {

    @Autowired
    SamtykkeRepository repository

    SamtykkeGetHandler handler

    void setup() {
        handler = new SamtykkeGetHandler(repository)
    }

    def "Given values are correct"() {
        given:
        repository.save(newEntity("id-test-1", "org-no", newResource("test-of-id-1", getDate(), getPeriode())))
        def event = newEvent('org-no')

        when:
        handler.accept(event)
        def resource = (SamtykkeResource) event.data.get(0)

        then:
        resource
        resource.systemId.identifikatorverdi == 'test-of-id-1'
        resource.opprettet == getDate()
        resource.getGyldighetsperiode() == getPeriode()
    }

    def "Correct number of rows"() {
        given:
        repository.save(newEntity("id1", "org-no", newResource("id1", getDate(), getPeriode())))
        repository.save(newEntity("id2", "org-no", newResource("id2", getDate(), getPeriode())))
        repository.save(newEntity("id3", "org-no", newResource("id3", getDate(), getPeriode())))
        repository.save(newEntity("id-4", "org-no", newResource("id-4", getDate(), getPeriode())))
        def event = newEvent('org-no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 4
    }

    def "Dont get rows from other orgid"() {
        given:
        repository.save(newEntity("id1", "org-no", newResource("id1", getDate(), getPeriode())))
        def event = newEvent('correct-1.no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 0
    }

    def newEvent(String orgId) {
        return new Event<FintLinks>(
                orgId: orgId
        )
    }

    def getLocalDateTime() {
        return LocalDateTime.parse('2021-01-01T00:00:00.00');
    }

    def newEntity(String id, String orgId, SamtykkeResource resource) {
        return SamtykkeEntity.builder()
                .id(id)
                .orgId(orgId)
                .resource(resource)
                .lastModifiedDate(getLocalDateTime())
                .build()
    }

    def newResource(String systemId, Date opprettet, Periode gyldighetsperiode) {
        def resource = new SamtykkeResource()
        resource.setSystemId(newId(systemId))
        resource.setOpprettet(opprettet)
        resource.setGyldighetsperiode(gyldighetsperiode)
        return resource
    }

    def newId(String systemId) {
        def id = new Identifikator()
        id.setIdentifikatorverdi(systemId)
        return id;
    }

    def getDate() {
        return Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
    }

    def getPeriode() {
        def periode = new Periode()
        periode.start = Date.from(Instant.parse('2021-01-01T00:00:00.00Z'))
        periode.slutt = Date.from(Instant.parse('2021-02-01T00:00:00.00Z'))
        return periode
    }
}
