package no.fint.personvern.repository

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.personvern.handler.samtykke.behandling.BehandlingEntity
import no.fint.personvern.handler.samtykke.behandling.BehandlingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

import java.time.LocalDateTime

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@DirtiesContext
class BehandlingRepositorySpec extends Specification {

    @Autowired
    BehandlingRepository repository

    def "Add element"() {
        given:

        when:
        repository.save(BehandlingEntity.builder().id("1234").orgId("test-no").build())

        then:
        repository.count() == 1
    }

    def "Add multiple elements"() {
        given:
        repository.save(newEntity('id-1', 'org-id-1', newResource("Test", "Test", false)))
        repository.save(newEntity('id-2', 'org-id-2', newResource("Test", "Test", false)))

        when:
        def test = repository.findAll()

        then:
        test.size() == 2
    }

    def "Find by orgId and type returns documents"() {
        given:
        repository.save(newEntity('id-1', 'org-id-1', newResource("123", "Test", true)))
        repository.save(newEntity('id-2', 'org-id-1', newResource("1234", "Test2", false)))

        when:
        def test = repository.findById('id-1')

        then:
        test.isPresent()
        test.get().id == 'id-1'
        test.get().value.systemId.identifikatorverdi == "123"
        test.get().value.formal == "Test"
        test.get().value.aktiv == true
    }

    def "Find by id and orgId returns document"() {
        given:
        repository.save(newEntity('id-1', 'org-id-1', newResource("Test", "Test", false)))
        repository.save(newEntity('id-2', 'org-id-2', newResource("Test", "Test", false)))

        when:
        def test = repository.findById('id-1')

        then:
        test.isPresent()
        test.get().getId() == 'id-1'
        test.get().getOrgId() == 'org-id-1'
        test.get().getValue().getFormal() == "Test"
        test.get().getValue().getAktiv() == false
    }

    def newEntity(String id, String orgId, BehandlingResource resource) {
        return BehandlingEntity.builder()
                .id(id)
                .orgId(orgId)
                .value(resource)
                .lastModifiedDate(LocalDateTime.now())
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
