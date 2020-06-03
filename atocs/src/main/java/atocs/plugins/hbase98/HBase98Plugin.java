package atocs.plugins.hbase98;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import atocs.core.datastructures.StringValueState;
import atocs.core.datastructures.InvokeExprState;
import atocs.plugins.hbasecommon.FilterHandler;
import atocs.plugins.hbasecommon.HBasePlugin;

import java.util.ArrayList;
import java.util.List;

public class HBase98Plugin extends HBasePlugin {
    public enum HBaseDbName {
        HBASE98("hbase98");
        private final String value;

        HBaseDbName(String s) {
            value = s;
        }
        public String toString() {
            return this.value;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(HBase98Plugin.class);

    private final TableHandler tableHandler;
    private final FilterHandler98 filterHandler;

    private final HBaseDbName hBaseDbName;

    public HBase98Plugin(HBaseDbName databaseName){
        super();
        hBaseDbName = databaseName;
        tableHandler = new TableHandler();
        filterHandler = new FilterHandler98(this.requirementGenerator);
    }

    @Override
    protected FilterHandler getFilterHandler() {
        return filterHandler;
    }

    @Override
    public String getApiFilePath() {
        switch (hBaseDbName) {
            case HBASE98:
            default:
                return HBaseInfo98.HBASE98_API_FILE_PATH;
        }
    }

    @Override
    public List<String> getLibPaths() {
        List<String> libPaths = new ArrayList<>();
        libPaths.add(HBaseInfo98.HBASE98_LIB_PATH);
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
            case HBASE98:
            default:
                tableNames = tableHandler.getHTableName(invokeExprState);
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
