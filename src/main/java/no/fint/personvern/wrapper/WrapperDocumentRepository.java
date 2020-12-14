package no.fint.personvern.wrapper;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Document(collection = "personvern")
public interface WrapperDocumentRepository extends MongoRepository<WrapperDocument, String> {

    WrapperDocument findByIdAndOrgId(String id, String orgId);
    List<WrapperDocument> findByOrgIdAndType(String orgId, String type);
}
