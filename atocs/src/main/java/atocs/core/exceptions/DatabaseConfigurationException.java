package atocs.core.exceptions;

public class DatabaseConfigurationException extends SystemException {
    public DatabaseConfigurationException(String database) {
        super(database + " API file path or library path not set correctly");
    }
}