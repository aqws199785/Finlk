package cn.itcast.canal.bean;

import cn.itcast.canal.protobuf.ProtoBufable;
import cn.itcast.canal.protobuf.CanalModel;
import com.alibaba.fastjson.JSON;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.HashMap;
import java.util.Map;

/*
* 这个是canal数据的protobuf的实现类
* 能够使用protobuf序列化成bean对象
* 将binlog解析后的map对象 ，转化为protobuf的序列化后的字节码数据，最终写入到kafka集群
*/
public class CanalRowData implements ProtoBufable {

  private String logfileName;
  private Long logfileOffset;
  private Long executeTime;
  private String schemaName;
  private String tableName;
  private String eventType;
  private Map<String,String> columns;

    public String getLogfileName() {
        return logfileName;
    }

    public void setLogfileName(String logfileName) {
        this.logfileName = logfileName;
    }

    public Long getLogFileOffset() {
        return logfileOffset;
    }

    public void setLogFileOffset(Long logfileOffset) {
        this.logfileOffset = logfileOffset;
    }

    public Long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, String> columns) {
        this.columns = columns;
    }

    /*
* 自定义构造方法 解析map对象的binlog日志
*
*/
    public CanalRowData(Map map){
//    解析map对象的所有参数
        if(map.size() > 0 ){
            this.logfileName = map.get("logfileName").toString();
            this.logfileOffset = Long.parseLong(map.get("logfileOffset").toString());
            this.executeTime = Long.parseLong(map.get("executeTime").toString());
            this.schemaName = map.get("schemaName").toString();
            this.tableName =  map.get("tableName").toString();
            this.eventType = map.get("eventType").toString();
            this.columns = (Map<String,String>)map.get("columns");
        }
    }
//    p34 12:54
    public CanalRowData(byte[] bytes){
        try {
            CanalModel.RowData rowData = CanalModel.RowData.parseFrom(bytes);
            this.logfileName=rowData.getLogfileName();
            this.logfileOffset=rowData.getLogfileOffset();
            this.executeTime=rowData.getExecuteTime();
            this.schemaName=rowData.getSchemaName();
            this.tableName=rowData.getTableName();
            this.eventType=rowData.getEventType();
//          将所有的列添加到 Map 中
            this.columns=new HashMap<>();
            this.columns.putAll(rowData.getColumnsMap());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
/*
* 需要将map对象解析出来的参数 赋值给protobuf对象 然后序列化后字节码返回
* */
    @Override
    public byte[] toBytes() {
        CanalModel.RowData.Builder builder = CanalModel.RowData.newBuilder();
        builder.setLogfileName(this.getLogfileName());
        builder.setLogfileOffset(this.getLogFileOffset());
        builder.setExecuteTime(this.getExecuteTime());
        builder.setSchemaName(this.getSchemaName());
        builder.setTableName(this.getTableName());
        builder.setEventType(this.getEventType());
//        获取Map的所有的key
        for (String key:this.getColumns().keySet()){
            builder.putColumns(key,this.getColumns().get(key));
        }
        return builder.build().toByteArray();
    }
    public String toString(){
        return JSON.toJSONString(this);
    }
}
