package cn.itcast.shop.realtime.etl.process

import cn.itcast.canal.bean.CanalRowData
import cn.itcast.shop.realtime.etl.`trait`.MysqlBaseETL
import cn.itcast.shop.realtime.etl.bean.{DimGoodsCatDBEntity, DimOrgDBEntity, DimShopCatDBEntity, DimShopsDBEntity, GoodsWideEntity}
import cn.itcast.shop.realtime.etl.utils.{GlobalConfigUtil, RedisUtil}
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import redis.clients.jedis.Jedis

case class GoodsDataETL(env: StreamExecutionEnvironment) extends MysqlBaseETL(env) {
  override def process(): Unit = {
    // 只过出来 itcast_goods表的日志数据 并进行转换
    val goodsCanalDataStream: DataStream[CanalRowData] = getKafkaDataStream().filter(rowdata => rowdata.getTableName == "itcast_goods")
    // 使用同步IO方式请求Redis拉取维度数据
    val goodsEntityDataStream: DataStream[GoodsWideEntity] = goodsCanalDataStream.map(new RichMapFunction[CanalRowData, GoodsWideEntity] {
      var jedis: Jedis = _

      override def open(parameters: Configuration) = {
        jedis = RedisUtil.getJedis()
        jedis.select(1)
      }

      override def map(rowData: CanalRowData): GoodsWideEntity = {
        val shopJson: String = jedis.hget("itcast_shop:dim_shops", rowData.getColumns.get("shopId"))
        val dimShop: DimShopsDBEntity = DimShopsDBEntity(shopJson)

        val thirdCatJSON: String = jedis.hget("itcast_shop:dim_goods_cats", rowData.getColumns.get("goodsCatId"))
        val dimThirdCat: DimGoodsCatDBEntity = DimGoodsCatDBEntity(thirdCatJSON)

        val secondCatJson: String = jedis.hget("itcast_shop:dim_goods_cats", rowData.getColumns.get(dimThirdCat.parentId))
        val dimSecondCat: DimGoodsCatDBEntity = DimGoodsCatDBEntity(secondCatJson)

        val firstCatJson: String = jedis.hget("itcast_shop:dim_goods_cats", rowData.getColumns.get(dimSecondCat.parentId))
        val dimFirstCat: DimGoodsCatDBEntity = DimGoodsCatDBEntity(firstCatJson)

        val secondShopCatJson: String = jedis.hget("itcast_shop:dim_goods_cats", rowData.getColumns.get("shopCatId1"))
        val dimSecondShopCat: DimShopCatDBEntity = DimShopCatDBEntity(secondShopCatJson)

        val firstShopCatJson: String = jedis.hget("itcast_shop:dim_goods_cats", rowData.getColumns.get("shopCatId2"))
        val dimFirstShopCat: DimShopCatDBEntity = DimShopCatDBEntity(firstShopCatJson)

        val cityJSON: String = jedis.hget("itcast_shop:dim_org", dimShop.areaId)
        val dimOrgCity: DimOrgDBEntity = DimOrgDBEntity(cityJSON)

        val regionJSON: String = jedis.hget("itcast_shop:dim_org", dimOrgCity.parentId)
        val dimOrgRegion: DimOrgDBEntity = DimOrgDBEntity(regionJSON)

        GoodsWideEntity(
          rowData.getColumns.get("goodsId").toLong,
          rowData.getColumns.get("goodsSn"),
          rowData.getColumns.get("productNo"),
          rowData.getColumns.get("goodsName"),
          rowData.getColumns.get("goodsImg"),
          rowData.getColumns.get("shopId"),
          dimShop.shopName,
          rowData.getColumns.get("goodsType"),
          rowData.getColumns.get("marketPrice"),
          rowData.getColumns.get("shopPrice"),
          rowData.getColumns.get("warnStock"),
          rowData.getColumns.get("goodsStock"),
          rowData.getColumns.get("goodsUnit"),
          rowData.getColumns.get("goodsTips"),
          rowData.getColumns.get("isSale"),
          rowData.getColumns.get("isBest"),
          rowData.getColumns.get("isHot"),
          rowData.getColumns.get("isNew"),
          rowData.getColumns.get("isRecom"),
          rowData.getColumns.get("goodsCatIdPath"),
          dimThirdCat.catId.toInt,
          dimThirdCat.CatName,
          dimSecondCat.catId.toInt,
          dimSecondCat.CatName,
          dimFirstCat.catId.toInt,
          dimFirstCat.CatName,
          dimFirstShopCat.getCatId,
          dimFirstShopCat.catName,
          dimSecondShopCat.getCatId,
          dimSecondShopCat.catName,
          rowData.getColumns.get("brandId"),
          rowData.getColumns.get("goodsDesc"),
          rowData.getColumns.get("goodsStatus"),
          rowData.getColumns.get("saleNum"),
          rowData.getColumns.get("saleTime"),
          rowData.getColumns.get("visitNum"),
          rowData.getColumns.get("appraiseNum"),
          rowData.getColumns.get("isSpec"),
          rowData.getColumns.get("gallery"),
          rowData.getColumns.get("goodsSeoKeywords"),
          rowData.getColumns.get("illegalRemarks"),
          rowData.getColumns.get("dataFlag"),
          rowData.getColumns.get("createTime"),
          rowData.getColumns.get("isFreeShipping"),
          rowData.getColumns.get("goodsSerachKeywords"),
          rowData.getColumns.get("modifyTime"),
          dimOrgCity.orgId.toInt,
          dimOrgCity.orgName,
          dimOrgRegion.orgId.toInt,
          dimOrgRegion.orgName
        )
      }

      override def close() = {
        if (jedis.isConnected) {
          jedis.close()
        }
      }
    })
    goodsEntityDataStream.printToErr("商品表数据>>>>")

    // (3)将数据流转换为json字符串
    val goodSWideJsonDataStream: DataStream[String] = goodsEntityDataStream.map(
      goodsWideEntity =>
        JSON.toJSONString(goodsWideEntity, SerializerFeature.DisableCircularReferenceDetect)
    )
    // (4) 将关联后的维度数据写入到kafka
    goodSWideJsonDataStream.addSink(kafkaProducer(GlobalConfigUtil.`output.topic.goods`))

  }

}
