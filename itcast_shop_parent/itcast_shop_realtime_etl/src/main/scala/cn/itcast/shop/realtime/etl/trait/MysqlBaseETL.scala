package cn.itcast.shop.realtime.etl.`trait`
import cn.itcast.canal.bean.CanalRowData
import cn.itcast.shop.realtime.etl.utils.{CanalRowDataDeserializerSchema, GlobalConfigUtil, KafkaProps}

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import org.apache.flink.api.scala._
/*
* 根据数据来源不同，可以抽象出来两个抽象类
* 该类主要是消费通过canal序列化到kafka的数据 需要自定义发序列化方式
* */
abstract class MysqlBaseETL(env:StreamExecutionEnvironment) extends BaseETL[CanalRowData]{
  override def getKafkaDataStream(topic: String= GlobalConfigUtil.`input.topic.canal`): DataStream[CanalRowData] = {
    //  消费的是kafka的canal数据 而binlog日志进行类protobuf的序列化 ，所以读取到的数据需要进行反序列化
//    注意 CanalRowDataDeserializerSchema的泛型必须为 CanalRowData
    val canalKafkaConsumer: FlinkKafkaConsumer011[CanalRowData] = new FlinkKafkaConsumer011[CanalRowData](
      topic,
      new CanalRowDataDeserializerSchema,
      KafkaProps.getKafkaProperties()
    )
//    注意导包 否则 addSource() 返回值类型错误
//    import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
//    import org.apache.flink.api.scala._
    val canalDataStream: DataStream[CanalRowData] = env.addSource(canalKafkaConsumer)
    canalDataStream
  }
}
