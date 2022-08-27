package no.fint.personvern.handler.samtykke

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.TjenesteResource
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteGetHandler
import no.fint.personvern.handler.samtykke.tjeneste.TjenesteRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@DirtiesContext
class TjenesteGetHandlerSpec extends Specification {

    @Autowired
    TjenesteRepository repository

    TjenesteGetHandler handler

    void setup() {
        handler = new TjenesteGetHandler(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "Given orgId response contains TjenesteResource for the given orgId"() {
        given:
        repository.save(WrapperDocument.builder().id('id-1').orgId('test-1.no').type(TjenesteResource.canonicalName).value(newTjenesteResource('id-1')).build())
        repository.save(WrapperDocument.builder().id('id-2').orgId('test-2.no').type(TjenesteResource.canonicalName).value(newTjenesteResource('id-2')).build())

        def event = newTjenesteEvent('test-1.no')

        when:
        handler.accept(event)

        then:
        event.data.size() == 1
        def resource = event.data.first() as TjenesteResource
        resource.systemId.identifikatorverdi == 'id-1'
        resource.navn == 'navn'
    }

    def newTjenesteEvent(String orgId) {
        return new Event<FintLinks>(
                orgId: orgId
        )
    }

    def newTjenesteResource(String id) {
        return new TjenesteResource(
                navn: 'navn',
                systemId: new Identifikator(identifikatorverdi: id)
        )
    }
}
