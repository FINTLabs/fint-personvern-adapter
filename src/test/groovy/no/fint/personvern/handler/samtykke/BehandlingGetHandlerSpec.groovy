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
@DirtiesContext
class BehandlingGetHandlerSpec extends Specification {

    @Autowired
    BehandlingRepository repository

    BehandlingGetHandler handler

    void setup() {
        handler = new BehandlingGetHandler(repository)
    }

    def "Given orgId response contains BehandlingResource for the given orgId"() {
        given:
        repository.save(BehandlingEntity.builder().id('id-1').orgId('test-1.no').value(newBehandlingResource('id-1')).lastModifiedDate(getDate()).build())
        repository.save(BehandlingEntity.builder().id('id-2').orgId('test-2.no').value(newBehandlingResource('id-2')).lastModifiedDate(getDate()).build())

        def event = newEvent('test-1.no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 1
        def resource = event.data.first() as BehandlingResource
        resource.systemId.identifikatorverdi == 'id-1'
        resource.formal == 'formal'
        resource.aktiv
    }

    def "Returned object contains correct values"() {

    }

    def "Return all rows"() {

    }

    def "Add element"() {
        given:
        def resource = newResource("Test", "Test", false)
        /// TODO feiler med resource, fungerer uten.
        def entity = BehandlingEntity.builder().id("1234").orgId("test-no").value(resource).lastModifiedDate(getDate()).build()
        repository.save(entity)
//        def entity = newEntity("1234", "wrong-1.no", newResource("1234", "formal", true))
//        repository.save(entity)
        def event = newEvent('correct-1.no')

        when:
        handler.accept(event)

        then:
        event.getData().size() == 0
    }

    def "Dont return rows with wrong orgid"() {
        given:
        def entity = newEntity("id1", "org-no", newResource("id1", "formal", true))
        repository.save(entity)

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

    def newBehandlingResource(String id) {
        return new BehandlingResource(
                aktiv: true,
                formal: 'formal',
                systemId: new Identifikator(identifikatorverdi: id)
        )
    }

    def getDate() {
        return LocalDateTime.parse('2021-01-01T00:00:00.00');
    }

    def newEntity(String id, String orgId, BehandlingResource resource) {
        return BehandlingEntity.builder()
                .id(id)
                .orgId(orgId)
                .value(resource)
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