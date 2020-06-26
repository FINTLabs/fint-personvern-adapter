package no.fint.personvern.exception;

public class CollectionNotFoundException extends RuntimeException {

    public CollectionNotFoundException() {
        super();
    }

    public CollectionNotFoundException(String message) {
        super(message);
    }
}
