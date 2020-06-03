package atocs.core.datastructures;

import soot.SootMethod;
import soot.Value;
import atocs.core.Stack;
import atocs.core.StackStmt;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the state of a value. This value is not to be mistake by a variable. A value is assigned to a variable
 * and can be of many types.
 * The scopeMethod is the method where this value is inserted.
 * The stack is the state of the scopeMethod in the exact place where the value resides.
 * The scopeMethodParamValues are the ValueStates of the scopeMethod arguments in case the scope method was called
 * by another method.
 * The methodChain holds the methods that the value as passed through but only as method parameter. For example if the
 * value was used in method "a" but only declared in method "b", which the called method "a" passing the value as an
 * argument. Then both method "a" and method "b" will be in this value chain. However if the value was obtained from the
 * return of another method "c", this method "c" would not be in the chain since the value was not used a method
 * parameter.
 * The tags can be used by the DatabaseModule to add additional information to a ValueState.
 * The paramMethodInvocations are the method invocations analysed in order to obtain the value of a certain method
 * parameter.
 */
public abstract class ValueState {
    private final SootMethod scopeMethod;
    private Stack stack;
    private final List<ValueState> scopeMethodParamValues;
    private List<SootMethod> methodChain;
    private List<String> tags;
    private List<InvokeExprState> paramMethodInvocations;

    public ValueState(SootMethod scopeMethod, Stack stack, List<ValueState> scopeMethodParamValues,
                      List<SootMethod> methodChain, StackStmt currentStmt,
                      List<InvokeExprState> paramMethodInvocations) {
        this.scopeMethod = scopeMethod;
        this.stack = new Stack(stack);
        this.stack.setCurrentStmt(currentStmt);
        this.scopeMethodParamValues = scopeMethodParamValues;
        this.methodChain = methodChain != null ? new ArrayList<>(methodChain) : new ArrayList<>();
        this.tags = new ArrayList<>();
        this.paramMethodInvocations = paramMethodInvocations != null ?
                new ArrayList<>(paramMethodInvocations) : new ArrayList<>();
    }

    public abstract Value getValue();

    public SootMethod getScopeMethod() {
        return scopeMethod;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public List<ValueState> getScopeMethodParamValues() {
        return scopeMethodParamValues;
    }

    public ValueState getScopeMethodParamValue(int index) {
        if (!hasScopeMethodParamValues())
            throw new RuntimeException("ValueState does not have any scope method parameter values.");
        else if (scopeMethodParamValues.size() <= index)
            throw new RuntimeException("Index out of bounds in ValueState scope method parameter values.");
        return scopeMethodParamValues.get(index);
    }

    public boolean hasScopeMethodParamValues() {
        return !(scopeMethodParamValues == null || scopeMethodParamValues.isEmpty());
    }

    public void addMethodToChain(SootMethod method) {
        methodChain.add(method);
    }

    public List<SootMethod> getMethodChain() {
        return methodChain;
    }

    public String getValueClassName() {
        return getValue().getType().toString();
    }

    public boolean methodChainIntersectsWith(ValueState otherValue) {
        List<SootMethod> otherChain = otherValue.getMethodChain();
        if (methodChain.isEmpty() || otherChain.isEmpty())
            return true;
        for (int i=0; i<Math.min(methodChain.size(), otherChain.size()); i++) {
            if (methodChain.get(i) != otherChain.get(i))
                return false;
        }
        return true;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public List<InvokeExprState> getParamMethodInvocations() {
        return paramMethodInvocations;
    }

    public InvokeExprState getLastParamMethodInvocations() {
        return paramMethodInvocations.isEmpty() ? null : paramMethodInvocations.get(paramMethodInvocations.size()-1);
    }

    public void addParamMethodInvocation(InvokeExprState invokeExprState) {
        paramMethodInvocations.add(invokeExprState);
    }

    public boolean hasParamMethodInvocations() {
        return !paramMethodInvocations.isEmpty();
    }

    @Override
    public String toString() {
        return "ValueState{" +
                "value=" + getValue() +
                "scopeMethod=" + scopeMethod +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ValueState)) {
            return false;
        }

        ValueState valueState = (ValueState) other;

        return this.getValue().equivTo(valueState.getValue())
                && this.getScopeMethod().equals(valueState.getScopeMethod())
                && this.methodChainIntersectsWith(valueState);
    }
}
