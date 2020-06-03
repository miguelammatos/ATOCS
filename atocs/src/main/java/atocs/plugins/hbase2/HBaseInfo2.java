package atocs.plugins.hbase2;

public class HBaseInfo2 {
    //HBase API info
    public static final String HBASE2_API_FILE_PATH = "src/main/java/system/modules/hbase2/resources/hbase.yml";
    public static final String HBASE2_LIB_PATH = "src/main/java/system/modules/hbase2/resources/hbase-client-2.2.3.jar";

    //SafeNoSQL API info
    public static final String SAFENOSQL_API_FILE_PATH = "src/main/java/system/modules/hbase2/resources/safenosql.yml";
    public static final String SAFENOSQL_LIB_PATH1 = "src/main/java/system/modules/hbase2/resources/safeclient-2.2.0.jar";
    public static final String SAFENOSQL_LIB_PATH2 = "src/main/java/system/modules/hbase2/resources/safemapper-2.2.0.jar";

    //SafeNoSQL classes
    public static final String CRYPTO_TABLE_CLASS = "pt.uminho.haslab.safeclient.secureTable.CryptoTable";

    //Table class and methods
    public static final String TABLE_INTERFACE = "org.apache.hadoop.hbase.client.Table";
    public static final String CONNECTION_CLASS = "org.apache.hadoop.hbase.client.Connection";
    public static final String CONNECTION_GET_TABLE_METHOD = "getTable";
    public static final String TABLENAME_CLASS = "org.apache.hadoop.hbase.TableName";
    public static final String TABLENAME_VALUE_OF_METHOD = "valueOf";

    //CompareOperator class and methods
    public static final String COMPARE_OPERATOR_ENUM = "org.apache.hadoop.hbase.CompareOperator";

}
