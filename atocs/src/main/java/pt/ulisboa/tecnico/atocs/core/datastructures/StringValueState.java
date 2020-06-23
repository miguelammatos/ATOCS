package pt.ulisboa.tecnico.atocs.core.datastructures;

import soot.Value;

/**
 * Defines the state of a String value. It also holds the Value from where the string was obtained.
 */
public class StringValueState extends ValueState {
    private final String stringValue;
    private final Value value;

    public StringValueState(String stringValue, ValueState valueState) {
        super(valueState.getScopeMethod(), valueState.getStack(),
                valueState.getScopeMethodParamValues(), valueState.getMethodChain(),
                valueState.getStack().getCurrentStmt(), valueState.getParamMethodInvocations());
        this.stringValue = stringValue;
        this.value = valueState.getValue();
    }

    public String getStringValue() {
        return stringValue;
    }

    @Override
    public Value getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof StringValueState)) {
            return false;
        }

        StringValueState stringValueState = (StringValueState) other;

        return this.getStringValue().equals(stringValueState.getStringValue());
    }
}
