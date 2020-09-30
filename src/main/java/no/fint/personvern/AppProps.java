package no.fint.personvern;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppProps {

    @Value("${spring.data.mongodb.database:personvern}")
    private String databaseCollection;

    @Value("${fint.metamodell.uri:https://beta.felleskomponent.no/fint/metamodell/klasse}")
    private String metamodellUri;

}
