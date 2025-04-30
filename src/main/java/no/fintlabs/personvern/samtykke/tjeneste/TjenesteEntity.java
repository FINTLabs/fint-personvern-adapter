package no.fintlabs.personvern.samtykke.tjeneste;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "Tjeneste")
public class TjenesteEntity {

    @Id
    private String id;

    @Type(JsonType.class)
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
