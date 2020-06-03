package atocs.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import atocs.core.CodeAnalyser;
import atocs.core.Parser;
import atocs.core.datastructures.InvokeExprState;
import atocs.core.exceptions.SystemException;

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
     * @param argumentList list of arguments of the method.
     * @param operation specifies the type of database operation performed by the method.
     */
    public void addMethod(String className, String methodName, List<Argument> argumentList, String operation) {
        try {
            MethodInfo methodInfo = new MethodInfo(operation, argumentList);
            List<Type> argTypes = new ArrayList<>();
            for (Argument arg : argumentList)
                argTypes.add(arg.getType());

            SootMethod method = Scene.v().getSootClass(className).getMethod(methodName, argTypes);
            methodsMap.put(method, methodInfo);
        } catch (RuntimeException e) {
            logger.error("Unable to create SootMethod from API method " + className + ":" + methodName);
        }
    }

    public static String getMethodKey(String className, String methodName) {
        return className + ":" + methodName;
    }

    public static String getMethodKey(SootMethod sootMethod) {
        return getMethodKey(sootMethod.getDeclaringClass().getName(), sootMethod.getName());
    }

    public void printAPI() {
        for(SootMethod m : methodsMap.keySet()) {
            logger.debug("METHOD: " + m.getSignature());
            logger.debug("ArgList:");
            for (Argument arg : methodsMap.get(m).getArguments()) {
                logger.debug("\t\tType: " + arg.getType().toString());
                logger.debug("\t\tNote: " + arg.getTag());
            }
            logger.debug("");
        }
    }
}
