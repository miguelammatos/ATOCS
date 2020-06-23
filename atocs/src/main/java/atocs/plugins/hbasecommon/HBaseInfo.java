package atocs.plugins.hbasecommon;

public class HBaseInfo {

    //Bytes class and methods
    public static final String BYTES_CLASS = "org.apache.hadoop.hbase.util.Bytes";
    public static final String BYTES_TO_BYTES_METHOD = "toBytes";

    //Table classes and methods
    public static final String TABLENAME_CLASS = "org.apache.hadoop.hbase.TableName";
    public static final String TABLENAME_VALUE_OF_METHOD = "valueOf";

    //Get class and methods
    public static final String GET_CLASS = "org.apache.hadoop.hbase.client.Get";
    public static final String ADD_COLUMN_METHOD = "addColumn";
    public static final String ADD_FAMILY_METHOD = "addFamily";
    public static final String SET_FILTER_METHOD = "setFilter";

    //Scan class and methods
    public static final String SCAN_CLASS = "org.apache.hadoop.hbase.client.Scan";

    //Increment class and methods
    public static final String INCREMENT_CLASS = "org.apache.hadoop.hbase.client.Increment";

    //Increment class and methods
    public static final String APPEND_CLASS = "org.apache.hadoop.hbase.client.Append";

    //Abstract Filter class and methods
    public static final String FILTER = "org.apache.hadoop.hbase.filter.Filter";
    public static final String FILTER_BASE = "org.apache.hadoop.hbase.filter.FilterBase";
    public static final String COMPARE_FILTER = "org.apache.hadoop.hbase.filter.CompareFilter";
    public static final String FILTER_LIST_BASE = "org.apache.hadoop.hbase.filter.FilterListBase";

    //Concrete Filter class and methods
    public static final String COLUMN_VALUE_FILTER = "org.apache.hadoop.hbase.filter.ColumnValueFilter";
    public static final String SINGLE_COLUMN_VALUE_FILTER = "org.apache.hadoop.hbase.filter.SingleColumnValueFilter";
    public static final String SINGLE_COLUMN_VALUE_EXCLUDE_FILTER =
            "org.apache.hadoop.hbase.filter.SingleColumnValueExcludeFilter";
    public static final String VALUE_FILTER = "org.apache.hadoop.hbase.filter.ValueFilter";
    public static final String ROW_FILTER = "org.apache.hadoop.hbase.filter.RowFilter";
    public static final String FUZZY_ROW_FILTER = "org.apache.hadoop.hbase.filter.FuzzyRowFilter";
    public static final String SKIP_FILTER = "org.apache.hadoop.hbase.filter.SkipFilter";
    public static final String WHILE_MATCH_FILTER = "org.apache.hadoop.hbase.filter.WhileMatchFilter";
    public static final String MULTIPLE_ROW_RANGE_FILTER = "org.apache.hadoop.hbase.filter.MultiRowRangeFilter";
    public static final String FILTER_LIST = "org.apache.hadoop.hbase.filter.FilterList";
    public static final String FILTER_LIST_ADD_FILTER_METHOD = "addFilter";
    public static final String FILTER_LIST_WITH_AND = "org.apache.hadoop.hbase.filter.FilterListWithAND";
    public static final String FILTER_LIST_WITH_OR = "org.apache.hadoop.hbase.filter.FilterListWithOR";
    public static final String FILTER_LIST_WITH_ADD_FILTER_LISTS_METHOD = "addFilterLists";
    public static final String QUALIFIER_FILTER = "org.apache.hadoop.hbase.filter.QualifierFilter";
    public static final String PREFIX_FILTER = "org.apache.hadoop.hbase.filter.PrefixFilter";
    public static final String COLUMN_PREFIX_FILTER = "org.apache.hadoop.hbase.filter.ColumnPrefixFilter";
    public static final String MULTIPLE_COLUMN_PREFIX_FILTER =
            "org.apache.hadoop.hbase.filter.MultipleColumnPrefixFilter";
    public static final String COLUMN_RANGE_FILTER = "org.apache.hadoop.hbase.filter.ColumnRangeFilter";
    public static final String FAMILY_FILTER = "org.apache.hadoop.hbase.filter.FamilyFilter";

    //ByteArrayComparable class and methods
    public static final String BINARY_COMPONENT_COMPARATOR = "org.apache.hadoop.hbase.filter.BinaryComponentComparator";
    public static final String BINARY_PREFIX_COMPARATOR = "org.apache.hadoop.hbase.filter.BinaryPrefixComparator";
    public static final String BIT_COMPARATOR = "org.apache.hadoop.hbase.filter.BitComparator";
    public static final String SUB_STRING_COMPARATOR = "org.apache.hadoop.hbase.filter.SubstringComparator";
    public static final String REGEX_STRING_COMPARATOR = "org.apache.hadoop.hbase.filter.RegexStringComparator";
}
