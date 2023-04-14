package no.fintlabs.personvern.samtykke.behandling.event;

import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Service
public class BehandlingResourceValidator implements Validator {

    public boolean supports(Class clazz) {
        return BehandlingResource.class.equals(clazz);
    }

    public void validate(Object obj, Errors e) {

    }
}