package no.fint.personvern.handler.samtykke.tjeneste;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Slf4j
@Converter(autoApply = true)
public class TjenesteConverter implements AttributeConverter<TjenesteResource, String> {

    private ObjectMapper objectMapper;

    public TjenesteConverter() {
        objectMapper = new ObjectMapper();
    }

    public TjenesteConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(TjenesteResource resource) {
        if (resource == null) return null;

        try {
            return objectMapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            log.error("Error convertToDatabaseColumn: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public TjenesteResource convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim() == "") return null;

        try {
            return objectMapper.readValue(dbData, TjenesteResource.class);
        } catch (JsonProcessingException e) {
            log.error("Error in convertToEntityAttribute: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}