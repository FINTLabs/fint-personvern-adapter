package no.fint.model.personvern.samtykke;

import lombok.Data;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class SamtykkeResourceWrapper extends SamtykkeResource {
    @Id private String id;
}
