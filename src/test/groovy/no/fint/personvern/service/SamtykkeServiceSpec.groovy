package no.fint.personvern.service

import no.fint.event.model.Event
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.FintLinks
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.personvern.exception.MongoCantFindDocumentException
import no.fint.personvern.wrapper.WrapperDocumentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

import java.time.ZonedDateTime

@DataMongoTest
class SamtykkeServiceSpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    SamtykkeService samtykkeService

    void setup() {
        samtykkeService = new SamtykkeService(repository)
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "When creating a new Samtykke systemId and Opprettet should be set"() {
        given:
        def date = Date.from(ZonedDateTime.parse('2020-12-12T14:00:00Z').toInstant())
        def event = newSamtykkeEvent('test.no', [newSamtykkeResource(date)], null)

        when:
        samtykkeService.createSamtykke(event)

        then:
        def resources = repository.findAll()
        resources.size() == 1
        def mongo = resources.first().value as SamtykkeResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == date

        event.data.size() == 1
        def data = event.data.first() as SamtykkeResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.gyldighetsperiode.start == date
    }

    def "When updating the mongo document should be updated"() {
        given:
        def oldDate = Date.from(ZonedDateTime.parse('2020-12-12T14:00:00Z').toInstant())
        def createEvent = newSamtykkeEvent('test.no', [newSamtykkeResource(oldDate)], null)
        samtykkeService.createSamtykke(createEvent)

        def newDate = Date.from(ZonedDateTime.parse('2020-12-12T15:00:00Z').toInstant())
        SamtykkeResource samtykkeResource = createEvent.data.first() as SamtykkeResource
        def updateEvent = newSamtykkeEvent('test.no', [newSamtykkeResource(newDate)], 'systemid/' + samtykkeResource.systemId.identifikatorverdi)

        when:
        samtykkeService.updateSamtykke(updateEvent)

        then:
        def resources = repository.findAll()
        resources.size() == 1
        def mongo = resources.first().value as SamtykkeResource
        mongo.systemId.identifikatorverdi == resources.first().id
        mongo.gyldighetsperiode.start == newDate

        updateEvent.data.size() == 1
        def data = updateEvent.data.first() as SamtykkeResource
        data.systemId.identifikatorverdi == mongo.systemId.identifikatorverdi
        data.gyldighetsperiode.start == newDate
    }

    def "When updating an object that does not exist and exception should be raised"() {
        given:
        def updateEvent = newSamtykkeEvent('test.no', [newSamtykkeResource(null)], 'systemid/id')

        when:
        samtykkeService.updateSamtykke(updateEvent)

        then:
        thrown(MongoCantFindDocumentException)
    }

    def "When get all Samtykke the list should only contain Tjenster for the given orgId"() {
        given:
        def createEventOne = newSamtykkeEvent('test1.no', [newSamtykkeResource(null)], null)
        samtykkeService.createSamtykke(createEventOne)

        def createEventTwo = newSamtykkeEvent('test2.no', [newSamtykkeResource(null)], null)
        samtykkeService.createSamtykke(createEventTwo)

        def getEvent = newSamtykkeEvent('test1.no', [], null)

        when:
        samtykkeService.getAllSamtykke(getEvent)

        then:
        def resources = repository.findAll()
        resources.size() == 2

        getEvent.data.size() == 1
        def resource = getEvent.data.first() as SamtykkeResource
        resource.systemId.identifikatorverdi
    }

    def newSamtykkeEvent(String orgId, List<FintLinks> data, String query) {
        return new Event<FintLinks>(
                orgId: orgId,
                data: data,
                query: query
        )
    }

    def newSamtykkeResource(Date start) {
        return new SamtykkeResource(
                gyldighetsperiode: new Periode(
                        start: start,
                        slutt: null
                )
        )
    }
}