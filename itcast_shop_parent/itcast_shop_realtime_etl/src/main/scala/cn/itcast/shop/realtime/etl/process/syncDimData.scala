package cn.itcast.shop.realtime.etl.process

import cn.itcast.canal.bean.CanalRowData
import cn.itcast.shop.realtime.etl.`trait`.MysqlBaseETL
import cn.itcast.shop.realtime.etl.bean.{DimGoodsCatDBEntity, DimGoodsDBEntity, DimOrgDBEntity, DimShopCatDBEntity, DimShopsDBEntity}
import cn.itcast.shop.realtime.etl.utils.RedisUtil
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import redis.clients.jedis.Jedis

case class syncDimData(env: StreamExecutionEnvironment) extends MysqlBaseETL(env) {
  //  增量更新维度数据到redis中
  override def process(): Unit = {
    //    （1）获取数据源
    val canalDataStream: DataStream[CanalRowData] = getKafkaDataStream()
    //    （2）过滤出来维度表
    val dimRowDataStream: DataStream[CanalRowData] = canalDataStream.filter(
      rowData =>
        rowData.getTableName match {
          case "itcast_goods" => true
          case "itcast_shops" => true
          case "itcast_goods_cats" => true
          case "itcast_org" => true
          case "itcast_shop_cats" => true
          //        一定要加上else 否则会抛出异常
          case _ => false
        }
    )
    //    （3）处理同步过来的数据更新到redis
    //    ctrl + O 重写父类方法
    dimRowDataStream.addSink(new RichSinkFunction[CanalRowData] {
      //      定义redis对象
      var jedis: Jedis = _

      //打开连接
      override def open(parameters: Configuration) = {
        //        获取一个连接
        jedis = RedisUtil.getJedis()
        //        维度数据放在第二个数据库中
        jedis.select(1)
      }

      //关闭连接
      override def close() = {
        //        如果是已连接中
        if (jedis.isConnected) {
          jedis.close()
        }
      }

      //TODO 处理数据
      override def invoke(rowData: CanalRowData, context: SinkFunction.Context[_]): Unit = {
        print("------------------update-------------------------")
        rowData.getEventType match {
          case "insert" => updateDimData(rowData)
          case "update" => updateDimData(rowData)
          case "delete" => deleteDimData(rowData)
          case _ =>
        }
      }

      //TODO 更新维度数据
      def updateDimData(rowData: CanalRowData): Unit = {
        rowData.getTableName match {
          case "itcast_goods" => {
            val goodsId: Long = rowData.getColumns.get("goodsId").toLong
            val goodsName: String = rowData.getColumns.get("goodsName")
            val shopId: Long = rowData.getColumns.get("shopId").toLong
            val goodsCatId: Int = rowData.getColumns.get("goodsCatId").toInt
            val shopPrice: Double = rowData.getColumns.get("shopPrice").toDouble
            print(goodsId)
            // 将获取到的商品维度表数据写入到redis中
            // redis是一个kv数据库 需要将以上五个字段封装成json结构保存到redis中
            val entity: DimGoodsDBEntity = DimGoodsDBEntity(goodsId, goodsName, shopId, goodsCatId, shopPrice)
            val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
            jedis.hset("itcast_shop:dim_goods", goodsId.toString, json)
          }
          case "itcast_shops" => {
            //            如果是店铺维度表更新
            val shopId: String = rowData.getColumns.get("shopId")
            val areaId: String = rowData.getColumns.get("areaId")
            val shopName: String = rowData.getColumns.get("shopName")
            val shopCompany: String = rowData.getColumns.get("shopCompany")
            val entity: DimShopsDBEntity = DimShopsDBEntity(shopId, areaId, shopName, shopCompany)
            val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
            jedis.hset("itcast_shop:dim_shops", shopId, json)
          }
          case "itcast_goods_cats" => {
            //            商品分类维度表
            val catId: String = rowData.getColumns.get("catId")
            val parentId: String = rowData.getColumns.get("parentId")
            val catName: String = rowData.getColumns.get("catName")
            val cat_level: String = rowData.getColumns.get("cat_level")
            val entity: DimGoodsCatDBEntity = DimGoodsCatDBEntity(catId, parentId, catName, cat_level)
            val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
            jedis.hset("itcast_shop:dim_cats", catId, json)
          }
          case "itcast_org" => {
            //            组织机构维度表更新
            val orgId: String = rowData.getColumns.get("catId")
            val parentId: String = rowData.getColumns.get("parentId")
            val orgName: String = rowData.getColumns.get("orgName")
            val orgLevel: String = rowData.getColumns.get("orgLevel")
            val entity: DimOrgDBEntity = DimOrgDBEntity(orgId, parentId, orgName, orgLevel)
            val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
            jedis.hset("itcast_shop:dim_org", orgId, json)
          }
          case """itcast_shop_cats""" => {
            //            门店商品维度表
            val catId: String = rowData.getColumns.get("catId")
            val parentId: String = rowData.getColumns.get("parentId")
            val catName: String = rowData.getColumns.get("catName")
            val catSort: String = rowData.getColumns.get("catSort")
            val entity: DimShopCatDBEntity = DimShopCatDBEntity(catId, parentId, catName, catSort)
            val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
            jedis.hset("itcast_shop:dim_shop_cats", catId, json)
          }
        }
      }

      //TODO 删除维度数据
      def deleteDimData(rowData: CanalRowData): Unit = {
        rowData.getTableName match {
          case "itcast_goods" => {
            jedis hdel("itcast_shop:dim_goods", rowData.getColumns.get("goodId"))
          }
          case "itcast_shops" => {
            jedis.hdel("itcast_shop:dim_shops",rowData.getColumns.get("shopId"))
          }
          case "itcast_goods_cats"=>{
            jedis.hdel("itcast_shop:dim_goods_cats",rowData.getColumns.get("catId"))
          }
          case ""=>{
            jedis.hdel("itcast_shop:dim_org",rowData.getColumns.get("orgId"))
          }
          case ""=>{
            jedis.hdel("itcast_shop:dim_shop_cats",rowData.getColumns.get("catId"))
          }
        }

      }

    })
  }
}
