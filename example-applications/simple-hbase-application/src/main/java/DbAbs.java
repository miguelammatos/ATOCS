import org.apache.hadoop.hbase.client.Table;

public class DbAbs {
    private Table myTable;

    public Table getMyTable() {
        return myTable;
    }

    public void setMyTable(Table myTable) {
        this.myTable = myTable;
    }
}
