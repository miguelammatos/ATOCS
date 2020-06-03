package atocs.core.datastructures;

import soot.SootMethod;
import soot.jimple.ParameterRef;
import atocs.core.Stack;
import atocs.core.StackStmt;

import java.util.List;

/**
 * Defines the state of a parameter reference value. This is also known as a method argument. The scopeMethod corresponds
 * to the method which receives this parameter ref as argument and the stack refers to the body of the scopeMethod.
 */
public class ParameterRefState extends ValueState {
    private final ParameterRef parameterRef;

    public ParameterRefState(ParameterRef parameterRef, SootMethod scopeMethod, Stack stack,
                             List<ValueState> scopeMethodParamValues, List<SootMethod> methodChain,
                             StackStmt currentStmt, List<InvokeExprState> paramMethodInvocations) {
        super(scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt, paramMethodInvocations);
        this.parameterRef = parameterRef;
    }

    @Override
    public ParameterRef getValue() {
        return parameterRef;
    }

    public int getIndex() {
        return parameterRef.getIndex();
    }
}
