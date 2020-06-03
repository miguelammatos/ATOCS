package atocs.plugins.hbase98;

public class HBaseInfo98 {

    //HBase API info
    public static final String HBASE98_API_FILE_PATH = "src/main/java/system/modules/hbase98/resources/hbase98.yml";
    public static final String HBASE98_LIB_PATH = "src/main/java/system/modules/hbase98/resources/hbase-client-0.98.6-hadoop2.jar";

    //Bytes class and methods
    public static final String BYTES_CLASS = "org.apache.hadoop.hbase.util.Bytes";
    public static final String BYTES_TO_BYTES_METHOD = "toBytes";

    //Table class and methods
    public static final String HTABLE_CLASS = "org.apache.hadoop.hbase.client.HTable";
    public static final String TABLENAME_CLASS = "org.apache.hadoop.hbase.TableName";
    public static final String TABLENAME_VALUE_OF_METHOD = "valueOf";

    //Filter class and methods
    public static final String FILTER_LIST_OPERATOR_ENUM = "org.apache.hadoop.hbase.filter.FilterList$Operator";

    //CompareOp enum
    public static final String COMPARE_OP_ENUM = "org.apache.hadoop.hbase.filter.CompareFilter$CompareOp";

}
