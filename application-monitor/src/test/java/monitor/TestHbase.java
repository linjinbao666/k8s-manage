//package monitor;
//
//import lombok.extern.slf4j.Slf4j;
////import org.apache.hadoop.conf.Configuration;
////import org.apache.hadoop.hbase.Cell;
////import org.apache.hadoop.hbase.HBaseConfiguration;
////import org.apache.hadoop.hbase.TableName;
////import org.apache.hadoop.hbase.client.*;
////import org.apache.hadoop.hbase.util.Bytes;
//
//import java.io.IOException;
//
//
//@Slf4j
//public class TestHbase {
//
//    static Configuration conf = null;
//    static Connection conn = null;
//
//    static {
//        conf = HBaseConfiguration.create();
//        conf.set("hbase.zookeeper.quorum", "fdp-master");
//        conf.set("hbase.zookeeper.property.client", "2181");
//        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
//        try {
//            conn = ConnectionFactory.createConnection(conf);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void createTable(TableName tableName, String[] columnFamilys) throws Exception {
//        Admin admin = conn.getAdmin();
//        if (!admin.tableExists(tableName)) {
//            TableDescriptorBuilder tdb = TableDescriptorBuilder.newBuilder(tableName);
//            ColumnFamilyDescriptorBuilder cdb;
//            ColumnFamilyDescriptor cfd;
//            for (String columnFamily : columnFamilys) {
//                cdb = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily));
//                cfd = cdb.build();
//                tdb.setColumnFamily(cfd);
//            }
//            TableDescriptor td = tdb.build();
//            admin.createTable(td);
//        } else {
//            System.out.println("表已存在！");
//        }
//    }
//
//    public static void selectRecords(String tableName) throws IOException {
//        Table table = conn.getTable(TableName.valueOf(tableName));
//        Scan scan = new Scan();
//        //20200908   2020.09.17 18:49:53 028   "20200908^1600334035828^13"
////        scan.setStartRow(Bytes.toBytes("20200809"));
//////        scan.setStopRow(Bytes.toBytes("20200908"));
//        scan.setCaching(1000);
//
//        Bytes.toBytes("");
//        ResultScanner resultScanner = table.getScanner(scan);
//        for (Result rs : resultScanner) {
//            if (rs.rawCells().length == 0) {
//                System.out.println(tableName + "表数据为空！");
//            } else {
//                for (Cell cell : rs.rawCells()) {
//                    byte[] familyArray = cell.getFamilyArray();
//                    byte[] qualifierArray = cell.getQualifierArray();
//                    byte[] valueArray = cell.getValueArray();
//                    System.out.println(new String(familyArray) + "|||" + new String(qualifierArray) + "|||" + new String(valueArray));
//                    System.out.println("---------------------------------------------------------------");
//                }
//            }
//        }
//    }
//
//    public static void main(String[] args) {
//        TableName tableName = TableName.valueOf("test2");
//        String[] columnFamilys = {"article", "author"};
//        try {
////            createTable(tableName, columnFamilys);
//            selectRecords("TraceV2");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
