package no.fintlabs.personvern.samtykke.behandling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "Behandling")
public class BehandlingEntity {

    @Id
    private String id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private BehandlingResource resource;

    private String orgId;

    private LocalDateTime lastModifiedDate;

    public static BehandlingEntity toEntity(BehandlingResource behandlingResource, String orgId) {
        return BehandlingEntity
                .builder()
                .id(behandlingResource.getSystemId().getIdentifikatorverdi())
                .resource(behandlingResource)
                .orgId(orgId)
                .lastModifiedDate(LocalDateTime.now())
                .build();
    }
}
