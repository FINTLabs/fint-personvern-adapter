package no.fint.personvern.handler.samtykke.samtykke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.handler.samtykke.behandling.BehandlingConverter;
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
@Entity(name = "Samtykke")
public class SamtykkeEntity {

    @Id
    private String id;

    @Convert(converter = SamtykkeConverter.class)
    @Column(name = "json_input", columnDefinition = "json")
    private SamtykkeResource value;

    private String orgId;

    private LocalDateTime lastModifiedDate;
}
