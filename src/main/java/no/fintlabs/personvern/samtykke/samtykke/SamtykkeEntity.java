package no.fintlabs.personvern.samtykke.samtykke;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
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
@Entity(name = "Samtykke")
public class SamtykkeEntity {

    @Id
    private String id;

    @Type(JsonType.class)
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
