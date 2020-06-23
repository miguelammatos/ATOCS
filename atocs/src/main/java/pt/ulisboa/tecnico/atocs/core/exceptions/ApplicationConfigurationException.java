package pt.ulisboa.tecnico.atocs.core.exceptions;

public class ApplicationConfigurationException extends SystemException {
    public ApplicationConfigurationException(String message) {
        super("Configuration file not correct: " + message);
    }
}
