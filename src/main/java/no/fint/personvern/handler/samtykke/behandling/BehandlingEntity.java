package no.fint.personvern.handler.samtykke.behandling;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
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
@Entity(name = "Behandling")
public class BehandlingEntity {

    @Id
    private String id;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private BehandlingResource resource;

    private String orgId;

    private LocalDateTime lastModifiedDate;
}
