package pt.ulisboa.tecnico.atocs.core.exceptions;

public class DatabaseModuleNotFoundException extends SystemException {
    public DatabaseModuleNotFoundException(String database) {
        super(database + " is not a supported database");
    }
}
