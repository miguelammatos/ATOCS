package atocs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import atocs.core.api.API;
import atocs.core.datastructures.*;
import atocs.plugins.DatabasePlugin;

import java.util.*;
import java.util.stream.Collectors;

import static atocs.core.Constants.*;

public class CodeAnalyser {
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalyser.class);

    private final DatabasePlugin dbPlugin;

    private static final Map<SootField, List<ValueState>> fieldValues = new HashMap<>();

    CodeAnalyser(DatabasePlugin dbPlugin) {
        this.dbPlugin = dbPlugin;
    }

    /**
     * Analysis of the set of methods with some database interactions.
     *
     * @param methodsToAnalyse set of methods with some database interactions.
     */
    void analyse(Set<SootMethod> methodsToAnalyse) {
        for (SootMethod method : methodsToAnalyse)
            analyse(method);
    }

    /**
     * Analysis of a method with some database interactions. Gathers information about the state of the method as
     * it is being analysed and calls the corresponding database module with this information when a database
     * interaction is found.
     *
     * @param method with some database interactions.
     */
    void analyse(SootMethod method) {
        StackManager stackManager = new StackManager(method);
        UnitGraph graph = new BriefUnitGraph(method.getActiveBody());
        for (Unit unit : graph) { // iterate the method unit graph
            // update the stack for each method unit
            updateStack(stackManager, graph, unit, method);
            if (unit instanceof Stmt && ((Stmt)unit).containsInvokeExpr()) {
                InvokeExpr invokeExpr = ((Stmt)unit).getInvokeExpr();
                if (API.getInstance().isApiMethod(invokeExpr.getMethod())) {
                    List<SootMethod> methodChain = new ArrayList<>();
                    methodChain.add(method);
                    dbPlugin.analyseDbInteraction(new InvokeExprState(invokeExpr, method,
                            stackManager.getUnitStack(unit), null, methodChain,
                            stackManager.getUnitStack(unit).getLastestStmt(), null));
                }
            }
        }
    }

    /**
     * Updates an unit stack. Starts with a merged stack from all the unit predecessors and updates it
     * according to the current unit statement. It also updates the unitsStack map with the current
     * unit stack.
     *
     * @param stackManager all method stack current information.
     * @param graph method unit graph.
     * @param unit current unit.
     * @return updated unit stack.
     */
    static Stack updateStack(StackManager stackManager, UnitGraph graph, Unit unit, SootMethod scopeMethod) {
        Unit last = stackManager.getLast();
        List<Unit> unitPreds = new ArrayList<>(graph.getPredsOf(unit));
        if (last instanceof GotoStmt && !unitPreds.contains(last))
                unitPreds.add(last);
        Stack updatedStack = getPreviousStack(stackManager, unitPreds, scopeMethod);

        // Check whether this new unit leaves a conditional block
        boolean removed = updatedStack.removeCondStmtIfUnitEqTarget(unit);

        // Check if the new unit is the beginning of a switch case block
        SwitchConditionalStmt switchConditionalStmt = null;
        boolean isDefaultCase = false;
        boolean isSwitchBlock = false;
        if (graph.getPredsOf(unit).size() == 1 && graph.getPredsOf(unit).get(0) instanceof SwitchStmt) {
            SwitchStmt switchStmt = (SwitchStmt)unitPreds.get(0);
            switchConditionalStmt = stackManager.getSwitchCondStmtWithUnit(switchStmt, unit);
            if (switchConditionalStmt != null) {
                updatedStack.addConditionalStmt(switchConditionalStmt);
            }
            isDefaultCase = switchStmt.getDefaultTarget().equals(unit);
            isSwitchBlock = true;
        }

        // A GotoStmt in a previous unit might mean the beginning of an else block or a switch case block
        IfConditionalStmt gotoConditionalStmt = null;
        // The second condition is false in case of a for or while loop, since the target of the Goto will be a unit
        // that was already analysed.
        if (last instanceof GotoStmt && stackManager.getUnitStack(((GotoStmt) last).getTarget()) == null) {
            if (isDefaultCase && switchConditionalStmt != null) { // default switch case block
                switchConditionalStmt.setTarget(((GotoStmt)last).getTarget());
            } else if (!isSwitchBlock) { // else block
                ConditionalStmt condStmt = stackManager.getUnitStack(last).getFirstConditionalStmtWithTarget(unit);
                if (condStmt instanceof IfConditionalStmt) {
                    gotoConditionalStmt = (IfConditionalStmt) condStmt;
                    Unit gotoTarget = ((GotoStmt) last).getTarget();
                    IfConditionalStmt newConditionalStmt =
                            new IfConditionalStmt(new StackStmt((Stmt) unit, scopeMethod), gotoTarget);
                    gotoConditionalStmt.addNewDependent(newConditionalStmt);
                    newConditionalStmt.addNewDependent(gotoConditionalStmt);
                    updatedStack.addConditionalStmt(newConditionalStmt);
                }
            }
            if (removed) stackManager.getUnitStack(last).removeCondStmtIfUnitEqTarget(unit);
        }

        if (unit instanceof IfStmt) {
            IfConditionalStmt newConditionalStmt =
                    new IfConditionalStmt(new StackStmt((Stmt) unit, scopeMethod), ((IfStmt) unit).getTarget());
            if (gotoConditionalStmt != null) {
                gotoConditionalStmt.addNewDependent(newConditionalStmt);
                newConditionalStmt.addNewDependent(gotoConditionalStmt);
            }
            updatedStack.addConditionalStmt(newConditionalStmt);
        } else if (unit instanceof SwitchStmt) {
            stackManager.createSwitch((SwitchStmt) unit);
        } else if (unit instanceof AssignStmt) {
            AssignStmt newAssignStmt = (AssignStmt) unit;
            Iterator<StackStmt> stackIt = updatedStack.getStatements().iterator();
            while (stackIt.hasNext()) { // iterate all the statements in the current complete stack
                StackStmt stackStmt = stackIt.next();
                if (stackStmt.getStmt() instanceof AssignStmt) { // only update the assignment statements
                    AssignStmt assignStmt = (AssignStmt) stackStmt.getStmt();
                    // if the newAssignStmt is a new definition of variable X, then all previous definitions of this
                    // variable will be removed from the stack
                    if (assignStmt.getLeftOp().equivTo(newAssignStmt.getLeftOp())) {
                        stackIt.remove();
                    }
                }
            }
            updatedStack.addNew(new StackStmt(newAssignStmt, scopeMethod));
        } else if (unit instanceof IdentityStmt || unit instanceof InvokeStmt){
            updatedStack.addNew(new StackStmt((Stmt) unit, scopeMethod));
        }

        stackManager.addUnitStack(unit, updatedStack);
        stackManager.setLast(unit);
        return updatedStack;
    }

    /**
     * Gets the precedents of an unit graph and mergers all their stacks in order to generate a complete
     * previous stack state.
     *
     * @param stackManager all method stack current information.
     * @param unitPreds list of units that are predecessors of the current unit.
     * @return merged list of previous stack statements.
     */
    static Stack getPreviousStack(StackManager stackManager, List<Unit> unitPreds, SootMethod scopeMethod) {
        Map<Unit, Stack> unitsStack = stackManager.getUnitsStack();
        Stack prevStack = stackManager.buildStack(scopeMethod);
        if (unitPreds.size() == 1) {
            Unit prevUnit = unitPreds.get(0);
            if (unitsStack.containsKey(prevUnit)) {
                prevStack.setConditionalStmts(unitsStack.get(prevUnit).getConditionalStmts());
                prevStack.addAllPrevious(unitsStack.get(prevUnit).getStatements());
                prevStack.setCurrentStmt(unitsStack.get(prevUnit).getCurrentStmt());
            }
        } else {
            for (Unit unit : unitPreds) {
                if (unitsStack.containsKey(unit)){
                    for (StackStmt stmt : unitsStack.get(unit).getStatements()) {
                        if (!prevStack.contains(stmt))
                            prevStack.addPrevious(stmt);
                    }
                    prevStack.mergeConditionalStmts(unitsStack.get(unit).getConditionalStmts());
                    prevStack.setCurrentStmt(unitsStack.get(unit).getCurrentStmt());
                }
            }
        }
        return prevStack;
    }

    /**
     * Determines the object on which the invoke expression is called.
     *
     * @param invokeExpr a method call.
     * @return the object instance of the invoke expression.
     */
    public static ValueState getInvokeExprInstance(InvokeExpr invokeExpr, SootMethod scopeMethod, Stack stack,
                                                   List<ValueState> scopeMethodParamValues,
                                                   List<SootMethod> methodChain, StackStmt currentStmt,
                                                   List<InvokeExprState> paramMethodInvocations) {
        ValueState caller = null;
        for (ValueBox v : invokeExpr.getUseBoxes()) {
            if (v instanceof JimpleLocalBox) {
                // The value is the object on which this invokeExpr is called
                caller = createValueState(v.getValue(), scopeMethod, stack, scopeMethodParamValues, methodChain,
                        currentStmt, paramMethodInvocations);
                break;
            }
        }
        return caller;
    }

    /**
     * Obtains the object references added to a List. Supports add, set and addAll methods.
     *
     * @param listRefState list reference state.
     * @return all object references added to a List, if any.
     */
    public static List<ValueState> getObjsAddedToList(ValueState listRefState) {
        return NativeJavaAnalyser.getObjsAddedToList(listRefState);
    }

    /**
     * Obtains the object references added to a Set. Supports Set, HashSet and TreeSet with the following methods: add,
     * set and addAll.
     *
     * @param setRefState set reference state.
     * @return all object references added to a Set, if any.
     */
    public static List<ValueState> getObjsAddedToSet(ValueState setRefState) {
        return NativeJavaAnalyser.getObjsAddedToSet(setRefState);
    }

    /**
     * Obtains the object references added to an array.
     *
     * @param arrayRefState array reference state.
     * @return all object references added to the array provided, if any.
     */
    public static List<ValueState> getObjsAddedToArray(ValueState arrayRefState) {
        List<ValueState> objsAddedToArray = new ArrayList<>();
        // For each array direct reference we can determine the objects added to the array.
        for (ValueState ref : getAllObjRefsFromSingleRef(arrayRefState)) {
            for (StackStmt stmt : ref.getStack().getStatements()) {
                if (stmt.getStmt() instanceof AssignStmt && ((AssignStmt) stmt.getStmt()).getLeftOp() instanceof ArrayRef
                    && ((ArrayRef)((AssignStmt) stmt.getStmt()).getLeftOp()).getBase().equivTo(ref.getValue())) {
                    ValueState obj = createValueState(((AssignStmt) stmt.getStmt()).getRightOp(), ref, stmt);
                    if (obj != null) objsAddedToArray.add(obj);
                }
            }
        }
        return objsAddedToArray;
    }

    /**
     * Obtains the all constructor method invocations of a given object.
     *
     * @param ref object reference.
     * @return all the possible constructor method invocations assigned to the object, if any.
     */
    public static List<InvokeExprState> findObjConstructorInvocationFromObjRef(ValueState ref) {
        return findMethodInvocationFromObjectRef(INIT_METHOD, ref);
    }

    /**
     * First determines all the possible object references from a given object reference. Then obtains
     * all the method invocations with a certain signature for each of the object references.
     *
     * @param methodName the name of the method invocation.
     * @param ref object reference.
     * @return all the possible method invocations with the given signature, if any.
     */
    public static List<InvokeExprState> findMethodInvocationFromObjectRef(String methodName, ValueState ref) {
        List<InvokeExprState> methodInvokeExprs = new ArrayList<>();
        List<ValueState> allObjRefs = getAllObjRefsFromSingleRef(ref);
        // For each direct reference we can then find the invoke expressions.
        for (ValueState objRef : allObjRefs) {
            methodInvokeExprs.addAll(findMethodInvocationFromSingleObjectRef(methodName, objRef, new ArrayList<>()));
        }
        return methodInvokeExprs;
    }

    /**
     * Determines all the object references from a single object reference.
     *
     * @param ref single object reference.
     * @return all the object's references.
     */
    static List<ValueState> getAllObjRefsFromSingleRef(ValueState ref) {
        List<ValueState> allObjRefs = new ArrayList<>();
        List<ValueState> refValues = new ArrayList<>();
        refValues.add(ref);
        // Determine the direct references of the object provided.
        logger.trace("Inside Loop");
        for (int i = 0; i < refValues.size(); i++) {
            ValueState refValue = refValues.get(i);
            if (refValue instanceof LocalState) {
                if (!allObjRefs.contains(refValue)) allObjRefs.add(refValue);
                addAllValuesIfNotContains(refValues, getValueOfVar(refValue));
            } else if (refValue instanceof FieldRefState) {
                addAllValuesIfNotContains(refValues, getValueOfVar(refValue));
            } else if (refValue instanceof InvokeExprState) {
                addAllValuesIfNotContains(refValues, getReturnValueFromInvokeExpr((InvokeExprState) refValue));
            } else if (refValue instanceof ParameterRefState) {
                ValueState paramState = getVarAssignedToParameter(refValue);
                if (!allObjRefs.contains(paramState)) allObjRefs.add(paramState);
                addAllValuesIfNotContains(refValues, getParameterValues((ParameterRefState) refValue));
            }
        }
        logger.trace("Outside Loop");
        return allObjRefs;
    }

    /**
     * Obtains the all the method invocations with a certain signature of a given object reference.
     *
     * @param methodName the name of the method invocation.
     * @param ref object reference.
     * @return all the possible method invocations with the given signature, if any.
     */
    static List<InvokeExprState> findMethodInvocationFromSingleObjectRef(String methodName, ValueState ref,
                                                                         List<SootMethod> prevSearchMethods) {
        List<InvokeExprState> methodInvokeExprs = new ArrayList<>();
        for (StackStmt stackStmt : ref.getStack().getStatements()) {
            if (stackStmt.getStmt().containsInvokeExpr()) {
                boolean analysed = false;
                InvokeExpr invokeExpr = stackStmt.getStmt().getInvokeExpr();
                if (invokeExpr.getMethod().getName().equals(methodName)) {
                    InvokeExprState invokeExprState = new InvokeExprState(invokeExpr, ref, stackStmt);
                    if (invokeExprState.hasInstance() &&
                            invokeExprState.getInstance().getValue().equivTo(ref.getValue())) {
                        methodInvokeExprs.add(invokeExprState);
                        analysed = true;
                    }
                }
                // Analyse the case where the object reference was passed as an argument to another method
                if (!analysed && !invokeExpr.getMethod().equals(ref.getScopeMethod()) &&
                        !prevSearchMethods.contains(invokeExpr.getMethod())) {
                    List<Value> args = invokeExpr.getArgs();
                    for (int i=0; i<args.size(); i++) {
                        if (args.get(i).equivTo(ref.getValue())){
                            List<SootMethod> newPrevSearchMethods = new ArrayList<>(prevSearchMethods);
                            newPrevSearchMethods.add(invokeExpr.getMethod());
                            List<ValueState> paramRefs = getRefsToMethodParam(new InvokeExprState(invokeExpr, ref,
                                    stackStmt), i);
                            for (ValueState paramRef : paramRefs)
                                methodInvokeExprs.addAll(findMethodInvocationFromSingleObjectRef(methodName,
                                        paramRef, newPrevSearchMethods));
                        }
                    }
                }
            }
        }
        if (methodInvokeExprs.isEmpty())
            logger.debug("Unable to find method invocation {} from object reference.", methodName);
        return methodInvokeExprs;
    }

    /**
     * Obtain a reference to a parameter of the provided method invocation inside its body.
     *
     * @param invokeExprState method which will be analysed to obtain a reference to a specific parameter.
     * @param index of the parameter to obtain.
     * @return all the references to the method parameter.
     */
    static List<ValueState> getRefsToMethodParam(InvokeExprState invokeExprState, int index) {
        List<ValueState> paramRefsState = new ArrayList<>();
        if (!assertMethodAnalysis(invokeExprState.getValue().getMethod()))
            return paramRefsState;
        StackManager stackManager = new StackManager(invokeExprState.getValue().getMethod());
        stackManager.setStartingCondStmts(invokeExprState.getStack().getCurrentConditionalStmts());
        UnitGraph graph = new BriefUnitGraph(invokeExprState.getValue().getMethod().getActiveBody());
        Stack completeStack = stackManager.buildStack(invokeExprState.getValue().getMethod());
        for (Unit unit : graph) {
            completeStack = updateStack(stackManager, graph, unit, invokeExprState.getValue().getMethod());
            Value paramRef = null;
            if (unit instanceof IdentityStmt) {
                IdentityStmt stmt = (IdentityStmt) unit;
                if (stmt.getRightOp() instanceof ParameterRef &&
                        ((ParameterRef)stmt.getRightOp()).getIndex() == index) {
                    paramRef = stmt.getLeftOp();
                }
            } else if (unit instanceof AssignStmt && paramRefsState.stream().map(ValueState::getValue)
                    .collect(Collectors.toList()).contains(((AssignStmt) unit).getRightOp())) {
                paramRef = ((AssignStmt) unit).getLeftOp();
            }
            if (paramRef != null) {
                ValueState paramRefState = createValueState(paramRef, invokeExprState.getValue().getMethod(),
                        completeStack, invokeExprState.getArgs(), invokeExprState.getMethodChain(),
                        completeStack.getLastestStmt(), invokeExprState.getParamMethodInvocations());
                if (paramRefState != null) paramRefsState.add(paramRefState);
            }
        }
        for (ValueState paramRefState : paramRefsState) { // Just to have the full method stack in every param ref
            paramRefState.setStack(completeStack);
        }
        return paramRefsState;
    }

    /**
     * Finds all method invocations that match a certain method signature (class name and method name) which are
     * assigned to the given value. This is an iterative process since the variable can be assigned to a method
     * invocation which can also return another method invocation and so forth.
     *
     * @param className the name of the class that declares the method to search.
     * @param methodName the name of the method invocation to search.
     * @param variableRef variable reference.
     * @return all the possible method invocations with the given signature assigned to the variable reference, if any.
     */
    public static List<InvokeExprState> findMethodInvocationAssignedToVariable(String className, String methodName,
                                                                               ValueState variableRef) {
        List<InvokeExprState> invokeExprs = new ArrayList<>();
        List<ValueState> variableValues = CodeAnalyser.getValueOfVar(variableRef);
        for (ValueState varValue : variableValues) {
            if (varValue instanceof InvokeExprState &&
                    isClassEqualToOrChildOf(((InvokeExprState) varValue).getDeclaringClassName(), className) &&
                    ((InvokeExprState) varValue).getMethodName().equals(methodName)) {
                invokeExprs.add((InvokeExprState) varValue);
            } else {
                List<InvokeExprState> exprStates = CodeAnalyser.getNextInvokeExprFromValue(varValue);
                logger.trace("Inside Loop");
                for (int i=0; i < exprStates.size(); i++) {
                    InvokeExprState possibleExprState = exprStates.get(i);
                    if (isClassEqualToOrChildOf(possibleExprState.getDeclaringClassName(), className) &&
                            possibleExprState.getMethodName().equals(methodName))
                        invokeExprs.add(possibleExprState);
                    else {
                        addAllValuesIfNotContains(exprStates, getNextInvokeExprFromValue(possibleExprState));
                    }
                }
                logger.trace("Outside Loop");
            }
        }
        return invokeExprs;
    }

    /**
     * Obtains the next method invoke expressions from a value.
     *
     * @param valueState value state.
     * @return the next invoke expressions, if any.
     */
    static List<InvokeExprState> getNextInvokeExprFromValue(ValueState valueState) {
        List<InvokeExprState> allPossibleInvokeExprs = new ArrayList<>();
        List<ValueState> possibleValueStates = getNextValue(valueState);

        logger.trace("Inside Loop");
        for (int i=0; i < possibleValueStates.size(); i++) {
            ValueState possibleValueState = possibleValueStates.get(i);
            if (possibleValueState instanceof InvokeExprState) {
                allPossibleInvokeExprs.add((InvokeExprState) possibleValueState);
            } else if (!(possibleValueState instanceof ConstantState || possibleValueState instanceof NewExprState)) {
                addAllValuesIfNotContains(possibleValueStates, getNextValue(possibleValueState));
            }
        }
        logger.trace("Outside Loop");

        return allPossibleInvokeExprs;
    }

    /**
     * Obtains the enumerate values assigned to a given variable, as Strings.
     *
     * @param enumType the type or class of the Enumerate.
     * @param variableRef variable reference.
     * @return all the possible enumerate values assigned to a given variable, if any.
     */
    public static List<StringValueState> getEnumValuesAssignedToVariable(String enumType, ValueState variableRef) {
        List<StaticFieldRefState> staticFieldValues = new ArrayList<>();
        List<ValueState> variableValueStates = CodeAnalyser.getValueOfVar(variableRef);
        for (ValueState varValue : variableValueStates) {
            if (varValue instanceof StaticFieldRefState && varValue.getValue().getType().toString().equals(enumType)) {
                staticFieldValues.add((StaticFieldRefState) varValue);
            } else {
                List<ValueState> valueStates = CodeAnalyser.getNextValue(varValue);
                logger.trace("Inside loop");
                for (int i=0; i < valueStates.size(); i++) {
                    ValueState possibleStaticFieldState = valueStates.get(i);
                    if (possibleStaticFieldState instanceof StaticFieldRefState)
                        staticFieldValues.add((StaticFieldRefState) possibleStaticFieldState);
                    else if (!(possibleStaticFieldState instanceof ConstantState ||
                            possibleStaticFieldState instanceof NewExprState))
                        addAllValuesIfNotContains(valueStates, getNextValue(possibleStaticFieldState));
                }
                logger.trace("Outside loop");
            }
        }
        List<StringValueState> enumValues = new ArrayList<>();
        for (StaticFieldRefState enumValue : staticFieldValues)
            enumValues.add(new StringValueState(enumValue.getValue().getField().getName(), enumValue));

        if (enumValues.isEmpty())
            logger.info("Unable to find static field reference with type {}", enumType);
        return enumValues;
    }

    /**
     * Obtains a list of String values assigned to the variable provided.
     *
     * @param varRef variable reference.
     * @return list of String values assigned to the provided variable.
     */
    public static List<StringValueState> getStringAssignedToVariable(ValueState varRef) {
        List<StringValueState> stringValues = new ArrayList<>();
        List<ConstantState> constantValues = getConstAssignedToVariable(varRef);
        for (ConstantState constantValue : constantValues) {
            stringValues.add(getStringFromConst(constantValue));
        }
        return stringValues;
    }

    /**
     * Obtains the constant value assigned to a corresponding variable.
     *
     * @param variableRef variable reference.
     * @return all the possible constant values assigned to the variable, if any.
     */
    static List<ConstantState> getConstAssignedToVariable(ValueState variableRef) {
        List<ConstantState> constantValues = new ArrayList<>();
        List<ValueState> variableValueStates = getValueOfVar(variableRef);
        for (ValueState varValue : variableValueStates) {
            List<ValueState> valueStates = getNextValue(varValue);
            logger.trace("Inside Loop");
            for (int i=0; i < valueStates.size(); i++) {
                ValueState valueState = valueStates.get(i);
                if (valueState instanceof ConstantState)
                    constantValues.add((ConstantState) valueState);
                else if (!(valueState instanceof NewExprState))
                    addAllValuesIfNotContains(valueStates, getNextValue(valueState));
            }
            logger.trace("Outside Loop");
        }
        if (constantValues.isEmpty()) logger.debug("Unable to get constant from variable {}", variableRef.getValue());
        return constantValues;
    }

    /**
     * Obtains a String value that corresponds to the constant provided.
     *
     * @param constantState constant ValueState.
     * @return String corresponding to the constant value.
     */
    static StringValueState getStringFromConst(ConstantState constantState) { //TODO check other types
        Constant constant = constantState.getValue();
        String value = "?";
        if (constant instanceof IntConstant)
            value = Integer.toString(((IntConstant) constant).value);
        else if (constant instanceof LongConstant)
            value = Long.toString(((LongConstant) constant).value);
        else if (constant instanceof FloatConstant)
            value = Float.toString(((FloatConstant) constant).value);
        else if (constant instanceof DoubleConstant)
            value = Double.toString(((DoubleConstant) constant).value);
        else if (constant instanceof StringConstant)
            value = ((StringConstant) constant).value;
        if (constant instanceof NullConstant)
            logger.debug("Obtained a null constant.");
        else if (value.equals("?"))
            logger.warn("Unable to determine String value of Constant {}", constant.getClass());
        return new StringValueState(value, constantState);
    }

    /**
     * Obtains the next values from a given value state. Can either be a constant, new invoke expression,
     * invoke expression, method parameter, instance field, static field or a local.
     *
     * @param valueState value state.
     * @return the next value and state from the provided value, if any.
     */
    public static List<ValueState> getNextValue(ValueState valueState) {
        List<ValueState> nextValueStates = new ArrayList<>();
        if (valueState instanceof ConstantState) {
            nextValueStates.add(valueState);
        } else if (valueState instanceof NewExprState) {
            nextValueStates.add(valueState);
        } else if (valueState instanceof InvokeExprState) {
            InvokeExprState invokeExprState = (InvokeExprState) valueState;
            String collection = NativeJavaAnalyser.isCollectionMethod(invokeExprState);
            if (collection != null && invokeExprState.hasInstance())
                return NativeJavaAnalyser.getObjsAddedToCollection(collection, invokeExprState.getInstance());
            else
                return getReturnValueFromInvokeExpr(invokeExprState);
        } else if (valueState instanceof ParameterRefState) {
            return getParameterValues((ParameterRefState) valueState);
        } else if (valueState instanceof FieldRefState || valueState instanceof LocalState) {
            return getValueOfVar(valueState);
        }
        return nextValueStates;
    }

    /**
     * Obtains the possible values of one of the parameters of a given method.
     *
     * @param parameter parameter reference that we want to obtain.
     * @return list of all the possible values for the specified method parameter.
     */
    static List<ValueState> getParameterValues(ParameterRefState parameter) {
        List<ValueState> thisParameterValueStates = new ArrayList<>();
        if (parameter.hasScopeMethodParamValues()) {
            thisParameterValueStates.add(parameter.getScopeMethodParamValue(parameter.getIndex()));
            return thisParameterValueStates;
        }
        // Find every invocation of the parameter's method and determine all the possible argument values
        List<List<ValueState>> allParameterValues = new ArrayList<>();
        for (int i=0; i<parameter.getScopeMethod().getParameterCount(); i++) {
            allParameterValues.add(new ArrayList<>());
        }
        CallGraph cg = Scene.v().getCallGraph();
        Iterator<MethodOrMethodContext> sources = new Sources(cg.edgesInto(parameter.getScopeMethod()));
        List<SootMethod> alreadyAnalysed = new ArrayList<>();
        while (sources.hasNext()) {
            SootMethod src = (SootMethod) sources.next();
            // ensure that we dont analyse the same method multiple time. CallGraph.edgesInto may return
            // the same method multiple times.
            if (alreadyAnalysed.contains(src))
                continue;
            alreadyAnalysed.add(src);
            List<List<ValueState>> newParameterValues = analyseParameterValues(src, parameter.getScopeMethod(),
                    parameter.getMethodChain());
            if (newParameterValues.size() == allParameterValues.size()) {
                for (int i=0; i<allParameterValues.size(); i++) {
                    for (ValueState newParameterValue : newParameterValues.get(i)) {
                        // only add if the value is not a parameter from a recursive method && only add if
                        // the new parameter value is not already in the list
                        if (!(src.equals(parameter.getScopeMethod()) && isMethodParameterRef(newParameterValue)) &&
                                !allParameterValues.get(i).contains(newParameterValue))
                            allParameterValues.get(i).addAll(newParameterValues.get(i));
                    }
                }
            }
        }

        // From all the possible values obtain the ones for the parameter we desire.
        if (allParameterValues.size() > parameter.getIndex())
            thisParameterValueStates = allParameterValues.get(parameter.getIndex());

        if (thisParameterValueStates.isEmpty())
            logger.debug("Unable to determine parameter value with index {} for method {}", parameter.getIndex(),
                    parameter.getScopeMethod().getSignature());
        return thisParameterValueStates;
    }

    /**
     * Finds the parameter values of a given method invocation inside another method (scope method).
     *
     * @param scopeMethod method that holds the invokeMethod invocation.
     * @param invokeMethod method which parameters we want to find.
     * @return list of values for each method parameter, in the correct order.
     */
    static List<List<ValueState>> analyseParameterValues(SootMethod scopeMethod, SootMethod invokeMethod,
                                                         List<SootMethod> prevMethodChain) {
        List<List<ValueState>> parametersValueStates = new ArrayList<>();
        for (int i=0; i<invokeMethod.getParameterCount(); i++) {
            parametersValueStates.add(new ArrayList<>());
        }
        if (!assertMethodAnalysis(scopeMethod))
            return parametersValueStates;

        StackManager stackManager = new StackManager(scopeMethod);
        UnitGraph graph = new BriefUnitGraph(scopeMethod.getActiveBody());
        for (Unit unit : graph) {
            updateStack(stackManager, graph, unit, scopeMethod);
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    SootMethod sootMethod = invokeExpr.getMethod();
                    if (isMethodEqualToOrOverrides(sootMethod, invokeMethod) &&
                            invokeExpr.getArgCount() == parametersValueStates.size()) {
                        InvokeExprState methodInvocation =
                                new InvokeExprState(invokeExpr, scopeMethod, stackManager.getUnitStack(unit), null,
                                        prevMethodChain, stackManager.getUnitStack(unit).getCurrentStmt(), null);
                        methodInvocation.addParamMethodInvocation(methodInvocation);
                        for (int i=0; i<methodInvocation.getArgCount(); i++) {
                            ValueState argValue = methodInvocation.getArg(i);
                            if (argValue.getMethodChain().contains(invokeMethod))
                                argValue.addMethodToChain(scopeMethod);
                            parametersValueStates.get(i).add(argValue);
                        }
                    }
                }
            }
        }
        if (parametersValueStates.isEmpty())
            logger.debug("Unable to determine parameter values from method invocation {} in method {}",
                    invokeMethod.getSignature(), scopeMethod.getSignature());
        return parametersValueStates;
    }

    /**
     * Obtains the return values of a given method invocation.
     *
     * @param invokeExprState method invocation to be analysed.
     * @return return values of the method invocation.
     */
    static List<ValueState> getReturnValueFromInvokeExpr(InvokeExprState invokeExprState) {
        List<ValueState> returnValueStates = new ArrayList<>();
        if (!invokeExprState.getValue().getMethod().equals(invokeExprState.getScopeMethod()))
            returnValueStates = getReturnValueFromSootMethod(invokeExprState.getValue().getMethod(),
                    invokeExprState.getScopeMethod(), invokeExprState);
        else
            logger.debug("Not reanalysing recursive method {}", invokeExprState.getValue().getMethod().getSignature());
        return returnValueStates;
    }

    /**
     * Obtains the return values of a given method invocation. Determines if a method is overridden by any of
     * the subclasses and also obtain the return values from those.
     *
     * @param method soot method to be analysed.
     * @param scopeMethod method where the invocation to be analysed is inserted in.
     * @param invokeExprState method invocation state.
     * @return return values of the method invocation.
     */
    static List<ValueState> getReturnValueFromSootMethod(SootMethod method, SootMethod scopeMethod,
                                                         InvokeExprState invokeExprState) {
        List<ValueState> returnValueStates = new ArrayList<>();
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();
        if (method.isAbstract()) {
            List<SootMethod> concreteMethodList = hierarchy.resolveAbstractDispatch(method.getDeclaringClass(), method);
            for (SootMethod concreteMethod : concreteMethodList) {
                if (!concreteMethod.equals(scopeMethod))
                    returnValueStates.addAll(getReturnValueFromSootMethod(concreteMethod, scopeMethod,
                            invokeExprState));
            }
        } else if (assertMethodAnalysis(method)) {
            List<SootClass> subClasses = hierarchy.getSubclassesOf(method.getDeclaringClass());
            List<SootMethod> allMethods = new ArrayList<>();
            allMethods.add(method);
            for (SootClass sootClass : subClasses) {
                if (sootClass.declaresMethod(method.getSubSignature()))
                    allMethods.add(sootClass.getMethod(method.getSubSignature()));
            }
            for (SootMethod m : allMethods) {
                StackManager stackManager = new StackManager(m);
                stackManager.setStartingCondStmts(invokeExprState.getStack().getCurrentConditionalStmts());
                UnitGraph graph = new BriefUnitGraph(m.getActiveBody());
                for (Unit unit : graph) {
                    updateStack(stackManager, graph, unit, m);
                    if (unit instanceof ReturnStmt){
                        ValueState returnValue = createValueState(((ReturnStmt) unit).getOp(), m,
                                stackManager.getUnitStack(unit), invokeExprState.getArgs(),
                                invokeExprState.getMethodChain(), stackManager.getUnitStack(unit).getLastestStmt(),
                                invokeExprState.getParamMethodInvocations());
                        if (returnValue != null) returnValueStates.add(returnValue);
                    }
                }
                if (returnValueStates.isEmpty())
                    logger.info("Unable to determine method return value of {}", m.getSignature());
            }
        }
        return returnValueStates;
    }

    /**
     * Obtains the values assigned to a corresponding variable.
     *
     * @param varState variable.
     * @return the values assigned to the variable.
     */
    static List<ValueState> getValueOfVar(ValueState varState) {
        List<ValueState> values = new ArrayList<>();

        if (varState instanceof ConstantState) {
            values.add(varState);
            return values;
        }
        Value value;
        for (StackStmt stackStmt : varState.getStack().getStatements()) {
            Stmt stmt = stackStmt.getStmt();
            value = null;
            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp().equivTo(varState.getValue())) {
                if (((AssignStmt) stmt).getRightOp() instanceof CastExpr) {
                    value = ((CastExpr) ((AssignStmt) stmt).getRightOp()).getOp();
                }
                else if (((AssignStmt) stmt).getRightOp() instanceof ArrayRef) {
                    ArrayRef arrayRef = (ArrayRef) ((AssignStmt) stmt).getRightOp();
                    return getObjsAddedToArray(createValueState(arrayRef.getBase(), varState, stackStmt));
                } else {
                    value = ((AssignStmt) stmt).getRightOp();
                }
            } else if (stmt instanceof IdentityStmt && ((IdentityStmt) stmt).getLeftOp().equivTo(varState.getValue())) {
                value = ((IdentityStmt) stmt).getRightOp();
            }
            if (value != null) {
                ValueState valueState = createValueState(value, varState, stackStmt);
                if (valueState != null) values.add(valueState);
            }
        }
        if (varState instanceof FieldRefState && values.isEmpty()) {
            return getValueOfFieldRef((FieldRefState) varState);
        }
        if (values.isEmpty())
            logger.debug("Unable to find value of variable {} in scope method {}", varState.getValue(),
                    varState.getScopeMethod().getSignature());
        return values;
    }

    /**
     * Obtain the values assigned to a class attribute.
     * @param fieldRefState the class attribute reference.
     * @return the values assigned to the fieldRef.
     */
    static List<ValueState> getValueOfFieldRef(FieldRefState fieldRefState) {
        SootField field = fieldRefState.getValue().getField();
        List<ValueState> fieldValues = CodeAnalyser.fieldValues.get(field);
        if (fieldValues != null)
            return fieldValues;
        else
            fieldValues = new ArrayList<>();
        SootClass fieldClass = field.getDeclaringClass();
        if (field.isFinal()) {
            for (SootMethod m : fieldClass.getMethods()) {
                if (m.isEntryMethod() || m.isConstructor())
                    fieldValues.addAll(analyseMethodForFieldRef(m, fieldRefState));
            }
        } else {
            Hierarchy hierarchy = Scene.v().getActiveHierarchy();
            List<SootClass> classesToAnalyse = new ArrayList<>(hierarchy.getSubclassesOf(fieldClass));
            classesToAnalyse.add(fieldClass);
            for (SootClass classToAnalyse : classesToAnalyse) {
                for (SootMethod method : classToAnalyse.getMethods()) {
                    fieldValues.addAll(analyseMethodForFieldRef(method, fieldRefState));
                }
            }
        }
        CodeAnalyser.fieldValues.put(field, fieldValues);
        if (fieldValues.isEmpty())
            logger.info("Unable to determine value of attribute {} from class {}", field.getName(),
                    fieldClass.getName());
        return fieldValues;
    }

    /**
     * Obtain the values assigned to a given field reference inside a certain method body.
     *
     * @param method to analyse.
     * @param fieldRefState to search for.
     * @return the values assigned to the given field.
     */
    static List<ValueState> analyseMethodForFieldRef(SootMethod method, FieldRefState fieldRefState) {
        List<ValueState> fieldValues = new ArrayList<>();
        if (!assertMethodAnalysis(method))
            return fieldValues;
        StackManager stackManager = new StackManager(method);
        UnitGraph graph = new BriefUnitGraph(method.getActiveBody());
        for (Unit unit : graph) {
            updateStack(stackManager, graph, unit, method);
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                if (assignStmt.containsFieldRef() && assignStmt.getLeftOp() instanceof FieldRef
                        && ((FieldRef)assignStmt.getLeftOp()).getField().equals(
                                fieldRefState.getValue().getField())) {
                    ValueState fieldValue = createValueState(assignStmt.getRightOp(), method,
                            stackManager.getUnitStack(unit), null, null,
                            stackManager.getUnitStack(unit).getLastestStmt(), null);
                    if (fieldValue != null) fieldValues.add(fieldValue);
                }
            }
        }
        return  fieldValues;
    }

    /**
     * Obtains the variable assigned to a corresponding method parameter.
     *
     * @param parameter method parameter.
     * @return variable assigned to the given method parameter.
     */
    static ValueState getVarAssignedToParameter(ValueState parameter) {
        for (StackStmt stackStmt : parameter.getStack().getStatements()) {
            Stmt stmt = stackStmt.getStmt();
            Value var = null;
            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp().equivTo(parameter.getValue()))
                var = ((AssignStmt) stmt).getLeftOp();
            else if (stmt instanceof IdentityStmt && ((IdentityStmt) stmt).getRightOp().equivTo(parameter.getValue()))
                var = ((IdentityStmt) stmt).getLeftOp();
            if (var != null) {
                ValueState varState = createValueState(var, parameter, stackStmt);
                if (varState == null)
                    break;
                return varState;
            }
        }
        logger.error("Unable to obtain variable assigned to method parameter.");
        throw new RuntimeException("Unable to obtain variable assigned to method parameter.");
    }

    /**
     * Creates a subclass of ValueState given the new values and using the rest of the attributes from a previous
     * ValueState.
     *
     * @param value value.
     * @param valueState previous ValueState.
     * @return ValueState subclass with the given attributes.
     */
    public static ValueState createValueState(Value value, ValueState valueState, StackStmt currentStmt) {
        return createValueState(value, valueState.getScopeMethod(), valueState.getStack(),
                valueState.getScopeMethodParamValues(), valueState.getMethodChain(), currentStmt,
                valueState.getParamMethodInvocations());
    }

    /**
     * Creates a subclass of ValueState given the required arguments.
     *
     * @param value value.
     * @param scopeMethod method on which the value resides.
     * @param stack the stack state of the scopeMethod when the value was encountered.
     * @return ValueState subclass with the given attributes.
     */
    public static ValueState createValueState(Value value, SootMethod scopeMethod, Stack stack,
                                              List<ValueState> scopeMethodParamValues, List<SootMethod> methodChain,
                                              StackStmt currentStmt, List<InvokeExprState> paramMethodInvocations) {
        if (value instanceof Constant)
            return new ConstantState((Constant) value, scopeMethod, stack, scopeMethodParamValues, methodChain,
                    currentStmt, paramMethodInvocations);
        else if (value instanceof InvokeExpr)
            return new InvokeExprState((InvokeExpr) value, scopeMethod, stack, scopeMethodParamValues, methodChain,
                    currentStmt, paramMethodInvocations);
        else if (value instanceof ParameterRef)
            return new ParameterRefState((ParameterRef) value, scopeMethod, stack, scopeMethodParamValues, methodChain,
                    currentStmt, paramMethodInvocations);
        else if (value instanceof StaticFieldRef)
            return new StaticFieldRefState((StaticFieldRef) value, scopeMethod, stack, scopeMethodParamValues,
                    methodChain, currentStmt, paramMethodInvocations);
        else if (value instanceof FieldRef)
            return new FieldRefState((FieldRef) value, scopeMethod, stack, scopeMethodParamValues,
                    methodChain, currentStmt, paramMethodInvocations);
        else if (value instanceof Local)
            return new LocalState((Local) value, scopeMethod, stack, scopeMethodParamValues, methodChain, currentStmt,
                    paramMethodInvocations);
        else if (value instanceof NewExpr)
            return new NewExprState((NewExpr) value, scopeMethod, stack, scopeMethodParamValues, methodChain,
                    currentStmt, paramMethodInvocations);
        logger.info("Unable to create ValueState object with value class {}", value.getClass());
        return null;
    }

    /**
     * Adds all the values provided to the list if they are not already there.
     *
     * @param list to update.
     * @param values to insert.
     */
    static <T extends ValueState> void addAllValuesIfNotContains(List<T> list, List<T> values) {
        for (T newValue : values) {
            if (!list.contains(newValue))
                list.add(newValue);
        }
    }

    /**
     * Verifies if a given variable type is of the given class/interface or implements/extends that class/interface.
     *
     * @param varRefState the variable reference.
     * @param className the name of the class to verify.
     * @return true if the variable type is of the given class/interface or implements/extends that class/interface,
     * false otherwise.
     */
    public static boolean isOfType(ValueState varRefState, String className) {
        if (varRefState.getValue().getType().equals(RefType.v(className))) return true;
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();
        SootClass varClass = Scene.v().getSootClass(varRefState.getValue().getType().toString());
        SootClass superClass = Scene.v().getSootClass(className);
        List<SootClass> varSuperClassesAndInterfaces = new ArrayList<>();
        if (varClass.isInterface())
            varSuperClassesAndInterfaces.addAll(hierarchy.getSuperinterfacesOf(varClass));
        else {
            varSuperClassesAndInterfaces.addAll(hierarchy.getSuperclassesOf(varClass));
            varSuperClassesAndInterfaces.addAll(varClass.getInterfaces());
        }
        for (SootClass varSuperClass : varSuperClassesAndInterfaces) {
            if (varSuperClass.equals(superClass))
                return true;
        }
        return false;
    }

    /**
     * Verifies if a given variable is an array of the given class.
     *
     * @param arrayRefState the array reference state.
     * @param className the name of the class.
     * @return true if the variable is an array of the given class, false otherwise.
     */
    public static boolean isArrayOf(ValueState arrayRefState, String className) {
        return arrayRefState.getValue().getType().equals(ArrayType.v(RefType.v(className), 1));
    }

    /**
     * Verifies if a given variable is an array of bytes.
     *
     * @param arrayRefState the array reference state.
     * @return true if the variable is a byte array, false otherwise.
     */
    public static boolean isArrayOfBytes(ValueState arrayRefState) {
        return arrayRefState.getValue().getType().equals(ArrayType.v(ByteType.v(), 1));
    }

    /**
     * Verifies if a given class is a superclass or interface of a provided subclass.
     *
     * @param subClass subClass.
     * @param superClass superClass.
     * @return true if it is a superclass or interface of the subclass, false otherwise.
     */
    static boolean isSuperClassOrInterfaceOf(SootClass subClass, SootClass superClass) {
        if (subClass.isInterface() && superClass.isInterface())
            return Scene.v().getActiveHierarchy().isInterfaceSubinterfaceOf(subClass, superClass);
        else if (!subClass.isInterface() && superClass.isInterface())
            return isInterfaceOf(superClass, subClass);
        else if (!subClass.isInterface() && !superClass.isInterface())
            return Scene.v().getActiveHierarchy().isClassSubclassOf(subClass, superClass);
        else {
            logger.debug("Subclass {} cannot be an interface of a Superclass {}", subClass.getName(),
                    superClass.getName());
            return false;
        }
    }

    /**
     * Verifies if a given class implements the provided interface.
     *
     * @param inter the interface.
     * @param sootClass the class.
     * @return true if the class implements the provided interface, false otherwise.
     */
    static boolean isInterfaceOf(SootClass inter, SootClass sootClass) {
        List<SootClass> interfaces = new ArrayList<>(sootClass.getInterfaces());
        for (SootClass sInterface : interfaces) {
            if (sInterface.equals(inter))
                return true;
        }
        return false;
    }

    /**
     * Verifies if a given method is equal to another or if it overrides the other method.
     *
     * @param superMethod method that might be overridden or equal.
     * @param subMethod concrete method.
     * @return true if the methods are equal or if the subMethod overrides the superMethod, false otherwise.
     */
    public static boolean isMethodEqualToOrOverrides(SootMethod superMethod, SootMethod subMethod) {
        //TODO improve this comparison. SubSignature includes: method name, return type and argument types
        if (subMethod.getSubSignature().equals(superMethod.getSubSignature())) {
            if (subMethod.equals(superMethod)) // completely equal
                return true;
            else // overridden method from a subclass
                return isSuperClassOrInterfaceOf(subMethod.getDeclaringClass(), superMethod.getDeclaringClass());
        }
        return false;
    }

    /**
     * Verifies if a class is equal to or the child of another provided class.
     *
     * @param subClass the name of the possible subClass.
     * @param superClass the name of the possible superClass.
     * @return true if the subClass is equal to or child of the superClass, false otherwise.
     */
    static boolean isClassEqualToOrChildOf(String subClass, String superClass) {
        if (subClass.equals(superClass))
            return true;
        else {
            SootClass subSootClass = Scene.v().getSootClass(subClass);
            SootClass superSootClass = Scene.v().getSootClass(superClass);
            return isSuperClassOrInterfaceOf(subSootClass, superSootClass);
        }
    }

    /**
     * Verifies if the given value is a parameter of its scope method.
     *
     * @param valueState to inspect.
     * @return true if the value is a parameter reference of its scope method, false otherwise.
     */
    static boolean isMethodParameterRef(ValueState valueState) {
        if (valueState instanceof ParameterRefState)
            return true;
        if (!(valueState instanceof LocalState))
            return false;
        for (StackStmt stackStmt : valueState.getStack().getStatements()) { // iterate the method unit graph
            if (stackStmt.getStmt() instanceof IdentityStmt
                    && ((IdentityStmt) stackStmt.getStmt()).getLeftOp().equivTo(valueState.getValue()))
                return true;
        }
        return false;
    }

    /**
     * Verify if an analysis can be performed on the provided method.
     * @param methodToAnalyse method on which to perform an analysis.
     * @return true if the analysis can be done, false otherwise.
     */
    static boolean assertMethodAnalysis(SootMethod methodToAnalyse) {
        if (!methodToAnalyse.getDeclaringClass().isApplicationClass()) {
            logger.debug("Not analysing non application or library method {}", methodToAnalyse.getSignature());
            return false;
        }
        if (methodToAnalyse.isPhantom()) {
            logger.debug("Not analysing phantom method {}", methodToAnalyse.getSignature());
            return false;
        }
        if (!methodToAnalyse.hasActiveBody()) {
            logger.debug("Not analysing method without activeBody {}", methodToAnalyse.getSignature());
            return false;
        }
        return true;
    }

    /**
     * Obtains all the String values from a StringValueState list.
     *
     * @param stringValueStates list of StringValueState
     * @return all the String values from the list
     */
    public static List<String> getStringsFromStringValueStates(List<StringValueState> stringValueStates) {
        return stringValueStates.stream().map(StringValueState::getStringValue).collect(Collectors.toList());
    }

    /**
     * Prints the provided unit and corresponding stack to the logger.
     * @param unit to print.
     * @param stack from the unit.
     */
    void printUnit(Unit unit, List<Stmt> stack) {
        logger.debug("UNIT:");
        logger.debug(unit.toString());
        logger.debug("STACK:");
        for (Stmt stmt : stack)
            logger.debug(stmt.toString());
        logger.debug("");
    }
}
