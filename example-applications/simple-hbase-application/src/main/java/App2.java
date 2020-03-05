import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.filter.ColumnValueFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;

public class App2 {
    App2() {
        Db db = new Db();

//        db.createTable(Fields.TABLE_NAME);

//        db.put(TableName.valueOf(Fields.TABLE_NAME), "row1", Fields.COLUMN_NAME, Fields.COLUMN_NAME, "John");
//        Get get = new Get(Bytes.toBytes("row1"));
//        get.setFilter(new SingleColumnValueFilter(Bytes.toBytes("User"), Bytes.toBytes("Name"), CompareOperator.GREATER, Bytes.toBytes("Bla")));
//        Result result = db.get(Fields.TABLE_NAME, new Get(Bytes.toBytes("row1")));
//        Get get = db.getGetOp("row1");
//        get.setFilter(new ColumnValueFilter(Bytes.toBytes("User"), Bytes.toBytes("Address"),
//                CompareOperator.EQUAL, new SubstringComparator("Jo")));
//        Result result = db.get(Fields.TABLE_NAME, get);
        db.get(Fields.TABLE_NAME, Bytes.toBytes("row1"));
//        String greeting = Bytes.toString(result.getValue(Bytes.toBytes(Fields.COLUMN_FAMILY_NAME), Bytes.toBytes(Fields.COLUMN_NAME)));
//        System.out.println("READ: " + greeting);

//        db.deleteTable(Fields.TABLE_NAME);
    }
}
