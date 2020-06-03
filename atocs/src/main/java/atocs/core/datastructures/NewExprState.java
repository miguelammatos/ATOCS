package atocs.core.datastructures;

import soot.SootMethod;
import soot.jimple.NewExpr;
import atocs.core.Stack;
import atocs.core.StackStmt;

import java.util.List;

public class NewExprState extends ValueState {
    private final NewExpr newExpr;

    public NewExprState(NewExpr newExpr, SootMethod scopeMethod, Stack stack,
                        List<ValueState> scopeMethodParamValues, List<SootMethod> methodChain, StackStmt currentStmt,
                        List<InvokeExprState> paramMethodInvocations) {
        super(scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt, paramMethodInvocations);
        this.newExpr = newExpr;
    }

    @Override
    public NewExpr getValue() {
        return newExpr;
    }

}

