package no.fint.personvern.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.util.JSON;
import org.jooq.lambda.Unchecked;
import org.springframework.stereotype.Service;

@Service
public class Wrapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
