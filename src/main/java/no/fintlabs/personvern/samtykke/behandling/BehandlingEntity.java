package no.fintlabs.personvern.samtykke.behandling;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
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
@Entity(name = "Behandling")
public class BehandlingEntity {

    @Id
    private String id;

    @Type(JsonType.class)
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
