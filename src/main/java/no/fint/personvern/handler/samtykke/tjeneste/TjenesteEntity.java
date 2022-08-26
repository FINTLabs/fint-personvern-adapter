package no.fint.personvern.handler.samtykke.tjeneste;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "Tjeneste")
public class TjenesteEntity {

    @Id
    private String id;

    @Column(name = "json_input", columnDefinition = "json")
    private TjenesteResource value;

    private String orgId;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
