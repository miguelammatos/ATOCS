package atocs.plugins.hbasecommon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import atocs.core.*;
import atocs.core.datastructures.InvokeExprState;
import atocs.core.datastructures.StringValueState;
import atocs.core.datastructures.ValueState;
import atocs.plugins.DatabasePlugin;
import atocs.plugins.hbasecommon.datastructures.ColumnFamilyAndQualifier;

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
     * Analyses a put operation and determines the database field requirements.
     *
     * @param tableNames the possible table names.
     */
    protected void handlePutOperation(List<StringValueState> tableNames) {
        requirementGenerator.generatePutRequirement(CodeAnalyser.getStringsFromStringValueStates(tableNames));
    }

    /**
     * Analyses a get operation and determines the database field requirements.
     *
     * @param tableInvokeExprState application state when the get operation was found.
     * @param tableNames the possible table names.
     */
    protected void handleGetOperation(InvokeExprState tableInvokeExprState, List<StringValueState> tableNames) {
        if (tableInvokeExprState.getArgCount() == 1) {
            requirementGenerator.generateGetRequirement(CodeAnalyser.getStringsFromStringValueStates(tableNames));
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
            requirementGenerator.generateScanRequirement(CodeAnalyser.getStringsFromStringValueStates(tableNames));
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
        requirementGenerator.generateDeleteRequirement(CodeAnalyser.getStringsFromStringValueStates(tableNames));
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
        if (stringValues.isEmpty()) logger.warn("Unable to obtain String value from toBytes method");
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
