package pt.ulisboa.tecnico.atocs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;

import java.util.*;

public class Stack {
    private final Logger logger = LoggerFactory.getLogger(Stack.class);

    private List<StackStmt> statements;
    private List<ConditionalStmt> conditionalStmts = new ArrayList<>();
    private final SootMethod scopeMethod;
    private StackStmt currentStmt;

    Stack(SootMethod scopeMethod) {
        this.statements = new ArrayList<>();
        this.scopeMethod = scopeMethod;
    }

    public Stack(Stack other) {
        this.statements = new ArrayList<>(other.getStatements());
        this.conditionalStmts = new ArrayList<>(other.getConditionalStmts());
        this.scopeMethod = other.getScopeMethod();
        this.currentStmt = other.getCurrentStmt();
    }

    public void setCurrentStmt(StackStmt currentStmt) {
        if (!statements.contains(currentStmt))
            logger.error("Current statement does not exist in the stack.");
        else
            this.currentStmt = currentStmt;
    }

    public StackStmt getCurrentStmt() {
        return currentStmt;
    }

    StackStmt getLastestStmt() {
        return statements.isEmpty() ? null : statements.get(statements.size() - 1);
    }

    SootMethod getScopeMethod() {
        return scopeMethod;
    }

    List<StackStmt> getStatements() {
        return statements;
    }

    void addPrevious(StackStmt stackStmt) {
        statements.add(stackStmt);
    }

    void addNew(StackStmt stackStmt) {
        stackStmt.setConditionalStmts(this.conditionalStmts);
        statements.add(stackStmt);
        this.currentStmt = stackStmt;
    }

    void addAllPrevious(List<StackStmt> stackStmts) {
        statements.addAll(stackStmts);
    }

    boolean contains(StackStmt stackStmt) {
        return statements.contains(stackStmt);
    }

    List<ConditionalStmt> getConditionalStmts() {
        return conditionalStmts;
    }

    void setConditionalStmts(List<ConditionalStmt> conditionalStmts) {
        this.conditionalStmts = new ArrayList<>(conditionalStmts);
    }

    ConditionalStmt getFirstConditionalStmtWithTarget(Unit target) {
        for (ConditionalStmt conditionalStmt : conditionalStmts) {
            if (target.equals(conditionalStmt.getTarget()))
                return conditionalStmt;
        }
        return null;
    }

    void addConditionalStmt(ConditionalStmt conditionalStmt) {
        if (!conditionalStmts.contains(conditionalStmt))
            conditionalStmts.add(conditionalStmt);
    }

    /**
     *
     * @param unit
     * @return true if removed an item, false otherwise.
     */
    boolean removeCondStmtIfUnitEqTarget(Unit unit) {
        int indexToRemove = -1;
        for (int i=0; i < conditionalStmts.size(); i++) {
            ConditionalStmt condStmt = conditionalStmts.get(i);
            if (unit.equals(condStmt.getTarget())) {
                indexToRemove = conditionalStmts.indexOf(condStmt);
            }
        }
        if (indexToRemove == -1) return false;
        if (indexToRemove == conditionalStmts.size()-1) {
            conditionalStmts.remove(indexToRemove);
        } else {
            Unit target = conditionalStmts.get(indexToRemove).getTarget();
            for(int i=conditionalStmts.size()-1; i >= indexToRemove; i--) {
                ConditionalStmt condStmt = conditionalStmts.get(i);
                condStmt.setTarget(target);
                conditionalStmts.remove(condStmt);
            }
        }
        return true;
    }

    void mergeConditionalStmts(List<ConditionalStmt> conditionalStmts) {
        if (this.conditionalStmts.isEmpty())
            setConditionalStmts(conditionalStmts);
        else {
            for (ConditionalStmt newConditionalStmt : conditionalStmts) {
                if (!this.conditionalStmts.contains(newConditionalStmt)) {
                    this.conditionalStmts.add(newConditionalStmt);
                }
            }
        }
    }

    public List<ConditionalStmt> getCurrentConditionalStmts() {
        if (currentStmt != null)
            return currentStmt.getConditionalStmts();
        else
            return conditionalStmts;
    }

    public boolean currentCondStmtsContains(ConditionalStmt conditionalStmt) {
        return getCurrentConditionalStmts().contains(conditionalStmt);
    }

    public List<ConditionalStmt> getCurrentConditionalStmtsAfter(List<ConditionalStmt> after) {
        if (after.isEmpty())
            return getCurrentConditionalStmts();
        return getCurrentConditionalStmtsAfter(after.get(after.size() - 1));
    }

    List<ConditionalStmt> getCurrentConditionalStmtsAfter(ConditionalStmt last) {
        List<ConditionalStmt> conditionalStmts = getCurrentConditionalStmts();
        int lastIndex = conditionalStmts.indexOf(last);
        if (lastIndex == -1)
            return new ArrayList<>();
        return conditionalStmts.subList(lastIndex + 1, conditionalStmts.size());
    }

    public ConditionalStmt getSingleCurrentConditionalStmtsAfter(ConditionalStmt last) {
        List<ConditionalStmt> conditionalStmts = getCurrentConditionalStmtsAfter(last);
        return conditionalStmts.isEmpty() ? null : conditionalStmts.get(0);
    }

    public ConditionalStmt getCurrentLastConditionalStmt() {
        List<ConditionalStmt> conditionalStmts = getCurrentConditionalStmts();
        if (conditionalStmts.isEmpty())
            return null;
        else
            return conditionalStmts.get(conditionalStmts.size()-1);
    }

    public int currentNumConditionalStmts() {
        if (currentStmt != null)
            return currentStmt.numConditionalStmts();
        else
            return conditionalStmts.size();
    }

    public boolean currentlyInsideConditionalStmt() {
        if (currentStmt != null)
            return currentStmt.insideConditionalStmt();
        else
            return !conditionalStmts.isEmpty();
    }

    public boolean intersectConditionalStmts(Stack stack) {
        List<ConditionalStmt> myConditionalStmts = getCurrentConditionalStmts();
        List<ConditionalStmt> otherConditionalStmts = stack.getCurrentConditionalStmts();

        for (int i=0; i < Math.min(myConditionalStmts.size(), otherConditionalStmts.size()); i++) {
            if (!myConditionalStmts.get(i).equals(otherConditionalStmts.get(i)))
                return false;
        }
        return true;
    }

    public ConditionalStmt getCondStmtEqualToDependent(List<ConditionalStmt> dependents) {
        for (ConditionalStmt conditionalStmt : getCurrentConditionalStmts()) {
            if (dependents.contains(conditionalStmt))
                return conditionalStmt;
        }
        return null;
    }

    public ConditionalStmt getCondStmtEqualToDependent(ConditionalStmt dependent) {
        for (ConditionalStmt conditionalStmt : getCurrentConditionalStmts()) {
            if (dependent.equals(conditionalStmt))
                return conditionalStmt;
        }
        return null;
    }

    public boolean hasCondStmtEqualTo(ConditionalStmt dependent) {
        for (ConditionalStmt conditionalStmt : getCurrentConditionalStmts()) {
            if (dependent.equals(conditionalStmt))
                return true;
        }
        return false;
    }

    public ConditionalStmt getLastCondStmtsIntersect(List<ConditionalStmt> otherConditionalStmts) {
        List<ConditionalStmt> myConditionalStmts = getCurrentConditionalStmts();
        for (int i=0; i < Math.min(myConditionalStmts.size(), otherConditionalStmts.size()); i++) {
            if (!myConditionalStmts.get(i).equals(otherConditionalStmts.get(i))) {
                if (i-1 >= 0)
                    return myConditionalStmts.get(i-1);
                break;
            }
        }
        return null;
    }

}
