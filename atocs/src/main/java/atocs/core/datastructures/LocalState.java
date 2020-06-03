package atocs.core.datastructures;

import soot.Local;
import soot.SootMethod;
import atocs.core.Stack;
import atocs.core.StackStmt;

import java.util.List;

/**
 * Defines the state of a local value. This is the only value that can be considered a variable. This is a method
 * local variable.
 */
public class LocalState extends ValueState {
    private final Local local;

    public LocalState(Local local, SootMethod scopeMethod, Stack stack, List<ValueState> scopeMethodParamValues,
                      List<SootMethod> methodChain, StackStmt currentStmt,
                      List<InvokeExprState> paramMethodInvocations) {
        super(scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt, paramMethodInvocations);
        this.local = local;
    }

    @Override
    public Local getValue() {
        return local;
    }

}
