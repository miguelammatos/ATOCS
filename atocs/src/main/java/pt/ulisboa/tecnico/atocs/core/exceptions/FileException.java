package pt.ulisboa.tecnico.atocs.core.exceptions;

public class FileException extends SystemException {
    public FileException(String fileName) {
        super(fileName + " not found");
    }
}
