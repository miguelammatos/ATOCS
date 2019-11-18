package liveAnalysis;

import java.util.*;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.util.ArrayNumberer;
import soot.util.Cons;

public class AnalysisTransformer extends SceneTransformer
{
    private Map<String, List<String>> dbMethods = new HashMap<String, List<String>>();
    private final String DB_CLASS_NAME = "testers.DB";

    public AnalysisTransformer() {
        super();
        dbMethods.put("put", new ArrayList<String>());
        dbMethods.put("get", new ArrayList<String>());
        dbMethods.put("scan", new ArrayList<String>());
        dbMethods.put("delete", new ArrayList<String>());
    }
    @Override
    protected void internalTransform(String arg0, Map arg1) {
        Scene.v().loadNecessaryClasses();

        // Get Main Method
        SootMethod sMethod = Scene.v().getMainMethod();

        analyseMethod(sMethod);

        printAllDBMethodArgs();
    }

    public Value analyseMethod(SootMethod sMethod) {
        // Create graph based on the method
        UnitGraph graph = new BriefUnitGraph(sMethod.getActiveBody());

        // Perform LV Analysis on the Graph
//        LiveMethodAnalysis analysis = new LiveMethodAnalysis(graph);

        return iterateUnits(graph);

    }

    public Value iterateUnits(UnitGraph graph) {
        HashMap<Unit, Set<AssignStmt>> flowMap = new HashMap<Unit, Set<AssignStmt>>();
        Iterator<Unit> unitIt = graph.iterator();
        Unit prevUnit = null;
        while (unitIt.hasNext()) {
            Unit unit = unitIt.next();
//            System.out.println(unit);
//            Set<AssignStmt> flowAfter = analysis.getFlowAfter(unit);

            Set<AssignStmt> flowAfter = getNextFlow((prevUnit != null ? flowMap.get(prevUnit) : new HashSet<AssignStmt>()), unit);
            prevUnit = unit;

            if (unit instanceof ReturnStmt) {
                Value retOp = ((ReturnStmt) unit).getOp();
                Value retValue = (retOp instanceof Constant ? retOp : getValueOfArg(flowAfter, retOp));
                return retValue;
            }

            for (ValueBox def : unit.getUseAndDefBoxes()) {
                if (def.getValue() instanceof InvokeExpr) {
                    InvokeExpr methodCall = (InvokeExpr) def.getValue();

                    String methodName = methodCall.getMethod().getName();
                    if (methodCall.getMethod().getDeclaringClass().getName().equals(DB_CLASS_NAME) && dbMethods.keySet().contains(methodName)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("(");

                        Iterator<Value> itArgs = methodCall.getArgs().iterator();
                        while (itArgs.hasNext()) {
                            Value arg = itArgs.next();
                            Value value = (arg instanceof Constant ? arg : getValueOfArg(flowAfter, arg));

                            if (value == null)
                                stringBuilder.append("?");
                            else if (value instanceof Constant)
                                appendConstValue(stringBuilder, (Constant) value);
                            else if (value instanceof InvokeExpr) {
                                InvokeExpr method = (InvokeExpr) value;
                                Value methodRet = null;
                                SootMethod sootMethod = method.getMethod();
                                if (sootMethod != null) {
                                    methodRet = analyseMethod(method.getMethod());
                                    if (methodRet == null)
                                        stringBuilder.append("?");
                                    else if (methodRet instanceof Constant)
                                        appendConstValue(stringBuilder, (Constant) methodRet);
                                    else
                                        stringBuilder.append("?");
                                } else {
                                    stringBuilder.append("?");
                                }
                                updateMethodReturnValue(flowAfter, value, methodRet);
                            }
                            else
                                stringBuilder.append("?");
                            if (itArgs.hasNext())
                                stringBuilder.append(", ");
                        }

                        stringBuilder.append(")");
                        dbMethods.get(methodName).add(stringBuilder.toString());
                    }
                }
            }

            flowMap.put(unit, flowAfter);
        }
        return null;
    }

    private Set<AssignStmt> getNextFlow(Set<AssignStmt> prevFlow, Unit unit) {
        HashSet<AssignStmt> nextFlow = new HashSet<AssignStmt>();

        if (unit instanceof AssignStmt) {
            boolean added = false;
            AssignStmt nodeStmt = (AssignStmt) unit;
            AssignStmt clone;
            for (AssignStmt astmt: prevFlow) {
                if (astmt.getLeftOp().equals(nodeStmt.getLeftOp())) {
                    nextFlow.add((AssignStmt)nodeStmt.clone());
                    added = true;
                }
                else {
                    nextFlow.add(astmt);
                    //TODO differentiate normal static invokes from java static invokes
                    if (nodeStmt.getRightOp() instanceof StaticInvokeExpr && ((StaticInvokeExpr) nodeStmt.getRightOp()).getMethod().getDeclaringClass().getName().toLowerCase().startsWith("java.") && ((StaticInvokeExpr) nodeStmt.getRightOp()).getArgs().size() > 0) {
                        if (((StaticInvokeExpr) nodeStmt.getRightOp()).getArg(0).equals(astmt.getLeftOp())) {
                            clone = (AssignStmt) nodeStmt.clone();
                            clone.setRightOp(astmt.getRightOp());
                            nextFlow.add(clone);
                            added = true;
                        } else if (((StaticInvokeExpr) nodeStmt.getRightOp()).getArg(0) instanceof Constant) {
                            clone = (AssignStmt) nodeStmt.clone();
                            clone.setRightOp(((StaticInvokeExpr) nodeStmt.getRightOp()).getArg(0));
                            nextFlow.add(clone);
                            added = true;
                        }
                    }
                    else if (nodeStmt.getRightOp() instanceof Constant && (nodeStmt.getRightOp()).equals(astmt.getLeftOp())) {
                        clone = (AssignStmt) nodeStmt.clone();
                        clone.setRightOp(astmt.getRightOp());
                        nextFlow.add(clone);
                        added = true;
                    }
                    else if (nodeStmt.getRightOp() instanceof Local && (nodeStmt.getRightOp()).equals(astmt.getLeftOp())) {
                        clone = (AssignStmt) nodeStmt.clone();
                        clone.setRightOp(astmt.getRightOp());
                        nextFlow.add(clone);
                        added = true;
                    }
                }
            }
            if (!added)
                nextFlow.add(nodeStmt);
        } else {
            nextFlow.addAll(prevFlow);
        }

        return nextFlow;
    }

    private void updateMethodReturnValue(Set<AssignStmt> flowSet, Value methodCall, Value returnValue) {
        for (AssignStmt astmt : flowSet) {
            if (astmt.getRightOp().equals(methodCall))
                astmt.setRightOp(returnValue);
        }
    }

    private Value getValueOfArg(Set<AssignStmt> flowSet, Value value) {
        for (AssignStmt stmt : flowSet) {
            if (stmt.getLeftOp().equals(value))
                return stmt.getRightOp();
        }
        return null;
    }

    private void appendConstValue(StringBuilder stringBuilder, Constant constant) {
        if (constant == null)
            stringBuilder.append("?");
        else if (constant instanceof IntConstant)
            stringBuilder.append(((IntConstant) constant).value);
        else if (constant instanceof LongConstant)
            stringBuilder.append(((LongConstant) constant).value);
        else if (constant instanceof FloatConstant)
            stringBuilder.append(((FloatConstant) constant).value);
        else if (constant instanceof DoubleConstant)
            stringBuilder.append(((DoubleConstant) constant).value);
        else if (constant instanceof StringConstant)
            stringBuilder.append("\"" + ((StringConstant) constant).value + "\"");
        else
            stringBuilder.append("?");
    }

    private void printAllDBMethodArgs() {
        System.out.println("\nDB method calls:\n");
        for (String methodName : dbMethods.keySet()) {
            System.out.println(methodName + " arguments:");
            for (String str : dbMethods.get(methodName))
                System.out.println(str);
            System.out.println();
        }
    }

}
