package pt.ulisboa.tecnico.atocs.core;

import pt.ulisboa.tecnico.atocs.core.api.API;
import pt.ulisboa.tecnico.atocs.core.datastructures.InvokeExprState;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DatabasePlugin {
    public API api;

    public void setApi() {
        api = API.getInstance();
    }

    public abstract void analyseDbInteraction(InvokeExprState invokeExprState);

    public abstract String getApiFilePath();

    public abstract List<String> getLibPaths();

    public abstract void removeOverlappingRequirements(Map<String, Map<DbField, List<Requirement>>> requirementsMap);

    public abstract void removeOverlappingObtainedFields(Map<String, Set<DbField>> obtainedFields);
}
