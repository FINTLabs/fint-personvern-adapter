package no.fint.personvern.handler.samtykke.behandling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "Behandling")
public class BehandlingEntity {

    @Id
    private String id;

    @Convert(converter = BehandlingConverter.class)
    @Column(name = "json_input", columnDefinition = "json")
    private BehandlingResource value;

    private String orgId;

    private LocalDateTime lastModifiedDate;
}
