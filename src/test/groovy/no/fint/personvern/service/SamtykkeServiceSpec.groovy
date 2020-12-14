package no.fint.personvern.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.TestApplication
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.Link
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
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
class SamtykkeServiceSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    @Autowired
    AppProps appProps

    Wrapper wrapper = new Wrapper()
    SamtykkeService samtykkeService
    ObjectMapper objectMapper = new ObjectMapper()

    void setup() {
        samtykkeService = new SamtykkeService(wrapper, repository)

    }

    void cleanup() {
        repository.deleteAll()
    }

    def "When creating a new Samtykke systemId and Opprettet should be set"() {
        given:
        def event = ObjectFactory.createSamtykkeEvent()

        when:
        def samtykke = samtykkeService.createSamtykke(event)

        then:
        samtykke.getSystemId().identifikatorverdi != null
        samtykke.getOpprettet() != null
    }

    def "When creating a new Samtykke document _id should be equal to systemId"() {
        given:
        def event = ObjectFactory.createSamtykkeEvent()

        when:
        def samtykke = samtykkeService.createSamtykke(event)
        def doc = repository.findByIdAndOrgId(samtykke.getSystemId().getIdentifikatorverdi(), event.getOrgId())

        then:
        doc.id == samtykke.getSystemId().getIdentifikatorverdi()
    }

    def "When updating the mongo document should be updated"() {
        given:
        def event = ObjectFactory.createSamtykkeEvent()
        def samtykke = samtykkeService.createSamtykke(event)
        def slutt = new Date()
        samtykke.setGyldighetsperiode(new Periode(slutt: slutt))
        samtykke.addBehandling(Link.with("test1"))
        samtykke.addBehandling(Link.with("test2"))
        event.setData([samtykke])
        event.setQuery("systemid/" + samtykke.getSystemId().getIdentifikatorverdi())

        when:
        samtykkeService.updateSamtykke(event)
        def updatedSamtykke = objectMapper.convertValue(
                repository
                        .findByIdAndOrgId(samtykke.getSystemId().getIdentifikatorverdi(), event.orgId)
                        .getValue(),
                SamtykkeResource.class)

        then:
        updatedSamtykke.getGyldighetsperiode().slutt == slutt
        updatedSamtykke.getBehandling().size() == 2
    }

    def "When updating an object that does not exist and exception should be raised"() {
        when:
        samtykkeService.updateSamtykke(ObjectFactory.createSamtykkeEvent())

        then:
        thrown(MongoCantFindDocumentException)
    }

    def "When get all Samtykke the list should only contain Tjenster for the given orgId"() {
        given:
        samtykkeService.createSamtykke(ObjectFactory.createSamtykkeEvent("test1.no"))
        samtykkeService.createSamtykke(ObjectFactory.createSamtykkeEvent("test1.no"))
        samtykkeService.createSamtykke(ObjectFactory.createSamtykkeEvent("test2.no"))

        when:
        def test1 = samtykkeService.getAllSamtykke("test1.no")
        def test2 = samtykkeService.getAllSamtykke("test2.no")

        then:
        test1.size() == 2
        test2.size() == 1
    }
}
