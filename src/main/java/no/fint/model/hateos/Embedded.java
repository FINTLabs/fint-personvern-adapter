package no.fint.model.hateos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "_embedded"
})
public class Embedded {

    @JsonProperty("_embedded")
    private Entries entries;

    @JsonProperty("_embedded")
    public Entries getEmbedded() {
        return entries;
    }

    @JsonProperty("_embedded")
    public void setEmbedded(Entries entries) {
        this.entries = entries;
    }

}