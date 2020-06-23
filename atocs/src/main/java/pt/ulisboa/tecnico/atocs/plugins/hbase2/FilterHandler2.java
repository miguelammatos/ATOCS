package pt.ulisboa.tecnico.atocs.plugins.hbase2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.atocs.core.CodeAnalyser;
import pt.ulisboa.tecnico.atocs.core.Constants;
import pt.ulisboa.tecnico.atocs.core.datastructures.InvokeExprState;
import pt.ulisboa.tecnico.atocs.core.datastructures.StringValueState;
import pt.ulisboa.tecnico.atocs.core.datastructures.ValueState;
import pt.ulisboa.tecnico.atocs.plugins.hbasecommon.FilterHandler;
import pt.ulisboa.tecnico.atocs.plugins.hbasecommon.HBaseInfo;
import pt.ulisboa.tecnico.atocs.plugins.hbasecommon.datastructures.ColumnFamilyAndQualifier;
import pt.ulisboa.tecnico.atocs.plugins.hbasecommon.RequirementGenerator;

import java.util.ArrayList;
import java.util.List;

public class FilterHandler2 extends FilterHandler {

    private final Logger logger = LoggerFactory.getLogger(FilterHandler2.class);


    FilterHandler2(RequirementGenerator requirementGenerator) {
        super(requirementGenerator);
    }

    /**
     * Given a filter object reference, it analyses the filter in use and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     * @param filterRef the filter object reference.
     * @param familiesAndQualifiers the operation object column families and qualifiers.
     */
    @Override
    protected void handleFilter(List<String> tableNames, ValueState filterRef,
                      List<ColumnFamilyAndQualifier> familiesAndQualifiers) {
        String filterClassName = filterRef.getValueClassName();
        switch (filterClassName) {
            case HBaseInfo.COLUMN_VALUE_FILTER:
            case HBaseInfo.SINGLE_COLUMN_VALUE_FILTER:
            case HBaseInfo.SINGLE_COLUMN_VALUE_EXCLUDE_FILTER:
                handleColumnValueFilter(filterClassName, tableNames, filterRef);
                break;
            case HBaseInfo.VALUE_FILTER:
                handleValueFilter(filterClassName, tableNames, filterRef, familiesAndQualifiers);
                break;
            case HBaseInfo.ROW_FILTER:
                handleRowFilter(filterClassName, tableNames, filterRef);
                break;
            case HBaseInfo.FUZZY_ROW_FILTER:
            case HBaseInfo.PREFIX_FILTER:
                handleKeySearchFilter(filterClassName, tableNames);
                break;
            case HBaseInfo.QUALIFIER_FILTER:
                handleQualifierFilter(filterClassName, tableNames, filterRef);
                break;
            case HBaseInfo.COLUMN_PREFIX_FILTER:
            case HBaseInfo.MULTIPLE_COLUMN_PREFIX_FILTER:
            case HBaseInfo.COLUMN_RANGE_FILTER:
                handleColumnSearchFilter(filterClassName, tableNames);
                break;
            case HBaseInfo.FAMILY_FILTER:
                handleFamilyFilter(filterClassName, tableNames, filterRef);
                break;
            case HBaseInfo.SKIP_FILTER:
            case HBaseInfo.WHILE_MATCH_FILTER:
                handleWrapperFilter(tableNames, filterRef, familiesAndQualifiers);
                break;
            case HBaseInfo.MULTIPLE_ROW_RANGE_FILTER:
                handleMultipleRowRangeFilter(filterClassName, tableNames);
                break;
            case HBaseInfo.FILTER_LIST:
                handleFilterList(tableNames, filterRef, familiesAndQualifiers);
                break;
            case HBaseInfo.FILTER_LIST_WITH_AND:
            case HBaseInfo.FILTER_LIST_WITH_OR:
                handleFilterListWith(tableNames, filterRef, familiesAndQualifiers);
                break;
            case HBaseInfo.FILTER:
            case HBaseInfo.FILTER_BASE:
            case HBaseInfo.COMPARE_FILTER:
            case HBaseInfo.FILTER_LIST_BASE:
                obtainConcreteFilterFromAbstract(tableNames, filterRef, familiesAndQualifiers);
                break;
            default:
                logger.error("Unknown or unsupported filter %s", filterClassName);
                break;
        }
    }

    /**
     * Analyses a ColumnValueFilter object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     * @param filterRef filter object reference.
     */
    protected void handleColumnValueFilter(String filterClass, List<String> tableNames, ValueState filterRef) {
        List<InvokeExprState> filterCreateExprStateList =
                CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
        for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
            if (filterCreateExpr.getArgCount() == 4) {
                List<String> colFamilyNames = CodeAnalyser.getStringsFromStringValueStates(
                        HBase2Plugin.getStringFromToBytesMethod(filterCreateExpr.getArg(0)));
                List<String> colQualifierNames = CodeAnalyser.getStringsFromStringValueStates(
                        HBase2Plugin.getStringFromToBytesMethod(filterCreateExpr.getArg(1)));
                List<StringValueState> compareOpEnumValues = CodeAnalyser.getEnumValuesAssignedToVariable(
                        HBaseInfo2.COMPARE_OPERATOR_ENUM, filterCreateExpr.getArg(2));
                String comparatorClass = filterCreateExpr.getArg(3).getValueClassName();
                requirementGenerator.generateFilterRequirement(filterClass, tableNames, colFamilyNames,
                        colQualifierNames, CodeAnalyser.getStringsFromStringValueStates(compareOpEnumValues),
                        comparatorClass);
            } else {
                logger.error("Unknown init expression of ColumnFilter.");
            }
        }
    }

    /**
     * Analyses a ValueFilter object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     * @param filterRef filter object reference.
     * @param familiesAndQualifiers the operation object column families and qualifiers.
     */
    protected void handleValueFilter(String filterClass, List<String> tableNames, ValueState filterRef,
                                     List<ColumnFamilyAndQualifier> familiesAndQualifiers) {
        if (!familiesAndQualifiers.isEmpty()) {
            List<InvokeExprState> filterCreateExprStateList =
                    CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
            for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
                if (filterCreateExpr.getArgCount() == 2) {
                    List<StringValueState> compareOpEnumValues = CodeAnalyser.getEnumValuesAssignedToVariable(
                            HBaseInfo2.COMPARE_OPERATOR_ENUM, filterCreateExpr.getArg(0));
                    String comparatorClass = filterCreateExpr.getArg(1).getValueClassName();
                    for (ColumnFamilyAndQualifier famAndQua : familiesAndQualifiers) {
                        if (famAndQua.getQualifiers().isEmpty())
                            requirementGenerator.generateFilterRequirement(filterClass, tableNames,
                                    famAndQua.getStringFamilies(),
                                    CodeAnalyser.getStringsFromStringValueStates(compareOpEnumValues), comparatorClass);
                        else
                            requirementGenerator.generateFilterRequirement(filterClass, tableNames,
                                    famAndQua.getStringFamilies(), famAndQua.getStringQualifiers(),
                                    CodeAnalyser.getStringsFromStringValueStates(compareOpEnumValues), comparatorClass);
                    }
                } else {
                    logger.error("Unknown init expression of ValueFilter.");
                }
            }
        }
    }

    /**
     * Analyses a RowFilter object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     */
    protected void handleRowFilter(String filterClass, List<String> tableNames, ValueState filterRef) {
        List<InvokeExprState> filterCreateExprStateList =
                CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
        for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
            if (filterCreateExpr.getArgCount() == 2) {
                List<StringValueState> compareOpEnumValues = CodeAnalyser.getEnumValuesAssignedToVariable(
                        HBaseInfo2.COMPARE_OPERATOR_ENUM, filterCreateExpr.getArg(0));
                String comparatorClass = filterCreateExpr.getArg(1).getValueClassName();
                requirementGenerator.generateKeyFilterRequirement(filterClass, tableNames,
                        CodeAnalyser.getStringsFromStringValueStates(compareOpEnumValues), comparatorClass);
            } else {
                logger.error("Unknown init expression of RowFilter.");
            }
        }
    }

    /**
     * Analyses a QualifierFilter object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     */
    protected void handleQualifierFilter(String filterClass, List<String> tableNames, ValueState filterRef) {
        List<InvokeExprState> filterCreateExprStateList =
                CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
        for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
            if (filterCreateExpr.getArgCount() == 2) {
                List<StringValueState> compareOpEnumValues = CodeAnalyser.getEnumValuesAssignedToVariable(
                        HBaseInfo2.COMPARE_OPERATOR_ENUM, filterCreateExpr.getArg(0));
                String comparatorClass = filterCreateExpr.getArg(1).getValueClassName();
                requirementGenerator.generateQualifierFilterRequirement(filterClass, tableNames,
                        CodeAnalyser.getStringsFromStringValueStates(compareOpEnumValues), comparatorClass);
            } else {
                logger.error("Unknown constructor of RowFilter.");
            }
        }
    }

    /**
     * Analyses a FamilyFilter object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     */
    protected void handleFamilyFilter(String filterClass, List<String> tableNames, ValueState filterRef) {
        List<InvokeExprState> filterCreateExprStateList =
                CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
        for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
            if (filterCreateExpr.getArgCount() == 2) {
                List<StringValueState> compareOpEnumValues = CodeAnalyser.getEnumValuesAssignedToVariable(
                        HBaseInfo2.COMPARE_OPERATOR_ENUM, filterCreateExpr.getArg(0));
                String comparatorClass = filterCreateExpr.getArg(1).getValueClassName();
                requirementGenerator.generateFamilyFilterRequirement(filterClass, tableNames,
                        CodeAnalyser.getStringsFromStringValueStates(compareOpEnumValues), comparatorClass);
            } else {
                logger.error("Unknown constructor of FamilyFilter.");
            }
        }
    }

    /**
     * Analyses a FilterList object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     * @param filterRef filter object reference.
     * @param familiesAndQualifiers the operation object column families and qualifiers.
     */
    protected void handleFilterList(List<String> tableNames, ValueState filterRef,
                                    List<ColumnFamilyAndQualifier> familiesAndQualifiers) {
        List<ValueState> filterObjects = new ArrayList<>();
        List<InvokeExprState> filterCreateExprStateList =
                CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
        for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
            if (filterCreateExpr.getArgCount() == 1) {
                if (CodeAnalyser.isOfType(filterCreateExpr.getArg(0), Constants.JAVA_LIST)) {
                    filterObjects.addAll(CodeAnalyser.getObjsAddedToList(filterCreateExpr.getArg(0)));
                } else if (CodeAnalyser.isArrayOf(filterCreateExpr.getArg(0), HBaseInfo.FILTER)) {
                    filterObjects.addAll(CodeAnalyser.getObjsAddedToArray(filterCreateExpr.getArg(0)));
                } else {
                    logger.error("Unknown constructor of FilterList.");
                }
            } else if (filterCreateExpr.getArgCount() == 2 && CodeAnalyser.isOfType(filterCreateExpr.getArg(1),
                    Constants.JAVA_LIST)) {
                filterObjects.addAll(CodeAnalyser.getObjsAddedToList(filterCreateExpr.getArg(1)));
            } else {
                logger.error("Unknown constructor of FilterList.");
            }
        }
        List<InvokeExprState> filterAddMethodList = CodeAnalyser.findMethodInvocationFromObjectRef(
                HBaseInfo.FILTER_LIST_ADD_FILTER_METHOD, filterRef);
        for (InvokeExprState filterAddMethod : filterAddMethodList) {
            if (filterAddMethod.getArgCount() == 1) {
                if ((CodeAnalyser.isOfType(filterAddMethod.getArg(0), Constants.JAVA_LIST))) {
                    filterObjects.addAll(CodeAnalyser.getObjsAddedToList(filterAddMethod.getArg(0)));
                } else if (CodeAnalyser.isOfType(filterAddMethod.getArg(0), HBaseInfo.FILTER)) {
                    filterObjects.add(filterAddMethod.getArg(0));
                } else {
                    logger.error("Unknown addFilter expression of FilterList.");
                }
            }
            else {
                logger.error("Unknown addFilter expression of FilterList.");
            }
        }
        // Analyse each Filter added to the FilterList
        for (ValueState filterObj : filterObjects)
            handleFilter(tableNames, filterObj, familiesAndQualifiers);
    }

}
