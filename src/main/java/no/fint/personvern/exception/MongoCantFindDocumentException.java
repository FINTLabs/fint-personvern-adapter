package no.fint.personvern.exception;

public class MongoCantFindDocumentException extends RuntimeException {

    public MongoCantFindDocumentException() {
        super();
    }

    public MongoCantFindDocumentException(String message) {
        super(message);
    }
}
