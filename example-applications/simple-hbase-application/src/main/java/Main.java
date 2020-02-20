import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;

public class Main {
    public static void main(String[] args) throws IOException {
//        new App0(1);
        new App1("FirstTable");
//        new App2();
    }

}

