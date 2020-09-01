package no.fint.personvern.service;

import lombok.Getter;
import no.fint.personvern.AppProps;
import no.fint.personvern.utility.Springer;
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

    public void save(Springer object) {
        mongoTemplate.save(object, appProps.getDatabaseCollection());
    }

    public void insert(Springer object) {
        mongoTemplate.insert(object, appProps.getDatabaseCollection());
    }

}
