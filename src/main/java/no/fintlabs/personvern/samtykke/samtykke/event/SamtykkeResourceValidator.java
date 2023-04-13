package no.fintlabs.personvern.samtykke.samtykke.event;

import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Service
public class SamtykkeResourceValidator implements Validator {

    public boolean supports(Class clazz) {
        return SamtykkeResource.class.equals(clazz);
    }

    public void validate(Object obj, Errors e) {
        ValidationUtils.rejectIfEmpty(e, "gyldighetsperiode", "gyldighetsperiode.empty");
    }
}