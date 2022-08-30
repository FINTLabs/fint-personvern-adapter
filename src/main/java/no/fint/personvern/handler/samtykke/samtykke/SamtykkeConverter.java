package no.fint.personvern.handler.samtykke.samtykke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Slf4j
@Converter(autoApply = true)
public class SamtykkeConverter implements AttributeConverter<SamtykkeResource, String> {

    private ObjectMapper objectMapper;

    public SamtykkeConverter() {
        objectMapper = new ObjectMapper();
    }

    public SamtykkeConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(SamtykkeResource resource) {
        if (resource == null) return null;

        try {
            return objectMapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            log.error("Error convertToDatabaseColumn: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public SamtykkeResource convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim() == "") return null;

        try {
            return objectMapper.readValue(dbData, SamtykkeResource.class);
        } catch (JsonProcessingException e) {
            log.error("Error in convertToEntityAttribute: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
