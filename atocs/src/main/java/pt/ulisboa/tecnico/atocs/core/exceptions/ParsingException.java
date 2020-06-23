package pt.ulisboa.tecnico.atocs.core.exceptions;

public class ParsingException extends SystemException {
    public ParsingException(String fileName) {
        super("parsing " + fileName);
    }
}
