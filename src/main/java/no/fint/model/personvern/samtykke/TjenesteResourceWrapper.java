package no.fint.model.personvern.samtykke;

import lombok.Data;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class TjenesteResourceWrapper extends TjenesteResource {
    @Id private String id;
}
