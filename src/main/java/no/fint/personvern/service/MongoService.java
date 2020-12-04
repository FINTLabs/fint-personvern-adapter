package no.fint.personvern.service;

import com.mongodb.BasicDBObject;
import lombok.Getter;
import no.fint.personvern.AppProps;
import no.fint.personvern.wrapper.WrapperDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class MongoService {

    @Getter
    private final MongoTemplate mongoTemplate;
    @Getter
    private final AppProps appProps;

    public MongoService(MongoTemplate mongoTemplate, AppProps appProps) {
        this.mongoTemplate = mongoTemplate;
        this.appProps = appProps;
    }

    public void save(WrapperDocument object) {
        mongoTemplate.save(object, appProps.getDatabaseCollection());
    }

    public void insert(WrapperDocument object) {
        mongoTemplate.insert(object, appProps.getDatabaseCollection());
    }

    public boolean ping() {
            return mongoTemplate.getDb().command(new BasicDBObject("ping", "1")).ok();
    }

}
