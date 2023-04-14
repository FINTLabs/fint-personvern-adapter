package no.fintlabs.personvern.samtykke.tjeneste.event;

import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Service
public class TjenesteResourceValidator implements Validator {

    public boolean supports(Class clazz) {
        return SamtykkeResource.class.equals(clazz);
    }

    public void validate(Object obj, Errors e) {
    }
}