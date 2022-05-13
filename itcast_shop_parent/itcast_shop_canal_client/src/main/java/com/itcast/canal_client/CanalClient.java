package com.itcast.canal_client;

import cn.itcast.canal.bean.CanalRowData;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.itcast.canal_client.kafka.KafkaSender;
import com.itcast.canal_client.util.ConfigUitl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/*
 * canal 客户端程序
 * 与 canal 服务简历连接 获取 canalServer 端的 binlog 日志
 * */
public class CanalClient {
    //    一次拉取binlog数据的条数
    private static final int BATCH_SIZE = 5 * 1024;
    //    canal客户端连接器
    private CanalConnector canalConnector;
// Canal 配置项
    private Properties properties;
    private KafkaSender kafkasender = new KafkaSender();
    //    kafka 生产工具类
    /*
     * 构造方法
     * */
    public CanalClient() {
//        初始化连接
        canalConnector = CanalConnectors.newClusterConnector(
                ConfigUitl.zookeeperServerIp(),
                ConfigUitl.canalServerDestination(),
                ConfigUitl.canalServerUsername(),
                ConfigUitl.canalServerPassword()
        );
    }

    /*
     * 开始执行
     * */
    public void start() {
        try {
//        建立连接
            canalConnector.connect();
//        回滚上次的get请求。
            canalConnector.rollback();
//        订阅的数据库
            canalConnector.subscribe(ConfigUitl.canalSubscribeFilter());
//        可以设置某个文件存在来优雅的关闭（程序在运行中 不强制关闭）
//        本次使用不断循环拉取数据
            while (true) {
                //        拉取binlog日志 拉取一个批次大小
                Message message = canalConnector.getWithoutAck(BATCH_SIZE);
                //        获取batch_id
                long batchId = message.getId();
                //        获取binlog数据的条数
                int size = message.getEntries().size();
                if (batchId == -1  || size == 0) {
//                如果没有拉取到数据
                } else {
//                  拉取的数据是一个Map对象

                    Map binlogMessageToMap = binlogMessageToMap(message);
                    System.out.println(binlogMessageToMap);
//                  将Map对象序列化一个protobuf格式写入到kafka
                    CanalRowData rowData = new CanalRowData(binlogMessageToMap);
                    System.out.println("----------------");
                    System.out.println(rowData);
                    System.out.println("----------------");
                    if (binlogMessageToMap.size()>0){
//              将数据发送到kafka
                        kafkasender.send(rowData);
                    }
                }
            }
        } catch (RuntimeException |InvalidProtocolBufferException e) {
            e.printStackTrace();
        } finally {
            canalConnector.disconnect();
        }
    }

    /*
     * 将binlog日志转化为Map结构
     * 原因：key是列名 value 是列的值的集合 减少了列名出现的次数
     * */
    private Map binlogMessageToMap(Message message) throws InvalidProtocolBufferException {
        Map rowDataMap = new HashMap();
//        遍历 message中的所有binlog实体
        for (CanalEntry.Entry entry : message.getEntries()) {
//            只处理事务型binlog
            if (
                    entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN
                    ||
                    entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND
            ){
                continue;
            }
            /*
            * 获取binlog文件名称
            * 获取logfile偏移量
            * sql执行时间搓
            * 获取数据库名
            * 获取表名
            * 获取事件类型 insert update delete
            * */
                String logfileName = entry.getHeader().getLogfileName();
                long logfileOffset = entry.getHeader().getLogfileOffset();
                long executeTime = entry.getHeader().getExecuteTime();
                String schemaName = entry.getHeader().getSchemaName();
                String tableName = entry.getHeader().getTableName();
                String eventType = entry.getHeader().getEventType().toString().toLowerCase();

                rowDataMap.put("logfileName", logfileName);
                rowDataMap.put("logfileOffset", logfileOffset);
                rowDataMap.put("executeTime", executeTime);
                rowDataMap.put("schemaName", schemaName);
                rowDataMap.put("tableName", tableName);
                rowDataMap.put("eventType", eventType);

//            获取所有行的变更
                Map<String, String> columnDataMap = new HashMap<>();
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                List<CanalEntry.RowData> columnDataList = rowChange.getRowDatasList();
                for (CanalEntry.RowData rowData : columnDataList) {
                    if (eventType.equals("insert") || eventType.equals("update")) {
                        // 获取更改之后的数据
                        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                            columnDataMap.put(column.getName(), column.getValue().toString());
                        }
                    } else if (eventType.equals("delete")) {
                        // 获取更改之前的数据
                        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
                            columnDataMap.put(column.getName(), column.getValue().toString());
                        }
                    }
                }

                rowDataMap.put("columns", columnDataMap);

        }
        return rowDataMap;
    }
}
