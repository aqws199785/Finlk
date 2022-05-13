package cn.itcast.shop.realtime.etl.`trait`
import cn.itcast.shop.realtime.etl.utils.KafkaProps
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
/*
* 根据数据来源不同，可以抽象出来两个抽象类
* 该类主要是消费日志数据和购物车数据 这类数据存入kafka就是字符串类型 直接使用kafka反序列化
*/
// 抽象类可以不重写 process()
abstract class MQBaseETL(env: StreamExecutionEnvironment) extends BaseETL [String]{

  override def getKafkaDataStream(topic: String): DataStream[String] = {
//    注意 KafkaProps的返回类型为 Properties
    val kafkaProducer= new FlinkKafkaConsumer011[String](
      topic,
      new SimpleStringSchema(),
      KafkaProps.getKafkaProperties()
    )
//    将消费者对象添加到数据源
    val logDataStream: DataStream[String] = env.addSource(kafkaProducer)
//    返回消费到的数据
    logDataStream
  }
}
