package atocs.core;

import soot.SootMethod;
import soot.jimple.Stmt;

import java.util.*;

public class StackStmt {
    private final Stmt stmt;
    private List<ConditionalStmt> conditionalStmts = new ArrayList<>();
    private final SootMethod scopeMethod;

    StackStmt(Stmt stmt, SootMethod scopeMethod) {
        this.stmt = stmt;
        this.scopeMethod = scopeMethod;
    }

    StackStmt(Stmt stmt, SootMethod scopeMethod, List<ConditionalStmt> conditionalStmts) {
        this.stmt = stmt;
        this.scopeMethod = scopeMethod;
        this.conditionalStmts = new ArrayList<>(conditionalStmts);
    }

    Stmt getStmt() {
        return stmt;
    }

    List<ConditionalStmt> getConditionalStmts() {
        return conditionalStmts;
    }

    SootMethod getScopeMethod() {
        return scopeMethod;
    }

    void setConditionalStmts(List<ConditionalStmt> conditionalStmts) {
        this.conditionalStmts = new ArrayList<>(conditionalStmts);
    }

    int numConditionalStmts() {
        return conditionalStmts.size();
    }

    boolean insideConditionalStmt() {
        return !conditionalStmts.isEmpty();
    }

    @Override
    public String toString() {
        return stmt.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StackStmt)) return false;
        StackStmt stackStmt = (StackStmt) o;
        return Objects.equals(getStmt(), stackStmt.getStmt()) &&
                Objects.equals(getScopeMethod(), stackStmt.getScopeMethod());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStmt(), getConditionalStmts(), getScopeMethod());
    }
}
