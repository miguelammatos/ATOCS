import org.apache.hadoop.hbase.util.Bytes;

public class App2 {
    App2() {
        DbAbs db = new DBWrapper();

        db.createTable(Fields.TABLE_NAME);

        db.getWithGetOp(Fields.TABLE_NAME);
//        db.get(Fields.TABLE_NAME, Bytes.toBytes("row1"));
//        db.getWithFilterList(Fields.TABLE_NAME, Bytes.toBytes("row1"));
//        db.getRecursive(Fields.TABLE_NAME, Bytes.toBytes("row1"));

        db.deleteTable(Fields.TABLE_NAME);
    }
}
