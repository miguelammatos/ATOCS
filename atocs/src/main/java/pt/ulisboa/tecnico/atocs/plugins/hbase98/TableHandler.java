package pt.ulisboa.tecnico.atocs.plugins.hbase98;

import pt.ulisboa.tecnico.atocs.core.Constants;
import pt.ulisboa.tecnico.atocs.plugins.hbase2.HBase2Plugin;
import pt.ulisboa.tecnico.atocs.plugins.hbasecommon.HBaseInfo;
import pt.ulisboa.tecnico.atocs.plugins.hbasecommon.HBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.atocs.core.CodeAnalyser;
import pt.ulisboa.tecnico.atocs.core.datastructures.InvokeExprState;
import pt.ulisboa.tecnico.atocs.core.datastructures.StringValueState;
import pt.ulisboa.tecnico.atocs.core.datastructures.ValueState;

import java.util.ArrayList;
import java.util.List;

public class TableHandler {
    private final Logger logger = LoggerFactory.getLogger(TableHandler.class);

    /**
     * From a HTable method invocation, identifies the corresponding table name. It assumes that a table can only
     * be obtained from its constructor.
     *
     * @param tableInvokeExprState application state when the table method invocation was found.
     * @return list of possible names of the table.
     */
    List<StringValueState> getHTableName(InvokeExprState tableInvokeExprState) {
        List<StringValueState> tableNames = new ArrayList<>();
        ValueState tableRef = tableInvokeExprState.getInstance(); //HTable object
        List<InvokeExprState> tableInitExprs = CodeAnalyser.findObjConstructorInvocationFromObjRef(tableRef);
        for (InvokeExprState initExpr : tableInitExprs) {
            List<StringValueState> possibleTableNames = new ArrayList<>();
            if (initExpr.getArgCount() >= 2) {
                ValueState firstArg = initExpr.getArg(0);
                ValueState secondArg = initExpr.getArg(1);
                if (CodeAnalyser.isOfType(firstArg, HBaseInfo.TABLENAME_CLASS)) {
                    possibleTableNames = getStringValueStatesFromTableNameArg(firstArg);
                } else if (CodeAnalyser.isArrayOfBytes(firstArg)) {
                    possibleTableNames = HBasePlugin.getStringFromToBytesMethod(firstArg);
                } else if (CodeAnalyser.isOfType(secondArg, Constants.STRING_CLASS)) {
                    possibleTableNames = CodeAnalyser.getStringAssignedToVariable(secondArg);
                } else if (CodeAnalyser.isArrayOfBytes(secondArg)) {
                    possibleTableNames = HBasePlugin.getStringFromToBytesMethod(secondArg);
                } else if (CodeAnalyser.isOfType(secondArg, HBaseInfo.TABLENAME_CLASS)) {
                    possibleTableNames = getStringValueStatesFromTableNameArg(secondArg);
                }
            }
            for (StringValueState tableName : possibleTableNames) {
                if (!tableName.getStringValue().equals("?"))
                    tableNames.add(tableName);
            }
        }
        return tableNames;
    }

    private List<StringValueState> getStringValueStatesFromTableNameArg(ValueState arg) {
        List<StringValueState> values = new ArrayList<>();
        List<InvokeExprState> tableNameValueOfMethodList = CodeAnalyser.findMethodInvocationAssignedToVariable(
                HBaseInfo.TABLENAME_CLASS, HBaseInfo.TABLENAME_VALUE_OF_METHOD, arg);
        for (InvokeExprState tableNameValueOfMethod : tableNameValueOfMethodList) {
            if (tableNameValueOfMethod.getArgCount() == 1) {
                if (CodeAnalyser.isOfType(tableNameValueOfMethod.getArg(0), Constants.STRING_CLASS)) {
                    values.addAll(CodeAnalyser.getStringAssignedToVariable(
                            tableNameValueOfMethod.getArg(0)));
                } else if (CodeAnalyser.isArrayOfBytes(tableNameValueOfMethod.getArg(0))) {
                    values.addAll(HBase2Plugin.getStringFromToBytesMethod(
                            tableNameValueOfMethod.getArg(0)));
                }
            } else if (tableNameValueOfMethod.getArgCount() == 3) {
                values.addAll(HBase2Plugin.getStringFromToBytesMethod(
                        tableNameValueOfMethod.getArg(0)));
            } else {
                logger.error("Unable to recognise method " + tableNameValueOfMethod.getValue());
            }
        }
        return values;
    }

}
