package atocs.core.api;

import soot.Type;

import java.util.List;

public class MethodInfo {
    private final String operation;
    private final List<Type> arguments;

    MethodInfo(String operation, List<Type> arguments) {
        this.operation = operation;
        this.arguments = arguments;
    }

    String getOperation() {
        return this.operation;
    }

    List<Type> getArguments() {
        return arguments;
    }

}
