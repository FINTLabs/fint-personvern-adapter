// Built from tag personvern

package no.fint.model.personvern.samtykke;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.*;
import no.fint.model.FintMainObject;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import org.hibernate.validator.constraints.NotBlank;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Behandling implements FintMainObject {
    public enum Relasjonsnavn {
            PERSONOPPLYSNING,
            BEHANDLINGSGRUNNLAG,
            TJENESTE
    }

    @NotNull
    private Boolean aktiv;
    @NotBlank
    private String formal;
    @NotNull
    private @Valid
    Identifikator systemId;
}
