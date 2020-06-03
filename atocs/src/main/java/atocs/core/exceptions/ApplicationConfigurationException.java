package atocs.core.exceptions;

public class ApplicationConfigurationException extends SystemException {
    public ApplicationConfigurationException() {
        super("Configuration file not correct");
    }
}
