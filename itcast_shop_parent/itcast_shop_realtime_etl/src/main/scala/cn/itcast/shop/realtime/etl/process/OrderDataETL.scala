package cn.itcast.shop.realtime.etl.process

import cn.itcast.canal.bean.CanalRowData
import cn.itcast.shop.realtime.etl.`trait`.MysqlBaseETL
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import cn.itcast.shop.realtime.etl.bean.OrderEntity
import cn.itcast.shop.realtime.etl.utils.GlobalConfigUtil
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.streaming.api.scala._

/*
* 订单数据实时 ETL
*/
case class OrderDataETL(env: StreamExecutionEnvironment) extends MysqlBaseETL(env) {
  /*
   * 根据业务抽取出来 process方法 因为所有etl都有操作方法
   */
  override def process(): Unit = {
    // TODO   （1）从 kafka 中消费出来的订单数据 过滤出来订单表的数据
    val orderDataStream: DataStream[CanalRowData] = getKafkaDataStream().filter(data => data.getTableName == "itcast_orders")
    // TODO    （2）将 rowData转换成OrderEntity对象

    //      注意：需要导入隐式转换 和样例类创建无误(不然会识别不到orderEntity的apply方法)
    val orderEntityDataStream: DataStream[OrderEntity] = orderDataStream.map(rowData => OrderEntity(rowData))
    // TODO    （3）将OrderEntity对象转换成Json字符串
    val orderEntityJsonDataStream: DataStream[String] = orderEntityDataStream.map(orderEntity =>
      JSON.toJSONString(orderEntity, SerializerFeature.DisableCircularReferenceDetect)
    )
    orderEntityJsonDataStream.printToErr("订单日志数据")
    // TODO    （4）将转换后的json字符串写入到kafka集群
    orderEntityJsonDataStream.addSink(kafkaProducer(GlobalConfigUtil.`output.topic.order`))
  }
}
