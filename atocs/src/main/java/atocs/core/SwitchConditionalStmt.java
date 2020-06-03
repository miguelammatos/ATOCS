package atocs.core;

import soot.Unit;
import soot.jimple.SwitchStmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SwitchConditionalStmt extends ConditionalStmt {
    private List<SwitchConditionalStmt> dependents = new ArrayList<>();
    private boolean isDefaultCase;

    SwitchConditionalStmt(StackStmt stackStmt, Unit target, SwitchStmt switchStmt) {
        super(stackStmt, target);
        this.isDefaultCase = switchStmt.getDefaultTarget().equals(stackStmt.getStmt());
    }

    public List<SwitchConditionalStmt> getDependents() {
        return dependents;
    }

    void addNewDependent(SwitchConditionalStmt conditionalStmt) {
        dependents.add(conditionalStmt);
    }

    void addAllDependents(List<SwitchConditionalStmt> conditionalStmts) {
        for (SwitchConditionalStmt condStmt : conditionalStmts) {
            if (!this.equals(condStmt)){
                addNewDependent(condStmt);
            }
        }
    }

    boolean isDefaultCase() {
        return isDefaultCase;
    }

    public boolean hasDefaultDependent() {
        if (isDefaultCase && getTarget() != null)
            return true;
        for (SwitchConditionalStmt condStmt : dependents) {
            if (condStmt.isDefaultCase() && condStmt.getTarget() != null)
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SwitchConditionalStmt that = (SwitchConditionalStmt) o;
        return isDefaultCase() == that.isDefaultCase();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isDefaultCase());
    }
}
