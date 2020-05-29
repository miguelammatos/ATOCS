import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;

public class Main {
    public static void main(String[] args) throws IOException {
//        new App0(1);
//        new App1("FirstTable");
//        new App2();
        Configuration config = HBaseConfiguration.create();
        config.addResource("src/main/java/conf.xml");
        Connection connection = ConnectionFactory.createConnection(config);
        Admin admin = connection.getAdmin();
        ClusterStatus clusterStatus = admin.getClusterStatus();

        for (ServerName serverName : clusterStatus.getServers()) {
            ServerLoad serverLoad = clusterStatus.getLoad(serverName);

            for (Map.Entry<byte[], RegionLoad> entry : serverLoad.getRegionsLoad().entrySet()) {
                final String region = Bytes.toString(entry.getKey());
                final RegionLoad regionLoad = entry.getValue();
                long storeFileSize = regionLoad.getStorefileSizeMB();
                System.out.println("Stored in " + region + ": " + storeFileSize);
                // other useful thing in regionLoad if you like
            }
        }
    }

}

