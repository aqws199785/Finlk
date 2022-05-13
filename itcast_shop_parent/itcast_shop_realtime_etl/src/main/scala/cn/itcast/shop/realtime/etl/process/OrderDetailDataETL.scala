package cn.itcast.shop.realtime.etl.process

import java.util.concurrent.TimeUnit

import cn.itcast.canal.bean.CanalRowData
import cn.itcast.shop.realtime.etl.`trait`.MysqlBaseETL
import cn.itcast.shop.realtime.etl.async.AsyncOrderDetailRedisRequest
import cn.itcast.shop.realtime.etl.bean.OrderGoodWideEntity
import cn.itcast.shop.realtime.etl.utils.{GlobalConfigUtil, HbaseUtil}
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.{AsyncDataStream, DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.{Admin, Connection, Put, Table}
import org.apache.hadoop.hbase.util.Bytes


/*
* 订单明细的实时ETL
*  将订单明细数据的事实表和维度表的数据关联后写入到 Hbase 中
* 将拉宽后的订单数据保存到kafka中
*
* 为什么要保留两份数据
* 保留到hbase中的数据是明细数据 可以持久化存储 将来需要分析明细数据的时候 可以查询
* 保留到kafka中的数据是有时效性的 会根据kafka的保留策略过期删除数据 摄取到Durid以后 就不在是明细数据
*/
case class OrderDetailDataETL(env: StreamExecutionEnvironment) extends MysqlBaseETL(env) {
  override def process(): Unit = {
    //  (1)获取canal的订单数据 过滤订单明细表的数据 将canalRowData转换成OrderGoods样例类
    val orderGoodsCanalDataStream: DataStream[CanalRowData] = getKafkaDataStream().filter(data => data.getTableName == "itcast_order_goods")
    //  (2)将订单明细表的数据进行实时的拉宽操作 (异步IO:设置并发度 提高吞吐量)(注意导包和隐式转换)
    val orderGoodWideEntityDataStream: DataStream[OrderGoodWideEntity] = AsyncDataStream.unorderedWait(
      orderGoodsCanalDataStream, //数据流
      new AsyncOrderDetailRedisRequest, //异步请求的对象（需要自己定义）
      1, //超时时间
      TimeUnit.SECONDS, // 超时时间单位：秒
      100 // 并发数
    )
    orderGoodsCanalDataStream.printToErr("拉宽后的订单明细数据")
    // (3) 将数据流对象转换为json字符串
    val orderEntityJsonDataStream: DataStream[String] = orderGoodWideEntityDataStream.map(
      orderEntity =>
        JSON.toJSONString(orderEntity, SerializerFeature.DisableCircularReferenceDetect)
    )
    orderEntityJsonDataStream.printToErr("订单数据>>>")
    // (4) 将json字符串数据写入到kafka中
    orderEntityJsonDataStream.addSink(kafkaProducer(GlobalConfigUtil.`output.topic.order_detail`))
    // (5) 将拉宽后的订单明细数据写入到hbase中
    orderGoodWideEntityDataStream.addSink(new RichSinkFunction[OrderGoodWideEntity] {
      // 5.1 定义连接对象(注意导包为hbase)
      var connection:Connection = _
      // 5.2 定义hbase的表
      var table:Table = _
      override def open(parameters: Configuration) = {
        // 初始化hbase的连接对象(注意 getpool()返回值类型)
        connection = HbaseUtil.getpool().getConnection

        // 初始化要写入的表名(注意 TableName导包)
        table = connection.getTable(TableName.valueOf(GlobalConfigUtil.`hbase.table.orderdetail`))
//        自动创建列
//        val admin: Admin = connection.getAdmin
//        val hTableDescriptor = new HTableDescriptor(table.getName)
//        val hColumnDescriptor = new HColumnDescriptor("detail")
//        hTableDescriptor.addFamily(hColumnDescriptor)
//        if (admin.tableExists(hTableDescriptor.getTableName)){
//              admin.disableTable(hTableDescriptor.getTableName)
//              admin.deleteTable(hTableDescriptor.getTableName)
//        }
//        else{
//          println("不存在该数据表")
//          admin.createTable(hTableDescriptor)
//        }
      }

      override def close() = {
        if (table != null){table.close()}

        if (!connection.isClosed){
          // 将连接放回连接池
          HbaseUtil.getpool().returnConnection(connection)
        }
      }

      override def invoke(orderGoodWideEntity: OrderGoodWideEntity, context: SinkFunction.Context[_]) = {
        // 使用订单明细id作为rowKey （注意导包：import org.apache.hadoop.hbase.util.Bytes）
        var rowKey = Bytes.toBytes(orderGoodWideEntity.getOgId.toString)
        var family: Array[Byte] = Bytes.toBytes(GlobalConfigUtil.`hbase.table.family`)

        val put = new Put(rowKey)

        val ogIdCol = Bytes.toBytes("ogId")
        val orderIdCol = Bytes.toBytes("orderId")
        val goodsIdCol = Bytes.toBytes("goodsId")
        val goodsNumCol = Bytes.toBytes("goodsNum")
        val goodsPriceCol = Bytes.toBytes("goodsPrice")
        val goodsNameCol = Bytes.toBytes("goodsName")
        val shopIdCol = Bytes.toBytes("shopId")
        val goodsThirdCatIdCol = Bytes.toBytes("goodsThirdCatId")
        val goodsThirdCatNameCol = Bytes.toBytes("goodsThirdCatName")
        val goodsSecondCatIdCol = Bytes.toBytes("goodsSecondCatId")
        val goodsSecondCatNameCol = Bytes.toBytes("goodsSecondCatName")
        val goodsFirstCatIdCol = Bytes.toBytes("goodsFirstCatId")
        val goodsFirstCatNameCol = Bytes.toBytes("goodsFirstCatName")
        val areaIdCol = Bytes.toBytes("areaId")
        val shopNameCol = Bytes.toBytes("shopName")
        val shopCompanyCol = Bytes.toBytes("shopCompany")
        val cityIdCol = Bytes.toBytes("cityId")
        val cityNameCol = Bytes.toBytes("cityName")
        val regionIdCol = Bytes.toBytes("regionId")
        val regionNameCol = Bytes.toBytes("regionName")

        put.addColumn(family, ogIdCol, Bytes.toBytes(orderGoodWideEntity.ogId.toString))
        put.addColumn(family, orderIdCol, Bytes.toBytes(orderGoodWideEntity.orderId.toString))
        put.addColumn(family, goodsIdCol, Bytes.toBytes(orderGoodWideEntity.goodsId.toString))
        put.addColumn(family, goodsNumCol, Bytes.toBytes(orderGoodWideEntity.goodsNum.toString))
        put.addColumn(family, goodsPriceCol, Bytes.toBytes(orderGoodWideEntity.goodsPrice.toString))
        put.addColumn(family, goodsNameCol, Bytes.toBytes(orderGoodWideEntity.goodsName))
        put.addColumn(family, shopIdCol, Bytes.toBytes(orderGoodWideEntity.shopId.toString))
        put.addColumn(family, goodsThirdCatIdCol, Bytes.toBytes(orderGoodWideEntity.goodsThirdCatId.toString))
        put.addColumn(family, goodsThirdCatNameCol, Bytes.toBytes(orderGoodWideEntity.goodsThirdCatName.toString))
        put.addColumn(family, goodsSecondCatIdCol, Bytes.toBytes(orderGoodWideEntity.goodsSecondCatId.toString))
        put.addColumn(family, goodsSecondCatNameCol, Bytes.toBytes(orderGoodWideEntity.goodsSecondCatName.toString))
        put.addColumn(family, goodsFirstCatIdCol, Bytes.toBytes(orderGoodWideEntity.goodsFirstCatId.toString))
        put.addColumn(family, goodsFirstCatNameCol, Bytes.toBytes(orderGoodWideEntity.goodsFirstCatName.toString))
        put.addColumn(family, areaIdCol, Bytes.toBytes(orderGoodWideEntity.areaId.toString))
        put.addColumn(family, shopNameCol, Bytes.toBytes(orderGoodWideEntity.shopName.toString))
        put.addColumn(family, shopCompanyCol, Bytes.toBytes(orderGoodWideEntity.shopCompany.toString))
        put.addColumn(family, cityIdCol, Bytes.toBytes(orderGoodWideEntity.cityId.toString))
        put.addColumn(family, cityNameCol, Bytes.toBytes(orderGoodWideEntity.cityName.toString))
        put.addColumn(family, regionIdCol, Bytes.toBytes(orderGoodWideEntity.regionId.toString))
        put.addColumn(family, regionNameCol, Bytes.toBytes(orderGoodWideEntity.regionName.toString))

        //3：执行put操作
        table.put(put)
      }
    })
  }
}
