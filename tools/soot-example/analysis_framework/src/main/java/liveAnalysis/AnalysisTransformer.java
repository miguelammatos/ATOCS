package liveAnalysis;

import java.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.util.ArrayNumberer;
import soot.util.Cons;

public class AnalysisTransformer extends SceneTransformer
{
    private Map<String, List<String>> dbMethods = new HashMap<String, List<String>>();
    private final String DB_CLASS_NAME = "testers.DB";

    private Map<String, List<Value>> methodsReturnValues = new HashMap<String, List<Value>>(); //String is the Signature of the SootMethod

    public AnalysisTransformer() {
        super();
        dbMethods.put("put", new ArrayList<String>());
        dbMethods.put("get", new ArrayList<String>());
        dbMethods.put("scan", new ArrayList<String>());
        dbMethods.put("delete", new ArrayList<String>());
        dbMethods.put("test", new ArrayList<String>());
    }
    @Override
    protected void internalTransform(String arg0, Map arg1) {
        Scene.v().loadNecessaryClasses();

        List<SootMethod> dbAPI = new ArrayList<SootMethod>();

        for (String methodName : dbMethods.keySet()) {
            SootMethod method = Scene.v().getSootClass(DB_CLASS_NAME).getMethodByName(methodName);
            dbAPI.add(method);
        }

        List<SootMethod> methodsToAnalyse = getDbInteractions(dbAPI);

        for (SootMethod sMethod : methodsToAnalyse)
            System.out.println("METHOD: " + sMethod.getSignature());

        for (SootMethod sMethod : methodsToAnalyse) {
            if (!methodsReturnValues.containsKey(sMethod.getSignature()))
                analyseMethod(sMethod);
        }
        printAllDBMethodArgs();
    }

    public List<Value> analyseMethod(SootMethod sMethod) {
        // Create graph based on the method
        UnitGraph graph = new BriefUnitGraph(sMethod.getActiveBody());

        List<Value> returnValues = iterateMethodUnits(graph);
        methodsReturnValues.put(sMethod.getSignature(), returnValues);
        return returnValues;
    }

    public List<Value> iterateMethodUnits(UnitGraph graph) {
        HashMap<Unit, Set<AssignStmt>> flowMap = new HashMap<Unit, Set<AssignStmt>>();
        Iterator<Unit> unitIt = graph.iterator();
        while (unitIt.hasNext()) {
            Unit unit = unitIt.next();
            System.out.println(unit);

            Set<AssignStmt> flowAfter = getNextFlow(getFlowBefore(flowMap, graph.getPredsOf(unit)), unit);

            if (unit instanceof ReturnStmt) {
                Value retOp = ((ReturnStmt) unit).getOp();
                List<Value> retValues = new ArrayList<Value>();
                if (retOp instanceof Constant)
                    retValues.add(retOp);
                else
                    retValues.addAll(getValueOfArg(flowAfter, retOp));
                return retValues;
            }
            for (ValueBox def : unit.getUseAndDefBoxes()) {
                if (def.getValue() instanceof InvokeExpr) {
                    InvokeExpr methodCall = (InvokeExpr) def.getValue();

                    String methodName = methodCall.getMethod().getName();
                    if (methodCall.getMethod().getDeclaringClass().getName().equals(DB_CLASS_NAME) && dbMethods.keySet().contains(methodName)) {
                        StringBuilder stringBuilder = new StringBuilder(); //TODO fazer string apenas depois da analise. Aqui adicionar apenas Constant ou null à lista de argumentos do método
                        stringBuilder.append("(");

                        Iterator<Value> itArgs = methodCall.getArgs().iterator();
                        while (itArgs.hasNext()) {
                            Value arg = itArgs.next();
                            List<Value> values = new ArrayList<Value>();
                            System.out.println("VALUE: " + arg.toString() + " CLASS: " + arg.getClass());
                            if (arg instanceof Constant)
                                values.add(arg);
                            else
                                values.addAll(getValueOfArg(flowAfter, arg));

                            if (values.isEmpty())
                                stringBuilder.append("?");
                            else {
                                Iterator<Value> itValue = values.iterator();
                                while (itValue.hasNext()) {
                                    Value value = itValue.next();
                                    if (value instanceof Constant)
                                        appendConstValue(stringBuilder, (Constant) value);
                                    else if (value instanceof InvokeExpr) {
                                        InvokeExpr method = (InvokeExpr) value;
                                        List<Value> methodRet = new ArrayList<Value>();
                                        SootMethod sootMethod = method.getMethod();
                                        if (sootMethod != null) {
                                            if (methodsReturnValues.containsKey(sootMethod.getSignature())) {
                                                methodRet.addAll(methodsReturnValues.get(sootMethod.getSignature()));
                                            } else {
                                                methodRet.addAll(analyseMethod(sootMethod));
                                            }
                                            if (methodRet.isEmpty())
                                                stringBuilder.append("?");
                                            else {
                                                Iterator<Value> itRetValue = methodRet.iterator();
                                                while (itRetValue.hasNext()) {
                                                    Value v = itRetValue.next();
                                                    if (v instanceof Constant)
                                                        appendConstValue(stringBuilder, (Constant) v);
                                                    else
                                                        stringBuilder.append("?");
                                                    if (itRetValue.hasNext())
                                                        stringBuilder.append("|");
                                                }
                                            }
                                        } else {
                                            stringBuilder.append("?");
                                        }
                                    }
                                    if (itValue.hasNext())
                                        stringBuilder.append("|");
                                }
                            }
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
        return new ArrayList<Value>();
    }

    private Set<AssignStmt> getFlowBefore(Map<Unit, Set<AssignStmt>> flowMap, List<Unit> preds) {
        HashSet<AssignStmt> flowBefore = new HashSet<AssignStmt>();
        for (Unit u : preds) {
            if (flowMap.containsKey(u))
                flowBefore.addAll(flowMap.get(u));
        }
        return flowBefore;
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
                    if (nodeStmt.getRightOp() instanceof StaticInvokeExpr && ((StaticInvokeExpr) nodeStmt.getRightOp()).getMethod().getDeclaringClass().getName().toLowerCase().startsWith("java.") && ((StaticInvokeExpr) nodeStmt.getRightOp()).getArgs().size() > 0) {
                        if (((StaticInvokeExpr) nodeStmt.getRightOp()).getArg(0).equals(astmt.getLeftOp())) { //when static invoke on a variable like "staticinvoke <java.lang.Integer: java.lang.Integer valueOf(int)>(variable)"
                            clone = (AssignStmt) nodeStmt.clone();
                            clone.setRightOp(astmt.getRightOp());
                            nextFlow.add(clone);
                            added = true;
                        } else if (!added && ((StaticInvokeExpr) nodeStmt.getRightOp()).getArg(0) instanceof Constant) { //when static invoke on a constant like "staticinvoke <java.lang.Integer: java.lang.Integer valueOf(int)>(123)"
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
                nextFlow.add((AssignStmt) nodeStmt.clone());
        } else {
            nextFlow.addAll(prevFlow);
        }

        return nextFlow;
    }

    private List<Value> getValueOfArg(Set<AssignStmt> flowSet, Value value) {
        List<Value> values = new ArrayList<Value>();
        for (AssignStmt stmt : flowSet) {
            if (stmt.getLeftOp().equals(value))
                values.add(stmt.getRightOp());
        }
        return values;
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

    public void printPossibleCallers(SootMethod target) {
        CallGraph cg = Scene.v().getCallGraph();
        Iterator sources = new Sources(cg.edgesInto(target));
        while (sources.hasNext()) {
            SootMethod src = (SootMethod)sources.next();
            System.out.println(target.getSubSignature() + " might be called by " + src.getSubSignature());
        }
    }

    public List<SootMethod> getDbInteractions(List<SootMethod> dbAPI) {
        List<SootMethod> methodsToAnalyse = new ArrayList<SootMethod>();
        CallGraph cg = Scene.v().getCallGraph();
        for (SootMethod target : dbAPI) {
            Iterator sources = new Sources(cg.edgesInto(target));
            while (sources.hasNext()) {
                SootMethod src = (SootMethod) sources.next();
                if (!listHasSootMethod(methodsToAnalyse, src))
                    methodsToAnalyse.add(src);
//                System.out.println(target.getSubSignature() + " might be called by " + src.getSubSignature());
            }
        }
        return methodsToAnalyse;
    }

    public boolean listHasSootMethod(List<SootMethod> list, SootMethod method) {
        for (SootMethod m : list) {
            if (equalSootMethods(m, method))
                return true;
        }
        return false;
    }

    public boolean equalSootMethods(SootMethod m1, SootMethod m2) {
        return m1.getSignature().equals(m2.getSignature());
    }
}
