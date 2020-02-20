import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

public class App2 {
    App2() {
        Db db = new Db();

        db.createTable(Fields.TABLE_NAME);

        db.put(TableName.valueOf(Fields.TABLE_NAME), "row1", Fields.COLUMN_NAME, Fields.COLUMN_NAME, "John");

        Result result = db.get(Fields.TABLE_NAME, "row1");
        result = db.get(Fields.TABLE_NAME, Bytes.toBytes("row1"));
        String greeting = Bytes.toString(result.getValue(Bytes.toBytes(Fields.COLUMN_FAMILY_NAME), Bytes.toBytes(Fields.COLUMN_NAME)));
        System.out.println("READ: " + greeting);

        db.deleteTable(Fields.TABLE_NAME);
    }
}
