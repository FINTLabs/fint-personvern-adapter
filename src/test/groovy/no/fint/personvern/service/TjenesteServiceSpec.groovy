package no.fint.personvern.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.TestApplication
import no.fint.model.resource.Link
import no.fint.model.resource.personvern.samtykke.TjenesteResource
import no.fint.personvern.AppProps
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.wrapper.Wrapper
import no.fint.personvern.wrapper.WrapperDocumentRepository
import no.fint.utilities.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@DataMongoTest
@ActiveProfiles("test")
@ContextConfiguration(classes = [AppProps.class])
@SpringBootTest(classes = TestApplication)
class TjenesteServiceSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    @Autowired
    AppProps appProps

    Wrapper wrapper = new Wrapper()
    TjenesteService tjenesteService
    ObjectMapper objectMapper = new ObjectMapper()

    void setup() {
        tjenesteService = new TjenesteService(wrapper, repository)

    }

    void cleanup() {
        repository.deleteAll()
    }

    def "When creating a new Tjeneste systemId should be set"() {
        given:
        def event = ObjectFactory.createTjenesteEvent()

        when:
        def tjeneste = tjenesteService.createTjeneste(event)

        then:
        tjeneste.getSystemId().identifikatorverdi != null
    }

    def "When creating a new Tjeneste document _id should be equal to systemId"() {
        given:
        def event = ObjectFactory.createTjenesteEvent()

        when:
        def tjeneste = tjenesteService.createTjeneste(event)
        def doc = repository.findByIdAndOrgId(tjeneste.getSystemId().getIdentifikatorverdi(), event.getOrgId())

        then:
        doc.id == tjeneste.getSystemId().getIdentifikatorverdi()
    }

    def "When updating the mongo document should be updateds"() {
        given:
        def event = ObjectFactory.createTjenesteEvent()
        def tjeneste = tjenesteService.createTjeneste(event)
        tjeneste.setNavn("Updated tjeneste")
        tjeneste.addBehandling(Link.with("test1"))
        tjeneste.addBehandling(Link.with("test2"))
        event.setData([tjeneste])
        event.setQuery("systemid/" + tjeneste.getSystemId().getIdentifikatorverdi())

        when:
        tjenesteService.updateTjeneste(event)
        def updatedTjeneste = objectMapper.convertValue(
                repository
                        .findByIdAndOrgId(tjeneste.getSystemId().getIdentifikatorverdi(), event.orgId)
                        .getValue(),
                TjenesteResource.class)

        then:
        updatedTjeneste.getNavn() == "Updated tjeneste"
        updatedTjeneste.getBehandling().size() == 2
    }

    def "When updating an object that does not exist and exception should be raised"() {
        when:
        tjenesteService.updateTjeneste(ObjectFactory.createTjenesteEvent())

        then:
        thrown(MongoCantFindDocumentException)
    }

    def "When get all Tjeneste the list should only contain Tjenster for the given orgId"() {
        given:
        tjenesteService.createTjeneste(ObjectFactory.createTjenesteEvent("test1.no"))
        tjenesteService.createTjeneste(ObjectFactory.createTjenesteEvent("test1.no"))
        tjenesteService.createTjeneste(ObjectFactory.createTjenesteEvent("test2.no"))

        when:
        def test1 = tjenesteService.getAllTjeneste("test1.no")
        def test2 = tjenesteService.getAllTjeneste("test2.no")

        then:
        test1.size() == 2
        test2.size() == 1
    }
}
