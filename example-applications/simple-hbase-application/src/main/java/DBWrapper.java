import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

public class DBWrapper extends DbAbs {
    DbAbs db;

    public DBWrapper() {
        this.db = new Db();
    }

    public void createTable(String tableName) {
        db.createTable(tableName);
    }

    public void deleteTable(String tableName) {
        db.deleteTable(tableName);
    }

    public void put(TableName tableName, String row, String columnFamily, String columnQualifier, String value) {
        db.put(tableName, row, columnFamily, columnQualifier, value);
    }

    public void put(String tableName, byte[] row, byte[] columnFamily, byte[] columnQualifier, byte[] value) {
        db.put(tableName, row, columnFamily, columnQualifier, value);
    }

    public Get getGetOp(String row) {
        return db.getGetOp(row);
    }

    @Override
    public Get getObjWithFilter(Get get) {
        return db.getObjWithFilter(get);
    }

    public Result getWithGetOp(String tableName) {
        return db.getWithGetOp(tableName);
    }

    public void get(String tableName, byte[] row) {
        db.get(tableName, row);
    }

    public void getWithFilterList(String tableName, byte[] row) {
       db.getWithFilterList(tableName, row);
    }

    public void getRecursive(String tableName, byte[] row) {
        db.getRecursive(tableName, row);
    }
}
