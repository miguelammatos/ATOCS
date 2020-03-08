import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

public class FilterConcrete extends FilterAbstract {
    public Filter getFilter(byte[] fam) {
        return new SingleColumnValueFilter(fam, Bytes.toBytes("Name"), CompareOperator.GREATER, Bytes.toBytes("Jo"));
    }

    public Filter getFilterRecursive(byte[] fam, byte[] qua, String bla) {
        if (fam == null)
            return getFilterRecursive(Bytes.toBytes("OTHER"), qua, bla);
        return new SingleColumnValueFilter(fam, qua, CompareOperator.GREATER, Bytes.toBytes("Jo"));
    }
}
