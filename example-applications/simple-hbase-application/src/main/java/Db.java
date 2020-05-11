import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import pt.uminho.haslab.safeclient.secureTable.CryptoTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Db extends DbAbs{
    private Connection connection;
    private FilterAbstract filterAbstract = new FilterConcrete();
    private Table t;


    public Db(){
        Configuration config = HBaseConfiguration.create();
        try {
            this.connection = ConnectionFactory.createConnection(config);
            setMyTable(connection.getTable(TableName.valueOf("MyTable")));
            if (Fields.TABLE_NAME.equals("")) {
                t = new CryptoTable(HBaseConfiguration.create(), "TTTT");
            } else
                t = connection.getTable(TableName.valueOf("TTable"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setFilter(FilterAbstract filter) {
        filterAbstract = filter;
    }

    public void createTable(String tableName) {
        try {
            Admin admin = connection.getAdmin();
            TableDescriptorBuilder tableDescriptorBuilder = TableDescriptorBuilder.newBuilder(TableName.valueOf(Fields.TABLE_NAME));
            TableDescriptor tableDescriptor = tableDescriptorBuilder.build();
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


    public Get getObjWithFilter(Get get) {
        get.setFilter(filterAbstract.getFilter(Bytes.toBytes("User")));
        return get;
    }
    public void getObjWithFilter2(Get get) {
        get.setFilter(filterAbstract.getFilter(Bytes.toBytes("User")));
    }

    public Result getWithGetOp(String tableName) {
        try {
            Get get = new Get(Bytes.toBytes("row1"));
            Table table = connection.getTable(TableName.valueOf(tableName));
            if (get.getFilter() == null)
                get = getObjWithFilter(get);
            return table.get(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void get(String tableName, byte[] row) {
        try {
            Scan scan = new Scan();
            if (tableName.equals(""))
                scan.addColumn("FAM".getBytes(), "QUA".getBytes());
//            else {
//                if (tableName.equals("ola"))
//                    scan.addColumn("FAM".getBytes(), "ADEUS".getBytes());
//                else
//                    scan.addColumn("FAM".getBytes(), "ADE".getBytes());
//            }

            t.getScanner(scan);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getWithFilterList(String tableName, byte[] row) {
        try {
            t = connection.getTable(TableName.valueOf("SpecificTable"));
            Get get = new Get(row);
            getObjWithFilter2(get);
            FilterList filterList = new FilterList(filterAbstract.getFilter(Bytes.toBytes("FAM")), filterAbstract.getFilter(Bytes.toBytes("FAM")));
            get.setFilter(filterList);
            t.get(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getRecursive(String tableName, byte[] row) {
        try {
            Table table = connection.getTable(TableName.valueOf(tableName));
            Get g = new Get(row);
            g.setFilter(filterAbstract.getFilterRecursive(Bytes.toBytes("FAM"), Bytes.toBytes("QUA"), "other"));
            Get get = g;
            table.get(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scan(String tableName, Scan s) {
        try {
            int x = (int) Math.random();
            if (x == 0)
                s.addColumn("FAM".getBytes(), "OLA".getBytes());
            else {
                s.addColumn("FAM".getBytes(), "ADEUS".getBytes());
                t.getScanner(s);
            }

//            if (x == 0)
//                s.addColumn("FAM".getBytes(), "ADEUS".getBytes());

//            if (s.hasFamilies())
//                s.addColumn("FAM".getBytes(), "OLA".getBytes());
//            else
//                s.addColumn("FAM".getBytes(), "QUA".getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void bla(Scan s) {
        if (s.hasFamilies())
            s.addColumn("FAM".getBytes(), "QUA".getBytes());
        else
            s.addColumn("FAM".getBytes(), "QUA".getBytes());
    }

}
