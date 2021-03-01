package no.fint.personvern.service;

import no.fint.event.model.Problem;
import no.fint.model.resource.FintLinks;
import org.springframework.stereotype.Service;

import javax.validation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ValidationService {
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    public <T extends FintLinks> List<Problem> getProblems(T resource) {
        return validator.validate(resource)
                .stream()
                .map(this::getProblem)
                .collect(Collectors.toList());
    }

    private <T extends FintLinks> Problem getProblem(ConstraintViolation<T> violation) {
        Problem problem = new Problem();

        Optional.ofNullable(violation.getPropertyPath()).map(Path::toString).ifPresent(problem::setField);
        Optional.ofNullable(violation.getMessage()).ifPresent(problem::setMessage);

        return problem;
    }
}