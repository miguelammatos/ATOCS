package atocs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackManager {
    private final Logger logger = LoggerFactory.getLogger(StackManager.class);

    private SootMethod scopeMethod;
    private Map<Unit, Stack> unitsStack = new HashMap<>();
    private Unit last;
    private List<SwitchStmt> switchList = new ArrayList<>();
    private Map<SwitchStmt, List<SwitchConditionalStmt>> switchMap = new HashMap<>();
    private List<ConditionalStmt> startingCondStmts;

    StackManager(SootMethod scopeMethod){
        this.scopeMethod = scopeMethod;
    }

    Stack buildStack(SootMethod scopeMethod) {
        Stack stack = new Stack(scopeMethod);
        if (startingCondStmts != null)
            stack.setConditionalStmts(startingCondStmts);
        return stack;
    }

    void setStartingCondStmts(List<ConditionalStmt> startingCondStmts) {
        this.startingCondStmts = startingCondStmts;
    }

    Map<Unit, Stack> getUnitsStack() {
        return unitsStack;
    }

    void addUnitStack(Unit newUnit, Stack stack) {
        unitsStack.put(newUnit, stack);
    }

    Stack getUnitStack(Unit unit) {
        return unitsStack.get(unit);
    }

    Unit getLast() {
        return last;
    }

    void setLast(Unit last) {
        this.last = last;
    }

    void createSwitch(SwitchStmt switchStmt) {
        switchList.add(switchStmt);
        List<Unit> uniqueSwitchTargets = getUniqueTargetsWithoutDefault(switchStmt);
        List<SwitchConditionalStmt> conditionalStmts = new ArrayList<>();
        conditionalStmts.add(new SwitchConditionalStmt(new StackStmt((Stmt) switchStmt.getDefaultTarget(), scopeMethod),
                null, switchStmt));
        for (int i=0; i < uniqueSwitchTargets.size(); i++) {
            Unit unitCase = uniqueSwitchTargets.get(i);
            Unit target = i+1 >= uniqueSwitchTargets.size() ?
                    switchStmt.getDefaultTarget() : uniqueSwitchTargets.get(i+1);
            conditionalStmts.add(
                    new SwitchConditionalStmt(new StackStmt((Stmt) unitCase, scopeMethod), target, switchStmt));
        }
        for (SwitchConditionalStmt condStmt : conditionalStmts) {
            condStmt.addAllDependents(conditionalStmts);
        }
        switchMap.put(switchStmt, conditionalStmts);
    }

    private List<Unit> getUniqueTargetsWithoutDefault(SwitchStmt switchStmt) {
        List<Unit> uniqueTargets = new ArrayList<>();
        for (Unit unit : switchStmt.getTargets()) {
            if (!uniqueTargets.contains(unit) && !switchStmt.getDefaultTarget().equals(unit)) {
                uniqueTargets.add(unit);
            }
        }
        return uniqueTargets;
    }

    SwitchConditionalStmt getSwitchCondStmtWithUnit(SwitchStmt switchStmt, Unit unit) {
        List<SwitchConditionalStmt> switchConditionalStmts = switchMap.get(switchStmt);
        if (switchConditionalStmts == null) return null;
        for (SwitchConditionalStmt condStmt : switchConditionalStmts) {
            if (condStmt.getStackStmt().getStmt().equals(unit))
                return condStmt;
        }
        logger.warn("Unable to find SwitchConditionalStmt with the specified unit.");
        return null;
    }

    boolean isSwitchTarget(Unit unit) {
        for(SwitchStmt switchStmt : switchList) {
            if (switchStmt.getTargets().contains(unit) || switchStmt.getDefaultTarget().equals(unit))
                return true;
        }
        return false;
    }

    SwitchStmt getSwitchWithTargetEqualTo(Unit unit) {
        for(SwitchStmt switchStmt : switchList) {
            if (switchStmt.getTargets().contains(unit) || switchStmt.getDefaultTarget().equals(unit))
                return switchStmt;
        }
        return null;
    }

    Unit getNextCaseUnitFromSwitch(SwitchStmt switchStmt, Unit currentCase) {
        if (currentCase.equals(switchStmt.getDefaultTarget()))
            return null;
        int currentIndex = switchStmt.getTargets().indexOf(currentCase);
        if (currentIndex + 1 >= switchStmt.getTargets().size())
            return null;
        return switchStmt.getTarget(currentIndex + 1);
    }
}
