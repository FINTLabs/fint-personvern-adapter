package no.fint.personvern.handler.kodeverk

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.event.model.Event
import no.fint.model.resource.FintLinks
import no.fint.model.resource.metamodell.KlasseResources
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class PersonopplysningHandlerSpec extends Specification {

    RestTemplate restTemplate = Mock()

    PersonopplysningHandler handler = new PersonopplysningHandler(restTemplate)

    def "Returns PersonopplysningResource, one resource pr. attribute in class"() {
        given:
        def clazz = new ObjectMapper().readValue(getClass().getClassLoader().getResource('klasse.json'), KlasseResources.class)

        def event = new Event<FintLinks>(orgId: 'test.no')

        when:
        handler.accept(event)

        then:
        1 * restTemplate.getForObject(_, _) >> clazz
        event.data.size() == 5
        def data = event.data.first() as PersonopplysningResource
        data.systemId
        data.kode
        data.navn
        !data.passiv
    }
}
