package no.fintlabs.personvern.samtykke.tjeneste;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


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

    @JdbcTypeCode(SqlTypes.JSON)
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
