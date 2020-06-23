package pt.ulisboa.tecnico.atocs.plugins.hbasecommon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.atocs.core.*;
import pt.ulisboa.tecnico.atocs.core.datastructures.InvokeExprState;
import pt.ulisboa.tecnico.atocs.core.datastructures.StringValueState;
import pt.ulisboa.tecnico.atocs.core.datastructures.ValueState;
import pt.ulisboa.tecnico.atocs.core.DatabasePlugin;
import pt.ulisboa.tecnico.atocs.plugins.hbasecommon.datastructures.ColumnFamilyAndQualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class HBasePlugin extends DatabasePlugin {
    private static final Logger logger = LoggerFactory.getLogger(HBasePlugin.class);

    protected final RequirementGenerator requirementGenerator;

    public HBasePlugin() {
        requirementGenerator = new RequirementGenerator();
    }

    @Override
    public abstract void analyseDbInteraction(InvokeExprState invokeExprState);

    @Override
    public abstract String getApiFilePath();

    @Override
    public abstract List<String> getLibPaths();

    @Override
    public void removeOverlappingRequirements(Map<String, Map<DbField, List<Requirement>>> requirementsMap) {
        requirementGenerator.removeOverlappingRequirements(requirementsMap);
    }

    @Override
    public void removeOverlappingObtainedFields(Map<String, Set<DbField>> obtainedFields) {
        requirementGenerator.removeOverlappingObtainedFields(obtainedFields);
    }

    protected abstract FilterHandler getFilterHandler();

    /**
     * Analyses any table operation and determines the database field requirements.
     *
     * @param tableNames the possible table names.
     */
    protected void handleOperation(InvokeExprState tableInvokeExprState, List<StringValueState> tableNames) {
        String methodOperation = api.getMethodOperation(tableInvokeExprState);
        switch (methodOperation) {
            case "PUT":
                handlePutOperation(tableNames);
                break;
            case "GET":
                handleGetOperation(tableInvokeExprState, tableNames);
                break;
            case "SCAN":
                handleScanOperation(tableInvokeExprState, tableNames);
                break;
            case "DELETE":
                handleDeleteOperation(tableNames);
                break;
            case "INCREMENT":
                handleIncrementOperation(tableInvokeExprState, tableNames);
                break;
            case "APPEND":
                handleAppendOperation(tableInvokeExprState, tableNames);
                break;
            case "MUTATE":
                handleMutateRowOperation(tableNames);
                break;
            case "CHECKMUTATE":
                handleCheckAndMutateOperation(tableInvokeExprState, tableNames);
                break;
            default:
                logger.error("Unknown HBase operation");
                break;
        }
    }


    /**
     * Analyses a put operation and determines the database field requirements.
     *
     * @param tableNames the possible table names.
     */
    protected void handlePutOperation(List<StringValueState> tableNames) {
        requirementGenerator.generateKeyEqualityRequirement("PUT",
                CodeAnalyser.getStringsFromStringValueStates(tableNames));
    }

    /**
     * Analyses a get operation and determines the database field requirements.
     *
     * @param tableInvokeExprState application state when the get operation was found.
     * @param tableNames the possible table names.
     */
    protected void handleGetOperation(InvokeExprState tableInvokeExprState, List<StringValueState> tableNames) {
        if (tableInvokeExprState.getArgCount() == 1) {
            requirementGenerator.generateKeyEqualityRequirement("GET",
                    CodeAnalyser.getStringsFromStringValueStates(tableNames));
            ValueState tableGetArg = tableInvokeExprState.getArg(0);
            List<ValueState> getObjRefs = new ArrayList<>();
            if (CodeAnalyser.isOfType(tableGetArg, HBaseInfo.GET_CLASS))
                getObjRefs.add(tableGetArg);
            else if (CodeAnalyser.isOfType(tableGetArg, Constants.JAVA_LIST))
                getObjRefs.addAll(CodeAnalyser.getObjsAddedToList(tableGetArg));
            else {
                logger.error("Unknown Table get operation argument.");
                return;
            }
            handleOperationArguments(getObjRefs, tableNames);
        } else {
            logger.error("Unknown Table get operation");
        }
    }

    /**
     * Analyses a scan operation and determines the database field requirements.
     *
     * @param tableInvokeExprState application state when the scan operation was found.
     * @param tableNames the possible table names.
     */
    protected void handleScanOperation(InvokeExprState tableInvokeExprState, List<StringValueState> tableNames) {
        if (tableInvokeExprState.getArgCount() == 1) {
            requirementGenerator.generateKeyOrderRequirement("SCAN",
                    CodeAnalyser.getStringsFromStringValueStates(tableNames));
            ValueState tableScanArg = tableInvokeExprState.getArg(0);
            if (CodeAnalyser.isOfType(tableScanArg, HBaseInfo.SCAN_CLASS)) {
                List<ValueState> scanObjRefs = new ArrayList<>();
                scanObjRefs.add(tableScanArg);
                handleOperationArguments(scanObjRefs, tableNames);
                return;
            }
        }
        logger.error("Unknown Table scan operation argument.");
    }

    /**
     * Analyses a delete operation and determines the database field requirements.
     *
     * @param tableNames the possible table names.
     */
    protected void handleDeleteOperation(List<StringValueState> tableNames) {
        requirementGenerator.generateKeyEqualityRequirement("DELETE",
                CodeAnalyser.getStringsFromStringValueStates(tableNames));
    }

    /**
     * Analyses an increment operation and determines the database field requirements.
     *
     * @param tableInvokeExprState application state when the scan operation was found.
     * @param tableNames the possible table names.
     */
    protected void handleIncrementOperation(InvokeExprState tableInvokeExprState, List<StringValueState> tableNames) {
        List<ColumnFamilyAndQualifier> columnFamilyAndQualifiers = new ArrayList<>();
        if (tableInvokeExprState.getArgCount() == 1) {
            List<ValueState> incObjRefs = new ArrayList<>();
            incObjRefs.add(tableInvokeExprState.getArg(0));
            columnFamilyAndQualifiers.addAll(handleIncrementAndAppendObject(incObjRefs));
        } else if (tableInvokeExprState.getArgCount() == 4 || tableInvokeExprState.getArgCount() == 5) {
            columnFamilyAndQualifiers.add(new ColumnFamilyAndQualifier(
                    getStringFromToBytesMethod(tableInvokeExprState.getArg(1)),
                    getStringFromToBytesMethod(tableInvokeExprState.getArg(2))));
        } else {
            logger.error("Unknown Table increment operation argument.");
            return;
        }

        intersectValues(tableNames, columnFamilyAndQualifiers);
    }

    /**
     * Analyses an append operation and determines the database field requirements.
     *
     * @param tableNames the possible table names.
     */
    protected void handleAppendOperation(InvokeExprState tableInvokeExprState, List<StringValueState> tableNames) {
        if (tableInvokeExprState.getArgCount() == 1) {
            List<ValueState> objRefs = new ArrayList<>();
            objRefs.add(tableInvokeExprState.getArg(0));
            List<ColumnFamilyAndQualifier> columnFamilyAndQualifiers = handleIncrementAndAppendObject(objRefs);
            intersectValues(tableNames, columnFamilyAndQualifiers);
        } else {
            logger.error("Unknown Table append operation argument.");
        }
    }

    /**
     * Analyses a mutate row operation and determines the database field requirements. Mutate row operation receives a
     * RowMutations object, which can only receive either Put or Delete objects.
     *
     * @param tableNames the possible table names.
     */
    protected void handleMutateRowOperation(List<StringValueState> tableNames) {
        requirementGenerator.generateKeyEqualityRequirement("MUTATE",
                CodeAnalyser.getStringsFromStringValueStates(tableNames));
    }

    /**
     * Analyses a checkAndMutate operation and determines the database field requirements.
     *
     * @param tableInvokeExprState application state when the scan operation was found.
     * @param tableNames the possible table names.
     */
    protected void handleCheckAndMutateOperation(InvokeExprState tableInvokeExprState,
                                                 List<StringValueState> tableNames) {
        requirementGenerator.generateKeyEqualityRequirement("CHECKMUTATE",
                CodeAnalyser.getStringsFromStringValueStates(tableNames));
        if (tableInvokeExprState.getArgCount() == 2) {
            if (CodeAnalyser.isOfType(tableInvokeExprState.getArg(1), HBaseInfo.FILTER)) {
                getFilterHandler().handleFilter(CodeAnalyser.getStringsFromStringValueStates(tableNames),
                        tableInvokeExprState.getArg(1), new ArrayList<>());
            }
        } else {
            logger.error("Unknown Table checkAndMutate operation argument.");
        }
    }


    /**
     * Determines the String value of the column family and qualifier from a given Increment or Append object.
     *
     * @param objRefs the Increment or Append objects
     * @return the list of Strings associated with this column family and qualifier.
     */
    protected List<ColumnFamilyAndQualifier> handleIncrementAndAppendObject(List<ValueState> objRefs) {
        List<ColumnFamilyAndQualifier> columnFamilyAndQualifiers = new ArrayList<>();
        for (int i = 0; i < objRefs.size(); i++) {
            ValueState objRef = objRefs.get(i);
            // Determine if the object was created based on another object and add it
            // to the objRefs
            List<InvokeExprState> incInitExprs = CodeAnalyser.findObjConstructorInvocationFromObjRef(objRef);
            for (InvokeExprState incInitExpr : incInitExprs) {
                if (incInitExpr.getArgCount() == 1
                        && (CodeAnalyser.isOfType(incInitExpr.getArg(0), HBaseInfo.INCREMENT_CLASS)
                        || CodeAnalyser.isOfType(incInitExpr.getArg(0), HBaseInfo.APPEND_CLASS))) {
                    objRefs.add(incInitExpr.getArg(0));
                } else if (incInitExpr.getArgCount() == 3
                        && CodeAnalyser.isOfType(incInitExpr.getArg(2), Constants.JAVA_MAP)) {
                    logger.warn("HBase Increment and Append objects with the following constructor are not supported. " +
                            "Analysis may be less precise regarding this operation. {}", incInitExpr);
                }
            }

            List<InvokeExprState> addColumnExpList = CodeAnalyser.findMethodInvocationFromObjectRef(
                    HBaseInfo.ADD_COLUMN_METHOD, objRef);
            for (InvokeExprState addColumnExp : addColumnExpList) {
                ColumnFamilyAndQualifier columnFamilyAndQualifier =
                        new ColumnFamilyAndQualifier(getStringFromToBytesMethod(addColumnExp.getArg(0)),
                                getStringFromToBytesMethod(addColumnExp.getArg(1)));
                columnFamilyAndQualifiers.add(columnFamilyAndQualifier);
            }

        }
        return columnFamilyAndQualifiers;
    }

    protected void intersectValues(List<StringValueState> tableNames,
                                   List<ColumnFamilyAndQualifier> columnFamilyAndQualifiers) {
        for (StringValueState tableName : tableNames) {
            List<String> intersectTables = new ArrayList<>();
            List<String> intersectFamilies = new ArrayList<>();
            List<String> intersectQualifiers = new ArrayList<>();
            for (ColumnFamilyAndQualifier famAndQua : columnFamilyAndQualifiers) {
                for (StringValueState family : famAndQua.getFamilies()) {
                    if (tableName.methodChainIntersectsWith(family))
                        intersectFamilies.add(family.getStringValue());
                }
                for (StringValueState qualifier : famAndQua.getQualifiers()) {
                    if (tableName.methodChainIntersectsWith(qualifier))
                        intersectQualifiers.add(qualifier.getStringValue());
                }
            }
            intersectTables.add(tableName.getStringValue());
            requirementGenerator.addObtainedField(intersectTables, intersectFamilies, intersectQualifiers);
            requirementGenerator.generateIncrementRequirement(intersectTables, intersectFamilies,
                    intersectQualifiers);
        }
    }

    /**
     * Analyses an operation init method, determines any additional operation references and handles their possible
     * filter objects.
     *
     * @param opObjRefs list of operation object references.
     * @param tableNames the possible table names.
     */
    protected void handleOperationArguments(List<ValueState> opObjRefs, List<StringValueState> tableNames) {
        for (int i = 0; i < opObjRefs.size(); i++) {
            ValueState opObjRef = opObjRefs.get(i);
            // Determine if the Op object was created based on another Op (Get or Scan) object and add it
            // to the opObjRefs
            List<InvokeExprState> opObjInitExprs = CodeAnalyser.findObjConstructorInvocationFromObjRef(opObjRef);
            for (InvokeExprState opObjInitExpr : opObjInitExprs) {
                if (opObjInitExpr.getArgCount() == 1 &&
                        (CodeAnalyser.isOfType(opObjInitExpr.getArg(0), HBaseInfo.GET_CLASS) ||
                                CodeAnalyser.isOfType(opObjInitExpr.getArg(0), HBaseInfo.SCAN_CLASS)))
                    opObjRefs.add(opObjInitExpr.getArg(0));
            }

            List<ColumnFamilyAndQualifier> columnFamilyAndQualifiers =
                    getFamilyAndQualifierFromOpObjRef(opObjRef, tableNames);

            // Find setFilter invocations for each Get or Scan object and handle its filters
            List<InvokeExprState> filterInvokeExprStateList = CodeAnalyser.findMethodInvocationFromObjectRef(
                    HBaseInfo.SET_FILTER_METHOD, opObjRef);
            for (InvokeExprState filterInvokeExprState : filterInvokeExprStateList) {
                List<String> intersectTableNames = new ArrayList<>();
                List<ColumnFamilyAndQualifier> intersectFamAndQua = new ArrayList<>();
                for (StringValueState tableName : tableNames) {
                    if (filterInvokeExprState.methodChainIntersectsWith(tableName))
                        intersectTableNames.add(tableName.getStringValue());
                    for (ColumnFamilyAndQualifier famAndQua : columnFamilyAndQualifiers) {
                        List<StringValueState> famValueState = new ArrayList<>();
                        List<StringValueState> quaValueState = new ArrayList<>();
                        for (StringValueState fam : famAndQua.getFamilies()) {
                            if (filterInvokeExprState.methodChainIntersectsWith(fam)
                                    && tableName.methodChainIntersectsWith(fam))
                                famValueState.add(fam);
                        }
                        for (StringValueState qua : famAndQua.getQualifiers()) {
                            if (filterInvokeExprState.methodChainIntersectsWith(qua)
                                    && tableName.methodChainIntersectsWith(qua))
                                quaValueState.add(qua);
                        }
                        intersectFamAndQua.add(new ColumnFamilyAndQualifier(famValueState, quaValueState));
                    }
                }
                getFilterHandler().handleFilter(intersectTableNames, filterInvokeExprState.getArg(0),
                        intersectFamAndQua);
            }
        }
    }

    /**
     * Determines the String value of a Bytes.toBytes method invocation.
     *
     * @param varRefState the reference state of the toBytes method invocation.
     * @return the list of Strings associated with this method invocation.
     */
    public static List<StringValueState> getStringFromToBytesMethod(ValueState varRefState) {
        List<StringValueState> stringValues = new ArrayList<>();
        List<InvokeExprState> hbaseToBytesMethodList = CodeAnalyser.findMethodInvocationAssignedToVariable(
                HBaseInfo.BYTES_CLASS, HBaseInfo.BYTES_TO_BYTES_METHOD, varRefState);
        for (InvokeExprState toBytesExpr : hbaseToBytesMethodList) {
            stringValues.addAll(CodeAnalyser.getStringAssignedToVariable(toBytesExpr.getArg(0)));
        }
        List<InvokeExprState> stringGetBytesMethodList = CodeAnalyser.findMethodInvocationAssignedToVariable(
                Constants.STRING_CLASS, Constants.STRING_GET_BYTES_METHOD, varRefState);
        for (InvokeExprState getBytesExpr : stringGetBytesMethodList) {
            stringValues.addAll(CodeAnalyser.getStringAssignedToVariable(getBytesExpr.getInstance()));
        }
        if (stringValues.isEmpty()) logger.info("Unable to obtain String value from toBytes method");
        return stringValues;
    }

    /**
     * Determines the String value of the column family and qualifier from a given Get or Scan operation.
     *
     * @param opObjRef the Get or Scan operation
     * @return the list of Strings associated with this column family and qualifier.
     */
    protected List<ColumnFamilyAndQualifier> getFamilyAndQualifierFromOpObjRef(ValueState opObjRef,
                                                                               List<StringValueState> tableNames) {
        List<ColumnFamilyAndQualifier> columnFamilyAndQualifierList = new ArrayList<>();
        if (CodeAnalyser.isOfType(opObjRef, HBaseInfo.GET_CLASS) ||
                CodeAnalyser.isOfType(opObjRef, HBaseInfo.SCAN_CLASS)) {
            List<InvokeExprState> opAddColumnExpList = CodeAnalyser.findMethodInvocationFromObjectRef(
                    HBaseInfo.ADD_COLUMN_METHOD, opObjRef);
            for (InvokeExprState opAddColumnExp : opAddColumnExpList) {
                opAddColumnExp.addTag(HBaseInfo.ADD_COLUMN_METHOD);
                ColumnFamilyAndQualifier famAndQua = new ColumnFamilyAndQualifier();
                famAndQua.setFamilies(HBasePlugin.getStringFromToBytesMethod(opAddColumnExp.getArg(0)));
                famAndQua.setQualifiers(HBasePlugin.getStringFromToBytesMethod(opAddColumnExp.getArg(1)));
                columnFamilyAndQualifierList.add(famAndQua);
            }
            List<InvokeExprState> opAddFamilyExpList = CodeAnalyser.findMethodInvocationFromObjectRef(
                    HBaseInfo.ADD_FAMILY_METHOD, opObjRef);
            for (InvokeExprState opAddFamilyExp : opAddFamilyExpList) {
                opAddFamilyExp.addTag(HBaseInfo.ADD_FAMILY_METHOD);
                ColumnFamilyAndQualifier famAndQua = new ColumnFamilyAndQualifier();
                famAndQua.setFamilies(HBasePlugin.getStringFromToBytesMethod(opAddFamilyExp.getArg(0)));
                columnFamilyAndQualifierList.add(famAndQua);
            }

            List<InvokeExprState> allMethodsToSearch = new ArrayList<>(opAddColumnExpList);
            allMethodsToSearch.addAll(opAddFamilyExpList);
            List<ValueState> obtainedValueStates = ConditionalAnalysis.performAnalysis(opObjRef, allMethodsToSearch);
            determineObtainedFieldsFromColumnMethods(obtainedValueStates, tableNames);
        } else {
            logger.error("Unknown operation to extract column family and qualifier");
        }
        return columnFamilyAndQualifierList;
    }

    protected void determineObtainedFieldsFromColumnMethods(List<ValueState> obtainedValueStates,
                                                            List<StringValueState> tableNames) {
        if (obtainedValueStates.isEmpty()) {
            for (StringValueState tableName : tableNames) {
                requirementGenerator.addObtainedField(tableName.getStringValue());
            }
        } else {
            for (ValueState obtainedValueState : obtainedValueStates) {
                if (obtainedValueState.hasTag(HBaseInfo.ADD_COLUMN_METHOD)
                        && obtainedValueState instanceof InvokeExprState) {
                    InvokeExprState opAddColumn = (InvokeExprState) obtainedValueState;
                    List<String> families = CodeAnalyser.getStringsFromStringValueStates(
                            HBasePlugin.getStringFromToBytesMethod(opAddColumn.getArg(0)));
                    List<String> qualifiers = CodeAnalyser.getStringsFromStringValueStates(
                            HBasePlugin.getStringFromToBytesMethod(opAddColumn.getArg(1)));
                    for (StringValueState tableName : tableNames) {
                        if (tableName.methodChainIntersectsWith(obtainedValueState)) {
                            for (String family : families) {
                                for (String qualifier : qualifiers) {
                                    requirementGenerator.addObtainedField(tableName.getStringValue(), family,
                                            qualifier);
                                }
                            }
                        }
                    }
                } else if (obtainedValueState.hasTag(HBaseInfo.ADD_FAMILY_METHOD)
                        && obtainedValueState instanceof InvokeExprState) {
                    InvokeExprState opAddFamily = (InvokeExprState) obtainedValueState;
                    List<String> families = CodeAnalyser.getStringsFromStringValueStates(
                            HBasePlugin.getStringFromToBytesMethod(opAddFamily.getArg(0)));
                    for (StringValueState tableName : tableNames) {
                        if (tableName.methodChainIntersectsWith(obtainedValueState)) {
                            for (String family : families) {
                                requirementGenerator.addObtainedField(tableName.getStringValue(), family);
                            }
                        }
                    }
                } else {
                    logger.warn("Unknown obtained ValueState.");
                }
            }
        }
    }
}
