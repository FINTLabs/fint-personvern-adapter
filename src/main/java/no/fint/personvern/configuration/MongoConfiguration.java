package no.fint.personvern.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfiguration {

    @Bean
    public String collection(@Value("${spring.data.mongodb.collection:samtykke}") final String name) {
        return name;
    }
}
