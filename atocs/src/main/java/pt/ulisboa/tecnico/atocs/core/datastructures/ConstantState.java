package pt.ulisboa.tecnico.atocs.core.datastructures;

import soot.SootMethod;
import soot.jimple.Constant;
import pt.ulisboa.tecnico.atocs.core.Stack;
import pt.ulisboa.tecnico.atocs.core.StackStmt;

import java.util.List;

/**
 * Defines the state of a constant value.
 */
public class ConstantState extends ValueState {
    private final Constant constant;

    public ConstantState(Constant constant, SootMethod scopeMethod, Stack stack,
                         List<ValueState> scopeMethodParamValues, List<SootMethod> methodChain, StackStmt currentStmt,
                         List<InvokeExprState> paramMethodInvocations) {
        super(scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt, paramMethodInvocations);
        this.constant = constant;
    }

    @Override
    public Constant getValue() {
        return constant;
    }

}
