package no.fintlabs.personvern.samtykke.tjeneste;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fintlabs.personvern.samtykke.samtykke.SamtykkeEntity;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
@Entity(name = "Tjeneste")
public class TjenesteEntity {

    @Id
    private String id;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private TjenesteResource resource;

    private String orgId;

    private LocalDateTime lastModifiedDate;

    public static TjenesteEntity toEntity(TjenesteResource tjenesteResource, String orgId) {
        return TjenesteEntity
                .builder()
                .id(tjenesteResource.getSystemId().getIdentifikatorverdi())
                .resource(tjenesteResource)
                .orgId(orgId)
                .lastModifiedDate(LocalDateTime.now())
                .build();
    }
}
