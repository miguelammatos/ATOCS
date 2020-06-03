package atocs.core.api;

import java.util.List;

public class MethodInfo {
    private final String operation;
    private final List<Argument> arguments;

    MethodInfo(String operation, List<Argument> arguments) {
        this.operation = operation;
        this.arguments = arguments;
    }

    String getOperation() {
        return this.operation;
    }

    List<Argument> getArguments() {
        return arguments;
    }

}
