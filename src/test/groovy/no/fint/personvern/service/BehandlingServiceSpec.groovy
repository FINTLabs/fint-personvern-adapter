package no.fint.personvern.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.TestApplication
import no.fint.model.resource.Link
import no.fint.model.resource.personvern.samtykke.BehandlingResource
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
class BehandlingServiceSpec extends Specification {
    @Autowired
    WrapperDocumentRepository repository

    @Autowired
    AppProps appProps

    Wrapper wrapper = new Wrapper()
    BehandlingService behandlingService
    ObjectMapper objectMapper = new ObjectMapper()

    void setup() {
        behandlingService = new BehandlingService(wrapper, repository)

    }

    void cleanup() {
        repository.deleteAll()
    }

    def "When creating a new Behandling systemId should be set"() {
        given:
        def event = ObjectFactory.createBehandlingEvent()

        when:
        def behandling = behandlingService.createBehandling(event)

        then:
        behandling.getSystemId().identifikatorverdi != null
    }

    def "When creating a new Behandling document _id should be equal to systemId"() {
        given:
        def event = ObjectFactory.createBehandlingEvent()

        when:
        def behandling = behandlingService.createBehandling(event)
        def doc = repository.findByIdAndOrgId(behandling.getSystemId().getIdentifikatorverdi(), event.getOrgId())

        then:
        doc.id == behandling.getSystemId().getIdentifikatorverdi()
    }

    def "When updating the mongo document should be updated"() {
        given:
        def event = ObjectFactory.createBehandlingEvent()
        def behandling = behandlingService.createBehandling(event)
        behandling.setFormal("Formål")
        behandling.addBehandlingsgrunnlag(Link.with("test1"))
        behandling.addBehandlingsgrunnlag(Link.with("test2"))
        event.setData([behandling])
        event.setQuery("systemid/" + behandling.getSystemId().getIdentifikatorverdi())

        when:
        behandlingService.updateBehandling(event)
        def updatedBehandling = objectMapper.convertValue(
                repository
                        .findByIdAndOrgId(behandling.getSystemId().getIdentifikatorverdi(), event.orgId)
                        .getValue(),
                BehandlingResource.class)

        then:
        updatedBehandling.getFormal() == "Formål"
        updatedBehandling.getBehandlingsgrunnlag().size() == 2
    }

    def "When updating an object that does not exist and exception should be raised"() {
        when:
        behandlingService.updateBehandling(ObjectFactory.createBehandlingEvent())

        then:
        thrown(MongoCantFindDocumentException)
    }

    def "When get all Behandling the list should only contain Tjenster for the given orgId"() {
        given:
        behandlingService.createBehandling(ObjectFactory.createBehandlingEvent("test1.no"))
        behandlingService.createBehandling(ObjectFactory.createBehandlingEvent("test1.no"))
        behandlingService.createBehandling(ObjectFactory.createBehandlingEvent("test2.no"))

        when:
        def test1 = behandlingService.getAllBehandling("test1.no")
        def test2 = behandlingService.getAllBehandling("test2.no")

        then:
        test1.size() == 2
        test2.size() == 1
    }
}
