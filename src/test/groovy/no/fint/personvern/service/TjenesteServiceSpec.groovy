package no.fint.personvern.service

import no.fint.event.model.Event
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.TjenesteResource
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.wrapper.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

@DataMongoTest
class TjenesteServiceSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    TjenesteService tjenesteService

    void setup() {
        tjenesteService = new TjenesteService(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "When creating a new Tjeneste systemId should be set"() {
        given:
        def event = newTjenesteEvent('test.no', [newTjenesteResource('navn')], null)

        when:
        tjenesteService.createTjeneste(event)

        then:
        def resources = repository.findAll()
        resources.size() == 1
        def mongo = resources.first().value as TjenesteResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'navn'

        event.data.size() == 1
        def data = event.data.first() as TjenesteResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.navn == 'navn'
    }

    def "When updating the mongo document should be updateds"() {
        given:
        def createEvent = newTjenesteEvent('test.no', [newTjenesteResource('navn')], null)
        tjenesteService.createTjeneste(createEvent)

        TjenesteResource tjenesteResource = createEvent.data.first() as TjenesteResource
        def updateEvent = newTjenesteEvent('test.no', [newTjenesteResource('nytt navn')], 'systemid/' + tjenesteResource.systemId.identifikatorverdi)

        when:
        tjenesteService.updateTjeneste(updateEvent)

        then:
        def resources = repository.findAll()
        resources.size() == 1
        def mongo = resources.first().value as TjenesteResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.navn == 'nytt navn'

        updateEvent.data.size() == 1
        def data = updateEvent.data.first() as TjenesteResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.navn == 'nytt navn'
    }

    def "When updating an object that does not exist and exception should be raised"() {
        given:
        def event = newTjenesteEvent('test.no', [newTjenesteResource(null)], 'systemid/id')

        when:
        tjenesteService.updateTjeneste(event)

        then:
        thrown(MongoCantFindDocumentException)
    }

    def "When get all Tjeneste the list should only contain Tjenster for the given orgId"() {
        given:
        def createEventOne = newTjenesteEvent('test1.no', [newTjenesteResource('navn')], null)
        tjenesteService.createTjeneste(createEventOne)

        def createEventTwo = newTjenesteEvent('test2.no', [newTjenesteResource('navn')], null)
        tjenesteService.createTjeneste(createEventTwo)

        def getEvent = newTjenesteEvent('test1.no', [], null)

        when:
        tjenesteService.getAllTjeneste(getEvent)

        then:
        def resources = repository.findByOrgIdAndType('test1.no', TjenesteResource.class.getCanonicalName())
        resources.size() == 1

        getEvent.data.size() == 1
    }

    def newTjenesteEvent(String orgId, List<FintLinks> data, String query) {
        return new Event<FintLinks>(
                orgId: orgId,
                data: data,
                query: query
        )
    }

    def newTjenesteResource(String navn) {
        return new TjenesteResource(
                navn: navn
        )
    }
}