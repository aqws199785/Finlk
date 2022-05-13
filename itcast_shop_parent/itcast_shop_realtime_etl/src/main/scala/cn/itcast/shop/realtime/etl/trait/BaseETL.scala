package cn.itcast.shop.realtime.etl.`trait`

import cn.itcast.shop.realtime.etl.utils.KafkaProps
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer011, FlinkKafkaProducer011}
import org.apache.flink.streaming.connectors.kafka.internals.KeyedSerializationSchemaWrapper
import org.apache.flink.streaming.api.scala._

/*
* 定义特质，抽取所有ETl操作的公共方法
*
*/
trait BaseETL[T] {
  def kafkaProducer(topic:String) ={
    //    注意该地方为生成者 请勿填写为消费者对象
    new FlinkKafkaProducer011[String](
      topic,
      //      用于读取kafka数据再写入kafka中
      new KeyedSerializationSchemaWrapper[String](new SimpleStringSchema()),
      KafkaProps.getKafkaProperties()
    )
  }
  //  根据业务可以抽出出来kafka读取方法 应为所有数据都会操作kafka
  def getKafkaDataStream(topic: String): DataStream[T]

  //  根据业务抽取出来process方法，因为所有的etl都有操作方法
  def process()

}
