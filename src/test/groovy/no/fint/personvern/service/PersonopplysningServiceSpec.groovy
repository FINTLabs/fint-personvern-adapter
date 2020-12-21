package no.fint.personvern.service

import com.fasterxml.jackson.databind.ObjectMapper

import no.fint.model.hateos.Embedded
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class PersonopplysningServiceSpec extends Specification {
    RestTemplate restTemplate = Mock()

    PersonopplysningService personopplysningService = new PersonopplysningService(restTemplate)

    def "Should return one personopplysning pr. attribute in class"() {
        given:
        def file = getClass().getClassLoader().getResource('klasse.json')
        def clazz = new ObjectMapper().readValue(file, Embedded.class)

        when:
        personopplysningService.getPersonopplysninger()

        then:
        1 * restTemplate.exchange(_, _, _, _) >> ResponseEntity.ok(clazz)
        personopplysningService.getPersonopplysningResources().size() == 5
    }
}