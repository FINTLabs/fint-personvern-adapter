package no.fint.personvern.repository;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "#{@collection}")
public class WrapperDocument {

    @Id
    private String id;
    private String type;
    private Object value;
    private String orgId;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
