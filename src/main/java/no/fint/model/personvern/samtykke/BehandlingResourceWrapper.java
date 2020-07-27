package no.fint.model.personvern.samtykke;

import lombok.Data;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class BehandlingResourceWrapper extends BehandlingResource {
    @Id private String id;
}
