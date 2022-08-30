package no.fint.personvern.exception;

public class RowNotFoundException extends RuntimeException {

    public RowNotFoundException() {
        super();
    }

    public RowNotFoundException(String message) {
        super(message);
    }
}
