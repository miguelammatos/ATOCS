package pt.ulisboa.tecnico.atocs.core.datastructures;

import soot.SootMethod;
import soot.jimple.StaticFieldRef;
import pt.ulisboa.tecnico.atocs.core.Stack;
import pt.ulisboa.tecnico.atocs.core.StackStmt;

import java.util.List;

/**
 * Defines the state of a static field reference value. This is also known as an Enum value.
 */
public class StaticFieldRefState extends FieldRefState {

    public StaticFieldRefState(StaticFieldRef staticFieldRef, SootMethod scopeMethod, Stack stack,
                               List<ValueState> scopeMethodParamValues, List<SootMethod> methodChain,
                               StackStmt currentStmt, List<InvokeExprState> paramMethodInvocations) {
        super(staticFieldRef, scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt,
                paramMethodInvocations);
    }

    @Override
    public StaticFieldRef getValue() {
        return (StaticFieldRef) super.getValue();
    }

}
