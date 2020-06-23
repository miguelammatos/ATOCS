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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IfConditionalStmt that = (IfConditionalStmt) o;
        return Objects.equals(getDependent().getStackStmt(), that.getDependent().getStackStmt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDependent().getStackStmt());
    }
}
