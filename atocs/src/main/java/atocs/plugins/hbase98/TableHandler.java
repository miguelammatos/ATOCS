package atocs.plugins.hbase98;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import atocs.core.CodeAnalyser;
import atocs.core.datastructures.InvokeExprState;
import atocs.core.datastructures.StringValueState;
import atocs.core.datastructures.ValueState;

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
    List<StringValueState> getHTableName(InvokeExprState tableInvokeExprState) { // TODO support all the other HTable constructors
        List<StringValueState> tableNames = new ArrayList<>();
        ValueState tableRef = tableInvokeExprState.getInstance(); //HTable object
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
                logger.error("Unknown HTable constructor.");
            }
        }
        return tableNames;
    }

}
