package no.fint.personvern.repository

import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

@DataMongoTest
class WrapperDocumentRepositorySpec extends Specification {

    @Autowired
    WrapperDocumentRepository repository

    void cleanup() {
        repository.deleteAll()
    }

    def "Find by id and orgId returns document"() {
        given:
        def resources = [newWrapperDocument('id-1', 'org-id-1', BehandlingResource.class.getCanonicalName()),
                         newWrapperDocument('id-2', 'org-id-2', BehandlingResource.class.getCanonicalName())]
        repository.save(resources)

        when:
        def test = repository.findByIdAndOrgId('id-1', 'org-id-1')

        then:
        test
        test.id == 'id-1'
    }

    def "Find by orgId and type returns documents"() {
        given:
        def resources = [newWrapperDocument('id-1', 'org-id-1', BehandlingResource.class.getCanonicalName()),
                         newWrapperDocument('id-2', 'org-id-1', SamtykkeResource.class.getCanonicalName())]
        repository.save(resources)

        when:
        def test = repository.findByOrgIdAndType('org-id-1', BehandlingResource.class.getCanonicalName())

        then:
        test.size() == 1
        test.first().id == 'id-1'
    }

    def newWrapperDocument(String id, String orgId, String type) {
        return WrapperDocument.builder()
                .id(id)
                .orgId(orgId)
                .type(type)
                .build()
    }
}
