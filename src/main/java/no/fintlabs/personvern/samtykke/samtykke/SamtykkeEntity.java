package no.fintlabs.personvern.samtykke.samtykke;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
@Entity(name = "Samtykke")
public class SamtykkeEntity {

    @Id
    private String id;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private SamtykkeResource resource;

    private String orgId;

    private LocalDateTime lastModifiedDate;

    public static SamtykkeEntity toEntity(SamtykkeResource samtykkeResource, String orgId) {
        return SamtykkeEntity
                .builder()
                .id(samtykkeResource.getSystemId().getIdentifikatorverdi())
                .resource(samtykkeResource)
                .orgId(orgId)
                .lastModifiedDate(LocalDateTime.now())
                .build();
    }
}
