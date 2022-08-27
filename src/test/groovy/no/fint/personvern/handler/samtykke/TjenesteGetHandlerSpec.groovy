package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.TjenesteResource
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteEntity
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteGetHandler
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDateTime

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class TjenesteGetHandlerSpec extends Specification {

    @Autowired
    TjenesteRepository repository

    TjenesteGetHandler handler

    void setup() {
        handler = new TjenesteGetHandler(repository)
    }

    def "Given values are correct"() {
        given:
        repository.save(newEntity("id-test-1", "org-no", newResource("test-of-id-1", "navn-test")))
        def event = newEvent('org-no')

        when:
        handler.accept(event)
        def resource = (TjenesteResource) event.data.get(0)

        then:
        resource
        resource.systemId.identifikatorverdi == 'test-of-id-1'
        resource.navn == 'navn-test'
    }

    def "Correct number of rows"() {
        given:
        repository.save(newEntity("id1", "org-no", newResource("id1", "navn")))
        repository.save(newEntity("id2", "org-no", newResource("id2", "navn")))
        repository.save(newEntity("id3", "org-no", newResource("id3", "navn")))
        repository.save(newEntity("id-4", "org-no", newResource("id-4", "navn")))
        def event = newEvent('org-no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 4
    }

    def "Dont get rows from other orgid"() {
        given:
        repository.save(newEntity("id1", "org-no", newResource("id1", "navn")))
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

    def newEntity(String id, String orgId, TjenesteResource resource) {
        return TjenesteEntity.builder()
                .id(id)
                .orgId(orgId)
                .resource(resource)
                .lastModifiedDate(getLocalDateTime())
                .build()
    }

    def newResource(String systemId, String navn) {
        def resource = new TjenesteResource()
        resource.setSystemId(newId(systemId))
        resource.setNavn(navn)
        return resource
    }

    def newId(String systemId) {
        def id = new Identifikator()
        id.setIdentifikatorverdi(systemId)
        return id;
    }
}
