package no.fint.personvern.handler.samtykke.samtykke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;

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
    @Column(columnDefinition = "json")
    private SamtykkeResource resource;

    private String orgId;

    private LocalDateTime lastModifiedDate;
}
