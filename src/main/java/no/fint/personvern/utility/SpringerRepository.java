package no.fint.personvern.utility;

import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.resource.FintLinks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.util.StreamUtils;

import java.util.stream.Stream;

public abstract class SpringerRepository {

    @Autowired
    protected Wrapper wrapper;

    protected void query(Class<? extends FintLinks> type, Event<FintLinks> response, MongoTemplate mongoTemplate, String orgId) {
        stream(type, mongoTemplate, orgId).map(wrapper.unwrapper(type)).forEach(response::addData);
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    protected Stream<Springer> stream(Class<? extends FintLinks> type, MongoTemplate mongoTemplate, String orgId) {
        return StreamUtils
                .createStreamFromIterator(mongoTemplate.stream(wrapper.query(type), Springer.class, orgId));
    }
}
