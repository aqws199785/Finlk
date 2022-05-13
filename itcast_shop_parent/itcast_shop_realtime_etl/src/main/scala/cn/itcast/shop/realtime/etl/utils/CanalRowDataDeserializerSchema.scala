package cn.itcast.shop.realtime.etl.utils


import cn.itcast.canal.bean.CanalRowData
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema

/*
 *  自定义反序列化方式 继承 AbstractDeserializationSchema 抽象类
 *  参考SimpleStringSchema
 */
//注意泛型
class CanalRowDataDeserializerSchema extends AbstractDeserializationSchema[CanalRowData] {
  override def deserialize(message: Array[Byte]): CanalRowData = {
    new CanalRowData(message)
  }
}