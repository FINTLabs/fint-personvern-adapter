// Built from tag personvern

package no.fint.model.personvern.kodeverk;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import no.fint.model.FintMainObject;
import no.fint.model.felles.basisklasser.Begrep;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Behandlingsgrunnlag extends Begrep implements FintMainObject {
}
