package no.fint.personvern.exception;

public class MongoEntryExistsException extends RuntimeException {

    public MongoEntryExistsException() {
        super();
    }

    public MongoEntryExistsException(String message) {
        super(message);
    }
}
