package pt.ulisboa.tecnico.atocs.core.exceptions;

public abstract class SystemException extends Exception {
    private String message;

    public SystemException(String message) {
        this.message = "ERROR: " + message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
