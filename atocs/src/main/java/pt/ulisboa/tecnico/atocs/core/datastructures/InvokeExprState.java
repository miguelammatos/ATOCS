package pt.ulisboa.tecnico.atocs.core.datastructures;

import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import pt.ulisboa.tecnico.atocs.core.CodeAnalyser;
import pt.ulisboa.tecnico.atocs.core.Stack;
import pt.ulisboa.tecnico.atocs.core.StackStmt;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the state of an invoke expression value. This represents the invocation of a method with certain
 * arguments.
 */
public class InvokeExprState extends ValueState {
    private final InvokeExpr invokeExpr;
    private final List<ValueState> argValues = new ArrayList<>();
    private final ValueState instance;

    public InvokeExprState(InvokeExpr invokeExpr, ValueState prevValue, StackStmt currentStmt) {
        this(invokeExpr, prevValue.getScopeMethod(), prevValue.getStack(), prevValue.getScopeMethodParamValues(),
                prevValue.getMethodChain(), currentStmt, prevValue.getParamMethodInvocations());
    }

    public InvokeExprState(InvokeExpr invokeExpr, SootMethod scopeMethod, Stack stack,
                           List<ValueState> scopeMethodParamValues, List<SootMethod> methodChain,
                           StackStmt currentStmt, List<InvokeExprState> paramMethodInvocations) {
        super(scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt, paramMethodInvocations);
        this.invokeExpr = invokeExpr;
        ValueState argState;
        for (Value arg : invokeExpr.getArgs()) {
            argState = CodeAnalyser.createValueState(arg, scopeMethod, stack, scopeMethodParamValues, methodChain,
                    currentStmt, paramMethodInvocations);
            if (argState == null)
                throw new RuntimeException("Unable to obtain value state form invoke expression argument.");
            argValues.add(argState);
        }
        this.instance = CodeAnalyser.getInvokeExprInstance(invokeExpr, scopeMethod, stack, scopeMethodParamValues,
                methodChain, currentStmt, paramMethodInvocations);
    }

    @Override
    public InvokeExpr getValue() {
        return invokeExpr;
    }

    public String getMethodName() {
        return invokeExpr.getMethod().getName();
    }

    public String getDeclaringClassName() {
        return invokeExpr.getMethod().getDeclaringClass().getName();
    }

    public int getArgCount() {
        return invokeExpr.getArgCount();
    }

    public List<ValueState> getArgs() {
        return argValues;
    }

    public ValueState getArg(int index) {
        if (invokeExpr.getArgCount() > index) {
            return argValues.get(index);
        }
        throw new RuntimeException("Unable to obtain argument " + index + " for method invocation " +
                invokeExpr.getMethod().getSignature());
    }

    public boolean hasInstance() {
        return instance != null;
    }

    public ValueState getInstance() {
        if (!hasInstance())
            throw new RuntimeException("Invoke expression does not have an instance: " + invokeExpr);
        return instance;
    }

    @Override
    public void addParamMethodInvocation(InvokeExprState invokeExprState) {
        super.addParamMethodInvocation(invokeExprState);
        instance.addParamMethodInvocation(invokeExprState);
        for (ValueState argValue : argValues) {
            argValue.addParamMethodInvocation(invokeExprState);
        }
    }

    private boolean equalArgValues(List<ValueState> otherArgValues) {
        if (argValues.size() != otherArgValues.size())
            return false;
        if (argValues.isEmpty())
            return true;
        for(int i=0; i<argValues.size(); i++) {
            if (!argValues.get(i).equals(otherArgValues.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof InvokeExprState)) {
            return false;
        }

        InvokeExprState invokeExprState = (InvokeExprState) other;

        return super.equals(other) && equalArgValues(invokeExprState.getArgs());
    }


}
