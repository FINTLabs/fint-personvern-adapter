package no.fint.personvern.utility;

import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.resource.FintLinks;
import no.fint.personvern.AppProps;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.util.StreamUtils;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
public abstract class WrapperDocumentRepository {

    protected final Wrapper wrapper;
    private final AppProps appProps;
    private final MongoTemplate mongoTemplate;

    protected WrapperDocumentRepository(Wrapper wrapper, AppProps appProps, MongoTemplate mongoTemplate) {
        this.wrapper = wrapper;
        this.appProps = appProps;
        this.mongoTemplate = mongoTemplate;
    }


    protected void query(Class<? extends FintLinks> type, Event<FintLinks> response, String orgId) {
        stream(type, orgId).map(wrapper.unwrapper(type)).forEach(response::addData);
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    protected Stream<WrapperDocument> stream(Class<? extends FintLinks> type, String orgId) {
        return StreamUtils
                .createStreamFromIterator(mongoTemplate.stream(wrapper.query(type, orgId), WrapperDocument.class, appProps.getDatabaseCollection()));
    }
}
