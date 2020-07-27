package pt.ulisboa.tecnico.atocs.core;

import soot.Unit;

import java.util.Objects;

public class IfConditionalStmt extends ConditionalStmt {
    private IfConditionalStmt dependent;

    IfConditionalStmt(StackStmt stackStmt, Unit target) {
        super(stackStmt, target);
    }

    public ConditionalStmt getDependent() {
        return dependent;
    }

    void addNewDependent(IfConditionalStmt conditionalStmt) {
        this.dependent = conditionalStmt;
    }
}
