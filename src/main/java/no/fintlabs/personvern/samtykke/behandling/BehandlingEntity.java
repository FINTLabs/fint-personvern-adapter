package no.fintlabs.personvern.samtykke.behandling;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fintlabs.personvern.samtykke.samtykke.SamtykkeEntity;
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
@Entity(name = "Behandling")
public class BehandlingEntity {

    @Id
    private String id;

    @Type(type = "json")
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
