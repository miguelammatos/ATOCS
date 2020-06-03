package atocs.plugins.hbase2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import atocs.core.datastructures.StringValueState;
import atocs.core.datastructures.InvokeExprState;
import atocs.plugins.hbasecommon.*;

import java.util.*;

public class HBase2Plugin extends HBasePlugin {
    public enum HBaseDbName {
        HBASE2("hbase2"),
        SAFENOSQL ("safenosql");
        private final String value;

        HBaseDbName(String s) {
            value = s;
        }
        public String toString() {
            return this.value;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(HBase2Plugin.class);

    private final TableHandler tableHandler;
    private final FilterHandler2 filterHandler;

    private final HBaseDbName hBaseDbName;

    public HBase2Plugin(HBaseDbName databaseName){
        super();
        hBaseDbName = databaseName;
        tableHandler = new TableHandler();
        filterHandler = new FilterHandler2(this.requirementGenerator);
    }

    protected FilterHandler getFilterHandler() {
        return filterHandler;
    }

    @Override
    public String getApiFilePath() {
        switch (hBaseDbName) {
            case SAFENOSQL:
                return HBaseInfo2.SAFENOSQL_API_FILE_PATH;
            case HBASE2:
            default:
                return HBaseInfo2.HBASE2_API_FILE_PATH;
        }
    }

    @Override
    public List<String> getLibPaths() {
        List<String> libPaths = new ArrayList<>();
        libPaths.add(HBaseInfo2.HBASE2_LIB_PATH);
        if (hBaseDbName.equals(HBaseDbName.SAFENOSQL)) {
            libPaths.add(HBaseInfo2.HBASE2_LIB_PATH);
            libPaths.add(HBaseInfo2.SAFENOSQL_LIB_PATH1);
            libPaths.add(HBaseInfo2.SAFENOSQL_LIB_PATH2);
        }
        return libPaths;
    }

    /**
     * Analyses a database interaction.
     *
     * @param invokeExprState application state when the database interaction was found.
     */
    @Override
    public void analyseDbInteraction(InvokeExprState invokeExprState) {
        List<StringValueState> tableNames;
        switch (hBaseDbName) {
            case SAFENOSQL:
                tableNames = tableHandler.getCryptoTableName(invokeExprState);
                break;
            case HBASE2:
            default:
                tableNames = tableHandler.getTableName(invokeExprState);
                break;
        }

        if (tableNames.isEmpty()) {
            logger.error("Unable to determine table name in " + invokeExprState.getScopeMethod());
            return;
        }

        String methodOperation = api.getMethodOperation(invokeExprState);
        switch (methodOperation) {
            case "PUT":
                handlePutOperation(tableNames);
                break;
            case "GET":
                handleGetOperation(invokeExprState, tableNames);
                break;
            case "SCAN":
                handleScanOperation(invokeExprState, tableNames);
                break;
            case "DELETE":
                handleDeleteOperation(tableNames);
                break;
            default:
                logger.error("Unknown HBase operation");
                break;
        }
    }
}
