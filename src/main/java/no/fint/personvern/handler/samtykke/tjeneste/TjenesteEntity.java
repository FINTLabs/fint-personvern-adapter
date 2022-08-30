package no.fint.personvern.handler.samtykke.tjeneste;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;

import javax.persistence.Column;
import javax.persistence.Convert;
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

    @Convert(converter = TjenesteConverter.class)
    @Column(columnDefinition = "json")
    private TjenesteResource resource;

    private String orgId;

    private LocalDateTime lastModifiedDate;
}
