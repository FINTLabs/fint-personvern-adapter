package no.fint.personvern.handler.samtykke.samtykke;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class)
})
@Entity(name = "Samtykke")
public class SamtykkeEntity {

    @Id
    private String id;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private SamtykkeResource resource;

    private String orgId;

    private LocalDateTime lastModifiedDate;
}
