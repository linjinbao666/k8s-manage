package ama;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class HBaseTest {

    private static final String ZOOKEEPER_QUORUM = "*.16.21.190:2181";

    public static final String TABLE_NAME = "linjb-test01";

    private Connection connection;

    private Configuration configuration;

    @Before
    public void setUp() throws IOException {
        configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", ZOOKEEPER_QUORUM);
        connection = ConnectionFactory.createConnection(configuration);
    }

    @Test
    public void createTable() throws Exception {
        Admin admin = connection.getAdmin();
        TableName tableName = TableName.valueOf(TABLE_NAME);

        TableDescriptorBuilder tableDescriptorBuilder = TableDescriptorBuilder.newBuilder(tableName);
        ColumnFamilyDescriptor baseInfo = ColumnFamilyDescriptorBuilder.of("base_info");
        ColumnFamilyDescriptor extInfo = ColumnFamilyDescriptorBuilder.of("ext_info");
        tableDescriptorBuilder.setColumnFamily(baseInfo);
        tableDescriptorBuilder.setColumnFamily(extInfo);

        TableDescriptor tableDescriptor = tableDescriptorBuilder.build();
        admin.createTable(tableDescriptor);
    }

    @Test
    public void dealTable() throws IOException {
        Admin admin = connection.getAdmin();
        String tableNameString = "test_user";
        TableName tableName = TableName.valueOf(tableNameString);
        if(admin.tableExists(tableName)){
            admin.disableTable(tableName);
            admin.deleteTable(tableName);
        }
    }

    @Test
    public void descTable() throws IOException {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        TableDescriptor tableDescriptor = table.getDescriptor();
        ColumnFamilyDescriptor[] columnFamilies = tableDescriptor.getColumnFamilies();
        for(ColumnFamilyDescriptor cfd : columnFamilies){
            System.out.println(Bytes.toString(cfd.getName()));
        }
    }

    @Test
    public void put() throws Exception {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        Put put=new Put(Bytes.toBytes("user_info_1"));
//        put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(columnName), Bytes.toBytes(value));
        put.addColumn(Bytes.toBytes("base_info"), Bytes.toBytes("name"), Bytes.toBytes("tim"));
        put.addColumn(Bytes.toBytes("base_info"), Bytes.toBytes("tel"), Bytes.toBytes("123"));
        table.put(put);
    }

    @Test
    public void puts() throws Exception {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        LinkedList<Put> puts = new LinkedList<>();
        puts.add(getPut("user_info_2"));
        puts.add(getPut("user_info_3"));
        puts.add(getPut("user_info_4"));
        table.put(puts);
    }

    private static Put getPut(String rowKey){
        Put put=new Put(Bytes.toBytes(rowKey));
        Random random = new Random();
        String name = String.valueOf(random.nextInt(1000000));
        put.addColumn(Bytes.toBytes("base_info"), Bytes.toBytes("name"), Bytes.toBytes(name));
        put.addColumn(Bytes.toBytes("base_info"), Bytes.toBytes("tel"), Bytes.toBytes(name));
        return put;
    }


    @Test
    public void get() throws Exception {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        Get get =new Get(Bytes.toBytes("user_info_1"));
        get.addColumn(Bytes.toBytes("base_info"), Bytes.toBytes("name"));
        Result result=table.get(get);

        List<Cell> cells = result.listCells();
        for(Cell cell: cells){
            System.out.println(Bytes.toString(cell.getFamilyArray()));
            System.out.println(Bytes.toString(cell.getQualifierArray()));
            System.out.println(Bytes.toString(cell.getValueArray()));
            System.out.println(cell.getTimestamp());
        }
    }

    @Test
    public void gets() throws IOException {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        LinkedList<Get> gets = new LinkedList<>();
        gets.add(getGet("user_info_1"));
        gets.add(getGet("user_info_2"));
        gets.add(getGet("user_info_3"));
        gets.add(getGet("user_info_4"));
        Result[] results = table.get(gets);
        for(Result result : results){
            System.out.println(new String(result.getRow()));
            System.out.println(new String(result.getValue(Bytes.toBytes("base_info"), Bytes.toBytes("name"))));
            System.out.println(new String(result.getValue(Bytes.toBytes("base_info"), Bytes.toBytes("tel"))));
        }
    }

    private static Get getGet(String rowKey){
        Get get =new Get(Bytes.toBytes(rowKey));
        get.addColumn(Bytes.toBytes("base_info"), Bytes.toBytes("name"));
        get.addColumn(Bytes.toBytes("base_info"), Bytes.toBytes("tel"));
        return get;
    }

    @Test
    public void scan() throws IOException {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        Scan s = new Scan();
        ResultScanner resultScanner = table.getScanner(s);
        for(Result result : resultScanner){
            byte[] row = result.getRow();
            System.out.println(new String(row));
            System.out.println(new String(result.getValue(Bytes.toBytes("base_info"), Bytes.toBytes("name"))));
        }
    }

    @Test
    public void deleteRowKey() throws Exception {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        Delete de =new Delete(Bytes.toBytes("rowKey"));
        table.delete(de);
    }

    @Test
    public void deleteColumn() throws Exception {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        Delete de =new Delete(Bytes.toBytes("rowKey"));
        de.addColumn(Bytes.toBytes(""), Bytes.toBytes(""));
        table.delete(de);
    }

    @Test
    public void disableTable() throws Exception {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Admin admin = connection.getAdmin();
        admin.disableTable(tableName);
    }

    @Test
    public void listNamespace() throws Exception {
        Admin admin = connection.getAdmin();
        NamespaceDescriptor[] namespaceDescriptors = admin.listNamespaceDescriptors();
        for(NamespaceDescriptor namespaceDescriptor : namespaceDescriptors){
            System.out.println(namespaceDescriptor.getName());
        }
    }

    @Test
    public void listTables() throws IOException {
        Admin admin = connection.getAdmin();
        List<TableDescriptor> tableDescriptors = admin.listTableDescriptors();
        for(TableDescriptor tableDescriptor : tableDescriptors){
            System.out.println(tableDescriptor.getTableName().getNameAsString());
        }
    }
}
