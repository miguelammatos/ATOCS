package pt.ulisboa.tecnico.atocs.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import pt.ulisboa.tecnico.atocs.core.CodeAnalyser;
import pt.ulisboa.tecnico.atocs.core.Parser;
import pt.ulisboa.tecnico.atocs.core.datastructures.InvokeExprState;
import pt.ulisboa.tecnico.atocs.core.exceptions.SystemException;

import java.util.*;

public class API {
    private final Logger logger = LoggerFactory.getLogger(API.class);

    private static API instance;
    private final Map<SootMethod, MethodInfo> methodsMap = new HashMap<>();

    private API() {}

    public static API getInstance() {
        if (instance == null)
            instance = new API();
        return instance;
    }

    public void init(String apiFile) throws SystemException {
        Parser.getInstance().parseApiFile(apiFile);
    }

    public Set<SootMethod> getMethods() {
        return this.methodsMap.keySet();
    }

    public String getMethodOperation(SootMethod method) {
        return methodsMap.get(method).getOperation();
    }

    public String getMethodOperation(InvokeExprState invokeExprState) {
        return getMethodOperation(getCorrespondingApiMethod(invokeExprState));
    }

    public boolean isApiMethod(SootMethod sootMethod) {
        for (SootMethod apiMethod : getMethods()) {
            if (CodeAnalyser.isMethodEqualToOrOverrides(sootMethod, apiMethod))
                return true;
        }
        return false;
    }

    SootMethod getCorrespondingApiMethod(InvokeExprState invokeExprState) {
        SootMethod sootMethod = invokeExprState.getValue().getMethod();
        if (getMethods().contains(sootMethod))
            return sootMethod;
        else {
            for (SootMethod apiMethod : getMethods()) {
                if (CodeAnalyser.isMethodEqualToOrOverrides(sootMethod, apiMethod))
                    return apiMethod;
            }
        }
        throw new RuntimeException("No API method found from method invocation " + invokeExprState.getValue());
    }

    /**
     * Adds a new method to the API.
     *
     * @param className name of the declaring class of the method.
     * @param methodName name of the method.
     * @param arguments list of argument types of the method.
     * @param operation specifies the type of database operation performed by the method.
     */
    public void addMethod(String className, String methodName, List<Type> arguments, String operation) {
        try {
            MethodInfo methodInfo = new MethodInfo(operation, arguments);
            SootMethod method = Scene.v().getSootClass(className).getMethod(methodName, arguments);
            methodsMap.put(method, methodInfo);
        } catch (RuntimeException e) {
            logger.error("Unable to create SootMethod from API method " + className + ":" + methodName);
        }
    }

    public static String getMethodKey(String className, String methodName) {
        return className + ":" + methodName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(SootMethod m : methodsMap.keySet()) {
            sb.append("METHOD: ").append(m.getSignature()).append("\n");
            sb.append("ArgList:" + "\n");
            for (Type arg : methodsMap.get(m).getArguments()) {
                sb.append("\t\tArg: ").append(arg.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}
