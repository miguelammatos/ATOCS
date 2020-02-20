import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;

public class Db {
    private Connection connection;

    public Db() {
        Configuration config = HBaseConfiguration.create();
        try {
            this.connection = ConnectionFactory.createConnection(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTable(String tableName) {
        try {
            Admin admin = connection.getAdmin();
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(Fields.TABLE_NAME));
            tableDescriptor.addFamily(new HColumnDescriptor(toBytes(Fields.COLUMN_FAMILY_NAME)));
            admin.createTable(tableDescriptor);
            admin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteTable(String tableName) {
        try {
            Admin admin = connection.getAdmin();
            admin.disableTable(TableName.valueOf(tableName));
            admin.deleteTable(TableName.valueOf(tableName));
            admin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(TableName tableName, String row, String columnFamily, String columnQualifier, String value) {
        Table table = null;
        try {
            table = connection.getTable(tableName);
            Put put = new Put(Bytes.toBytes(row));
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier), Bytes.toBytes(value));
            table.put(put);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(String tableName, byte[] row, byte[] columnFamily, byte[] columnQualifier, byte[] value) {
        Table table = null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Put put = new Put(row);
            put.addColumn(columnFamily, columnQualifier, value);
            table.put(put);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Result get(String tableName, String row) {
        try {
            Table table = connection.getTable(TableName.valueOf(tableName));
            return table.get(new Get(Bytes.toBytes(row)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Result get(String tableName, byte[] row) {
        try {
            Table table = connection.getTable(TableName.valueOf(tableName));
            return table.get(new Get(row));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
