package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.personvern.configuration.MongoConfiguration
import no.fint.personvern.handler.samtykke.behandling.BehandlingGetHandler
import no.fint.personvern.repository.WrapperDocument
import no.fint.personvern.repository.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@TestPropertySource(properties = "spring.mongodb.embedded.version=3.5.5")
@DataMongoTest
@Import(MongoConfiguration.class)
class BehandlingGetHandlerSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    BehandlingGetHandler handler

    void setup() {
        handler = new BehandlingGetHandler(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given orgId response contains BehandlingResource for the given orgId"() {
        given:
        repository.save(WrapperDocument.builder().id('id-1').orgId('test-1.no').type(BehandlingResource.canonicalName).value(newBehandlingResource('id-1')).build())
        repository.save(WrapperDocument.builder().id('id-2').orgId('test-2.no').type(BehandlingResource.canonicalName).value(newBehandlingResource('id-2')).build())

        def event = newBehandlingEvent('test-1.no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 1
        def resource = event.data.first() as BehandlingResource
        resource.systemId.identifikatorverdi == 'id-1'
        resource.formal == 'formal'
        resource.aktiv
    }

    def newBehandlingEvent(String orgId) {
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
}