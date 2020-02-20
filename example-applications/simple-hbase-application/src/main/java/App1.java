import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;

public class App1 {
    private String testTable;
    private static final String TABLE_NAME = "TestTable";
    private static final String COLUMN_FAMILY_NAME = "Personal";
    private static final String COLUMN_NAME = "Name";

    App1(String tableName) throws IOException {
        Configuration config = HBaseConfiguration.create();

        Connection connection = ConnectionFactory.createConnection(config);
        Admin admin = connection.getAdmin();

        HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(TABLE_NAME));
        tableDescriptor.addFamily(new HColumnDescriptor(toBytes(COLUMN_FAMILY_NAME)));
        admin.createTable(tableDescriptor);

        //Obtain tables
        testTable = "UserTable";
        Table table1 = connection.getTable(TableName.valueOf(testTable));
        Table table2 = getOtherTable(connection);

        //Put Operation
        Put put = new Put(Bytes.toBytes("row1"));
        put.addColumn(Bytes.toBytes(COLUMN_FAMILY_NAME), Bytes.toBytes(COLUMN_NAME), Bytes.toBytes("John"));
        table1.put(put);

        //Get Operation
        Get get = new Get(Bytes.toBytes("row1"));
        Result getResult = table2.get(get);
        String greeting = Bytes.toString(getResult.getValue(Bytes.toBytes(COLUMN_FAMILY_NAME), Bytes.toBytes(COLUMN_NAME)));
        System.out.println("READ: " + greeting);

        //Scan Operation
        Scan scan = new Scan();
        scan.withStartRow(Bytes.toBytes("row1")).withStopRow(Bytes.toBytes("row5"));
        ResultScanner scannerResult = table2.getScanner(scan);
        for (Result result : scannerResult) System.out.println("Scan Result: " + result.toString());

        //Delete Operation
        table1.delete(new Delete(Bytes.toBytes("row1")));

        admin.disableTable(TableName.valueOf(TABLE_NAME));
        admin.deleteTable(TableName.valueOf(TABLE_NAME));
        admin.close();
    }

    Table getOtherTable (Connection connection) throws IOException {
        return connection.getTable(getOtherTableName(getOtherTableNameString()));
    }

    TableName getOtherTableName(String table) {
        return TableName.valueOf(table);
    }

    public String getOtherTableNameString() {
        return "OtherTable";
    }

}
