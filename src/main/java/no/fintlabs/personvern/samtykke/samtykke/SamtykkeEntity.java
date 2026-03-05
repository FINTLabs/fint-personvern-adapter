package no.fintlabs.personvern.samtykke.samtykke;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


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

    @JdbcTypeCode(SqlTypes.JSON)
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
