package pt.ulisboa.tecnico.atocs.core.exceptions;

public class ReportException extends SystemException {
    public ReportException() {
        super("Error while writing the final report.");
    }
}