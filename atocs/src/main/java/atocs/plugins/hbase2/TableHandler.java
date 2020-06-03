package atocs.plugins.hbase2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import atocs.core.CodeAnalyser;
import atocs.core.Constants;
import atocs.core.datastructures.InvokeExprState;
import atocs.core.datastructures.StringValueState;
import atocs.core.datastructures.ValueState;

import java.util.ArrayList;
import java.util.List;

public class TableHandler {
    private final Logger logger = LoggerFactory.getLogger(TableHandler.class);

    /**
     * From a Table method invocation, identifies the corresponding table name. It assumes that a table can only be
     * obtained from the combination of the following methods: Connection.getTable(TableName.valueOf("tableName"))
     *
     * @param tableInvokeExprState application state when the table method invocation was found.
     * @return list of possible names of the table.
     */
    List<StringValueState> getTableName(InvokeExprState tableInvokeExprState) {
        List<StringValueState> possibleTableNames = new ArrayList<>();
        ValueState tableRef = tableInvokeExprState.getInstance(); //Table object
        List<InvokeExprState> connectionGetTableMethodList = CodeAnalyser.findMethodInvocationAssignedToVariable(
                HBaseInfo2.CONNECTION_CLASS, HBaseInfo2.CONNECTION_GET_TABLE_METHOD, tableRef);
        for (InvokeExprState connectionGetTableMethod : connectionGetTableMethodList) {
            if (connectionGetTableMethod.getArgCount() == 1) {
                //TableName, arg of method Connection.getTable(...)
                List<InvokeExprState> tableNameValueOfMethodList = CodeAnalyser.findMethodInvocationAssignedToVariable(
                        HBaseInfo2.TABLENAME_CLASS, HBaseInfo2.TABLENAME_VALUE_OF_METHOD,
                        connectionGetTableMethod.getArg(0));
                for (InvokeExprState tableNameValueOfMethod : tableNameValueOfMethodList) {
                    List<StringValueState> tableNames = new ArrayList<>();
                    if (tableNameValueOfMethod.getArgCount() == 1) {
                        if (CodeAnalyser.isOfType(tableNameValueOfMethod.getArg(0), Constants.STRING_CLASS)) {
                            //String, arg of method TableName.valueOf(...)
                            tableNames.addAll(CodeAnalyser.getStringAssignedToVariable(
                                    tableNameValueOfMethod.getArg(0)));
                        } else if (CodeAnalyser.isArrayOfBytes(tableNameValueOfMethod.getArg(0))) {
                            tableNames.addAll(HBase2Plugin.getStringFromToBytesMethod(
                                    tableNameValueOfMethod.getArg(0)));
                        }
                    } else if (tableNameValueOfMethod.getArgCount() == 3) {
                        tableNames.addAll(HBase2Plugin.getStringFromToBytesMethod(
                                tableNameValueOfMethod.getArg(0)));
                    } else {
                        logger.error("Cannot recognize method " + tableNameValueOfMethod.getValue());
                    }
                    for (StringValueState tableName : tableNames) {
                        if (!tableName.getStringValue().equals("?"))
                            possibleTableNames.add(tableName);
                    }
                }
            } else {
                logger.error("Cannot recognize method " + connectionGetTableMethod.getValue());
            }
        }
        return possibleTableNames;
    }

    /**
     * From a CryptoTable method invocation, identifies the corresponding table name. It assumes that a table can only
     * be obtained from its constructor.
     *
     * @param tableInvokeExprState application state when the table method invocation was found.
     * @return list of possible names of the table.
     */
    List<StringValueState> getCryptoTableName(InvokeExprState tableInvokeExprState) {
        List<StringValueState> tableNames = new ArrayList<>();
        ValueState tableRef = tableInvokeExprState.getInstance(); //CryptoTable object
        List<InvokeExprState> tableInitExprs = CodeAnalyser.findObjConstructorInvocationFromObjRef(tableRef);
        for (InvokeExprState initExpr : tableInitExprs) {
            if (initExpr.getArgCount() >= 2) {
                List<StringValueState> possibleTableNames = CodeAnalyser.getStringAssignedToVariable(
                        initExpr.getArg(1));
                for (StringValueState tableName : possibleTableNames) {
                    if (!tableName.getStringValue().equals("?"))
                        tableNames.add(tableName);
                }
            } else {
                logger.error("Unknown CryptoTable constructor.");
            }
        }
        return tableNames;
    }
}
