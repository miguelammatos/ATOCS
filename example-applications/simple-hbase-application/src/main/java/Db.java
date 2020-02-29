import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public Get getGetOp(String row) {
        Get get = new Get(Bytes.toBytes(row));
        get.setFilter(new SingleColumnValueFilter(Bytes.toBytes("User"), Bytes.toBytes("Name"), CompareOperator.GREATER, Bytes.toBytes("Bla")));
        return get;
    }

    public Result get(String tableName, Get get) {
        try {
            Table table = connection.getTable(TableName.valueOf(tableName));
            get.setFilter(getFilter(Bytes.toBytes("User")));
            return table.get(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Filter getFilter(byte[] fam) {
//        return new SingleColumnValueFilter(fam, Bytes.toBytes("Name"), CompareOperator.EQUAL, Bytes.toBytes("Jo"));
        List<MultiRowRangeFilter.RowRange> list = new ArrayList<MultiRowRangeFilter.RowRange>();
        MultiRowRangeFilter.RowRange rowRange = new MultiRowRangeFilter.RowRange(Bytes.toBytes("row1"), true, Bytes.toBytes("row12"), true);
        list.add(rowRange);
        return new MultiRowRangeFilter(list);
    }

    public Result get(String tableName, byte[] row) {
        try {
            Table table = connection.getTable(TableName.valueOf(tableName));
            Get get = new Get(row);
            get.addColumn(Bytes.toBytes("User"), Bytes.toBytes("Addr"));
            get.setFilter(new WhileMatchFilter(getFilter(Bytes.toBytes("FAM"))));
            return table.get(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
