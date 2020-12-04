package no.fint.personvern.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.util.JSON;
import no.fint.model.resource.FintLinks;
import org.jooq.lambda.Unchecked;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Wrapper {

    private ObjectMapper objectMapper = new ObjectMapper();

    public Function<Object, WrapperDocument> wrapper(Class<?> type, String orgId) {
        return Unchecked.function((Object content) -> new WrapperDocument(null, type.getCanonicalName(), JSON.parse(objectMapper.writeValueAsString(content)), orgId));
    }

    public <T> Function<WrapperDocument, T> unwrapper(Class<T> type) {
        return (WrapperDocument wrapperDocument) -> objectMapper.convertValue(wrapperDocument.getValue(), type);
    }

    public <T> Query query(Class<T> type, String orgId) {
        return new Query()
                .addCriteria(Criteria.where("type").is(type.getCanonicalName()))
                .addCriteria(Criteria.where("orgId").is(orgId));
    }

    public <T> WrapperDocument update(WrapperDocument wrapperDocument, T content) {
        wrapperDocument.setValue(JSON.parse(Unchecked.function(objectMapper::writeValueAsString).apply(content)));
        return wrapperDocument;
    }

    public WrapperDocument wrap(Object object, Class<?> type, String orgId, String documentId) {
        return (new WrapperDocument(documentId, type.getCanonicalName(), JSON.parse(Unchecked.function(objectMapper::writeValueAsString).apply(object)), orgId));
    }

    public WrapperDocument wrap(Object object, Class<?> type, String orgId) {
        return wrap(object, type, orgId, null);
    }
}
