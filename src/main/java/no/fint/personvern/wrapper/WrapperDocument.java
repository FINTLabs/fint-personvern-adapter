package no.fint.personvern.wrapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
public class WrapperDocument {

    @Id
    private String id;
    private String type;
    private Object value;
    private String orgId;
}
