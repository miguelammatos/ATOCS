package atocs.plugins.hbasecommon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import atocs.core.CodeAnalyser;
import atocs.core.Constants;
import atocs.core.datastructures.InvokeExprState;
import atocs.core.datastructures.ValueState;
import atocs.plugins.hbasecommon.datastructures.ColumnFamilyAndQualifier;

import java.util.ArrayList;
import java.util.List;

public abstract class FilterHandler {
    private final Logger logger = LoggerFactory.getLogger(FilterHandler.class);
    protected RequirementGenerator requirementGenerator;

    public FilterHandler(RequirementGenerator requirementGenerator) {
        this.requirementGenerator = requirementGenerator;
    }

    protected abstract void handleFilter(List<String> tableNames, ValueState filterRef,
                      List<ColumnFamilyAndQualifier> familiesAndQualifiers);

    /**
     * Analyses an abstract Filter object and determines the concrete object reference to analyse.
     *
     * @param tableNames the possible table names.
     * @param filterRef filter object reference.
     * @param familiesAndQualifiers the operation object column families and qualifiers.
     */
    protected void obtainConcreteFilterFromAbstract(List<String> tableNames, ValueState filterRef,
                                                    List<ColumnFamilyAndQualifier> familiesAndQualifiers) {
        List<ValueState> possibleConcreteFilters = CodeAnalyser.getNextValue(filterRef);
        for (ValueState possibleConcreteFilter : possibleConcreteFilters) {
            if (CodeAnalyser.isOfType(possibleConcreteFilter, HBaseInfo.FILTER))
                handleFilter(tableNames, possibleConcreteFilter, familiesAndQualifiers);
        }
    }

    /**
     * Analyses a FuzzyRowFilter or PrefixFilter objects and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     */
    protected void handleKeySearchFilter(String filterClass, List<String> tableNames) {
        requirementGenerator.generateKeySearchRequirement(filterClass, tableNames);
    }

    /**
     * Analyses a wrapper Filter object, determines the wrapped Filter and analyses it.
     *
     * @param tableNames the possible table names.
     * @param filterRef filter object reference.
     * @param familiesAndQualifiers the operation object column families and qualifiers.
     */
    protected void handleWrapperFilter(List<String> tableNames, ValueState filterRef,
                                       List<ColumnFamilyAndQualifier> familiesAndQualifiers) {
        List<InvokeExprState> filterCreateExprStateList =
                CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
        for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
            if (filterCreateExpr.getArgCount() == 1) {
                ValueState filterArgRef = filterCreateExpr.getArg(0);
                handleFilter(tableNames, filterArgRef, familiesAndQualifiers);
            } else {
                logger.error("Unknown constructor of SkipFilter.");
            }
        }
    }

    /**
     * Analyses a FuzzyRowFilter object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     */
    protected void handleMultipleRowRangeFilter(String filterClass, List<String> tableNames) {
        requirementGenerator.generateKeyOrderRequirement(filterClass, tableNames);
    }

    /**
     * Analyses a FilterListWithAND and FilterListWithOR object and determines the database requirements needed.
     *
     * @param tableNames the possible table names.
     * @param filterRef filter object reference.
     * @param familiesAndQualifiers the operation object column families and qualifiers.
     */
    protected void handleFilterListWith(List<String> tableNames, ValueState filterRef,
                                        List<ColumnFamilyAndQualifier> familiesAndQualifiers) {
        List<ValueState> filterObjects = new ArrayList<>();
        List<InvokeExprState> filterCreateExprStateList =
                CodeAnalyser.findObjConstructorInvocationFromObjRef(filterRef);
        for (InvokeExprState filterCreateExpr : filterCreateExprStateList) {
            if (filterCreateExpr.getArgCount() == 1 && CodeAnalyser.isOfType(filterCreateExpr.getArg(0),
                    Constants.JAVA_LIST))
                filterObjects.addAll(CodeAnalyser.getObjsAddedToList(filterCreateExpr.getArg(0)));
            else
                logger.error("Unknown constructor of FilterListWith.");
        }
        List<InvokeExprState> filterAddFilterListsMethodList = CodeAnalyser.findMethodInvocationFromObjectRef(
                HBaseInfo.FILTER_LIST_WITH_ADD_FILTER_LISTS_METHOD, filterRef);
        for (InvokeExprState filterAddFilterListsMethod : filterAddFilterListsMethodList) {
            if (filterAddFilterListsMethod.getArgCount() == 1 && (CodeAnalyser.isOfType(
                    filterAddFilterListsMethod.getArg(0), Constants.JAVA_LIST)))
                filterObjects.addAll(CodeAnalyser.getObjsAddedToList(filterAddFilterListsMethod.getArg(0)));
            else
                logger.error("Unknown addFilter expression of FilterList.");
        }
        // Analyse each Filter added to the FilterList
        for (ValueState filterObj : filterObjects)
            handleFilter(tableNames, filterObj, familiesAndQualifiers);
    }

    /**
     * Analyses a ColumnPrefixFilter, ColumnRangeFilter or MultipleColumnPrefixFilter objects and determines
     * the database requirements needed.
     *
     * @param tableNames the possible table names.
     */
    protected void handleColumnSearchFilter(String filterClass, List<String> tableNames) {
        requirementGenerator.generateColumnSearchRequirement(filterClass, tableNames);
    }

}
