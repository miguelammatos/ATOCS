import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

public class Main {
    public static void main(String[] args) {
        Db db = new Db();

        db.createTable(Fields.TABLE_NAME);

        db.scan(Fields.TABLE_NAME, new Scan());
        db.increment();
        db.getWithGetOp(Fields.TABLE_NAME);
        db.get(Fields.TABLE_NAME, Bytes.toBytes("row1"));
        db.getWithFilterList(Fields.TABLE_NAME, Bytes.toBytes("row1"));
        db.getRecursive(Fields.TABLE_NAME, Bytes.toBytes("row1"));

        db.deleteTable(Fields.TABLE_NAME);
    }

}

