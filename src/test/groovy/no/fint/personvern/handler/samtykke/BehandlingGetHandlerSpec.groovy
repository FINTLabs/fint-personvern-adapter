package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.personvern.handler.samtykke.behandling.BehandlingEntity
import no.fint.personvern.handler.samtykke.behandling.BehandlingGetHandler
import no.fint.personvern.handler.samtykke.behandling.BehandlingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

import java.time.LocalDateTime

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class BehandlingGetHandlerSpec extends Specification {

    @Autowired
    BehandlingRepository repository

    BehandlingGetHandler handler

    void setup() {
        handler = new BehandlingGetHandler(repository)
    }

    def "Given values are correct"() {
        given:
        repository.save(newEntity("id-test-1", "org-no", newResource("test-of-id-1", "formal with this record", true)))
        def event = newEvent('org-no')

        when:
        handler.accept(event)
        def resource = (BehandlingResource) event.data.get(0)

        then:
        resource
        resource.systemId.identifikatorverdi == 'test-of-id-1'
        resource.formal == 'formal with this record'
        resource.aktiv == true
    }

    def "Correct number of rows"() {
        given:
        repository.save(newEntity("id1", "org-no", newResource("id1", "formal", true)))
        repository.save(newEntity("id2", "org-no", newResource("id2", "other", false)))
        repository.save(newEntity("id3", "org-no", newResource("id3", "test", true)))
        repository.save(newEntity("id-4", "org-no", newResource("id-4", "formal test", false)))
        def event = newEvent('org-no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 4
    }

    def "Dont get rows from other orgid"() {
        given:
        repository.save(newEntity("id1", "org-no", newResource("id1", "formal", true)))
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

    def getDate() {
        return LocalDateTime.parse('2021-01-01T00:00:00.00');
    }

    def newEntity(String id, String orgId, BehandlingResource resource) {
        return BehandlingEntity.builder()
                .id(id)
                .orgId(orgId)
                .resource(resource)
                .lastModifiedDate(getDate())
                .build()
    }

    def newResource(String systemId, String formal, boolean aktiv) {
        def resource = new BehandlingResource()
        resource.setAktiv(aktiv)
        resource.setFormal(formal)
        resource.setSystemId(newId(systemId))
        return resource
    }

    def newId(String systemId) {
        def id = new Identifikator()
        id.setIdentifikatorverdi(systemId)
        return id;
    }
}