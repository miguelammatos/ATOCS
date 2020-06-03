package atocs.core;

import soot.Unit;

import java.util.Objects;

public abstract class ConditionalStmt {
    private StackStmt stackStmt;
    private Unit target;

    public ConditionalStmt(StackStmt stackStmt, Unit target) {
        this.stackStmt = stackStmt;
        this.target = target;
    }

    public StackStmt getStackStmt() {
        return stackStmt;
    }

    public Unit getTarget() { // Can be null
        return target;
    }

    void setTarget(Unit target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionalStmt that = (ConditionalStmt) o;
        return Objects.equals(getStackStmt(), that.getStackStmt()) &&
                Objects.equals(getTarget(), that.getTarget());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStackStmt(), getTarget());
    }
}
