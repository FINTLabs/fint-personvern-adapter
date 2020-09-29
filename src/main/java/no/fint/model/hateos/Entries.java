package no.fint.model.hateos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.fint.model.metamodell.Klasse;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "_entries"
})
public class Entries {

    @JsonProperty("_entries")
    private List<Klasse> entries = null;

    @JsonProperty("_entries")
    public List<Klasse> getEntries() {
        return entries;
    }

    @JsonProperty("_entries")
    public void setEntries(List<Klasse> entries) {
        this.entries = entries;
    }


}