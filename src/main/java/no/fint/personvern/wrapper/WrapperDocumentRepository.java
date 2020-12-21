package no.fint.personvern.wrapper;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WrapperDocumentRepository extends MongoRepository<WrapperDocument, String> {

    WrapperDocument findByIdAndOrgId(String id, String orgId);

    List<WrapperDocument> findByOrgIdAndType(String orgId, String type);
}
