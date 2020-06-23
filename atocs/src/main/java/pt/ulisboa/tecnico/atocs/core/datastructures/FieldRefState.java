package pt.ulisboa.tecnico.atocs.core.datastructures;

import soot.SootMethod;
import soot.jimple.FieldRef;
import pt.ulisboa.tecnico.atocs.core.Stack;
import pt.ulisboa.tecnico.atocs.core.StackStmt;

import java.util.List;

/**
 * Defines the state of a field reference value. This is also known as a class attribute.
 */
public class FieldRefState extends ValueState {
    private final FieldRef fieldRef;

    public FieldRefState(FieldRef fieldRef, SootMethod scopeMethod, Stack stack,
                         List<ValueState> scopeMethodParamValues, List<SootMethod> methodChain, StackStmt currentStmt,
                         List<InvokeExprState> paramMethodInvocations) {
        super(scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt, paramMethodInvocations);
        this.fieldRef = fieldRef;
    }

    @Override
    public FieldRef getValue() {
        return fieldRef;
    }

}
