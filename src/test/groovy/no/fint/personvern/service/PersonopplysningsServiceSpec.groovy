package no.fint.personvern.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.TestApplication
import no.fint.model.hateos.Embedded
import no.fint.personvern.AppProps
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ActiveProfiles("test")
@ContextConfiguration(classes = [AppProps.class])
@SpringBootTest(classes = TestApplication)
class PersonopplysningsServiceSpec extends Specification {

    @Autowired
    PersonopplysningsService personopplysningsService

    ObjectMapper objectMapper = new ObjectMapper()
    MockWebServer mockWebServer = new MockWebServer()

    void setup() {
        mockWebServer.start(8080)
    }

    void cleanup() {
        mockWebServer.shutdown()
    }

    def "Should return one personopplysning pr. attribute in class"() {
        given:
        def file = getClass().getClassLoader().getResource('klasse.json')
        def klasse = objectMapper.readValue(file, Embedded.class)

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(klasse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setResponseCode(HttpStatus.OK.value()))
        personopplysningsService.getPersonopplysninger()

        when:
        def list = personopplysningsService.personopplysningResourceList

        then:
        list.size() == 5

    }

}
