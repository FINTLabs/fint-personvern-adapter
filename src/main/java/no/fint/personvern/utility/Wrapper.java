package no.fint.personvern.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.util.JSON;
import org.jooq.lambda.Unchecked;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Wrapper {

    private ObjectMapper objectMapper = new ObjectMapper();

    public Function<Object, Springer> wrapper(Class<?> type, String orgId) {
        return Unchecked.function((Object content) -> new Springer(null, type.getCanonicalName(), JSON.parse(objectMapper.writeValueAsString(content)), orgId));
    }

    public <T> Function<Springer, T> unwrapper(Class<T> type) {
        return (Springer springer) -> objectMapper.convertValue(springer.getValue(), type);
    }

    public <T> Query query(Class<T> type, String orgId) {
        return new Query()
                .addCriteria(Criteria.where("type").is(type.getCanonicalName()))
                .addCriteria(Criteria.where("orgId").is(orgId));
    }

    public <T> Springer update(Springer springer, T content) {
        springer.setValue(JSON.parse(Unchecked.function(objectMapper::writeValueAsString).apply(content)));
        return springer;
    }

    public Springer wrap(Object object, Class<?> type, String orgId) {
        return (new Springer(null, type.getCanonicalName(), JSON.parse(Unchecked.function(objectMapper::writeValueAsString).apply(object)), orgId));
    }
}
