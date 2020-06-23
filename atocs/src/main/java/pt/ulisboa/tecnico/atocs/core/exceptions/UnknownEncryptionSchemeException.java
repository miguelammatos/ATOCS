package pt.ulisboa.tecnico.atocs.core.exceptions;

public class UnknownEncryptionSchemeException extends SystemException {
    public UnknownEncryptionSchemeException(String cipher) {
        super("Unknown encryption scheme: " + cipher);
    }
}
