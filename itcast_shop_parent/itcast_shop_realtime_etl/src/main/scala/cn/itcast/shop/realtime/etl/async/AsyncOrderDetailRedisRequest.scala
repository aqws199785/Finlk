package cn.itcast.shop.realtime.etl.async

import cn.itcast.canal.bean.CanalRowData
import cn.itcast.shop.realtime.etl.bean.{DimGoodsCatDBEntity, DimGoodsDBEntity, DimOrgDBEntity, DimShopsDBEntity, OrderGoodWideEntity}
import cn.itcast.shop.realtime.etl.utils.RedisUtil
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.concurrent.Executors
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}
import redis.clients.jedis.Jedis

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/*
* 异步查询订单明细数据与维度数据进行关联
* 根据订单明细的事实表与维度表进行关联 需要redis 从而打开关闭数据源 使用richAsyncFunction
* 使用异步IO是为了提高吞吐量
* */

// 注意 redis数据库表名和部分字段获取 当心字符串错误(与实际不符，获取的字符串不存在)
class AsyncOrderDetailRedisRequest extends RichAsyncFunction[CanalRowData, OrderGoodWideEntity] {
  //  定义redis对象
  var jedis: Jedis = _


  override def open(parameters: Configuration): Unit = {
    // 获取redis的连接
    jedis = RedisUtil.getJedis()
    //    指定维度数据所在的数据库的索引
    jedis.select(1)
  }
/*
* 定义异步回调的上下文对象（注意导包 注意隐式转换）
* */
  implicit lazy val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.directExecutor())

  override def asyncInvoke(rowData: CanalRowData, resultFuture: ResultFuture[OrderGoodWideEntity]): Unit = {
    // 发起异步请求，获取结束的Future（注意导包）
    Future {
      // 1： 根据商品id获取商品的详情信息
      val goodsJson: String = jedis.hget("itcast_shop:dim_goods", rowData.getColumns.get("goodsId"))
      // 将商品的json字符串解析为商品的样例类
      val dimGoods: DimGoodsDBEntity = DimGoodsDBEntity(goodsJson)

      // 2:根据商品表的店铺id获取店铺的详情信息（itcast_shop:dim_shops）
      val shopJson: String = jedis.hget("itcast_shop:dim_shops", dimGoods.shopId.toString)
      // 将店铺的段串转化为样例类
      val dimShop: DimShopsDBEntity = DimShopsDBEntity(shopJson)

      // 3:根据商品的id获取商品的分类信息
      // 3.1:根据商品的三级分类id获取三级分类名称
      val thirdCatJson: String = jedis.hget("itcast_shop:dim_goods_cats", dimGoods.getGoodsCatId.toString)
      val dimThirdCat: DimGoodsCatDBEntity = DimGoodsCatDBEntity(thirdCatJson)
      // 3.2:获取商品的二级分类信息
      val secondCatJson: String = jedis.hget("itcast_shop:dim_goods_cats", dimThirdCat.parentId)
      val dimSecondCat: DimGoodsCatDBEntity = DimGoodsCatDBEntity(secondCatJson)
      // 3.3:获取商品的三级分类
      val firstCatJson: String = jedis.hget("itcast_shop:dim_goods_cats", dimSecondCat.parentId)
      val dimFirstCat: DimGoodsCatDBEntity = DimGoodsCatDBEntity(firstCatJson)

      // 4 根据店铺的区域id找到组织机构数据
      // 4.1 根据区域id获取城市数据
      val cityJson: String = jedis.hget("itcast_shop:dim_org", dimShop.areaId.toString)
      val dimOrgCity: DimOrgDBEntity = DimOrgDBEntity(cityJson)
      // 4.2 根据区域的父id获取大区数据
      val regionJson: String = jedis.hget("itcast_shop:dim_org", dimOrgCity.parentId.toString)
      val dimOrgRegion: DimOrgDBEntity = DimOrgDBEntity(regionJson)

      // 构建订单明细宽表数据返回
      val orderGoodWideEntity: OrderGoodWideEntity = OrderGoodWideEntity(
        rowData.getColumns.get("ogId").toLong,
        rowData.getColumns.get("orderId").toLong,
        rowData.getColumns.get("goodsId").toLong,
        rowData.getColumns.get("goodsNum").toLong,
        rowData.getColumns.get("goodsPrice").toDouble,
        dimGoods.goodsName,
        dimShop.shopId.toLong,
        dimThirdCat.catId.toInt,
        dimThirdCat.CatName,
        dimSecondCat.catId.toInt,
        dimSecondCat.CatName,
        dimFirstCat.catId.toInt,
        dimFirstCat.CatName,
        dimShop.areaId.toInt,
        dimShop.shopName,
        dimShop.shopCompany,
        dimOrgCity.orgId.toInt,
        dimOrgCity.orgName,
        dimOrgRegion.orgId.toInt,
        dimOrgRegion.orgName
      )
      println(orderGoodWideEntity)
      // 异步请求回调(注意导包)
      resultFuture.complete(Array(orderGoodWideEntity))
    }
  }

  /*
  * 释放资源 关闭连接
  * */
  override def close(): Unit = {
    if (jedis.isConnected) {
      jedis.close()
    }
  }

  /*
  * 连接redis超时的操作 默认会抛出异常 一旦重写了该方法 则执行该方法的逻辑
  * */
  override def timeout(input: CanalRowData, resultFuture: ResultFuture[OrderGoodWideEntity]): Unit = {
    println("订单明细实时拉宽操作的时候,与维度数据进行关联操作时超时了")
  }
}
