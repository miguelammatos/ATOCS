import org.apache.hadoop.hbase.filter.Filter;

public abstract class FilterAbstract {
    public abstract Filter getFilter(byte[] fam);
    public abstract Filter getFilterRecursive(byte[] fam, byte[] qua, String bla);
}
