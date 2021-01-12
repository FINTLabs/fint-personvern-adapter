package no.fint.personvern.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@Document(collection = "personvernBeta")
public class WrapperDocument {

    @Id
    private String id;
    private String type;
    private Object value;
    private String orgId;
}
