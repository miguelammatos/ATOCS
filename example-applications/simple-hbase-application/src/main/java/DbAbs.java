import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

public abstract class DbAbs {
    private Table myTable;

    public Table getMyTable() {
        return myTable;
    }

    public void setMyTable(Table myTable) {
        this.myTable = myTable;
    }

    public abstract void createTable(String tableName) ;

    public abstract void deleteTable(String tableName) ;

    public abstract void put(TableName tableName, String row, String columnFamily, String columnQualifier, String value) ;

    public abstract void put(String tableName, byte[] row, byte[] columnFamily, byte[] columnQualifier, byte[] value) ;

    public abstract Get getGetOp(String row) ;

    public abstract Result get(String tableName, Get get) ;

    public abstract void get(String tableName, byte[] row) ;

    public abstract void getWithFilterList(String tableName, byte[] row) ;

    public abstract void getRecursive(String tableName, byte[] row) ;
}
