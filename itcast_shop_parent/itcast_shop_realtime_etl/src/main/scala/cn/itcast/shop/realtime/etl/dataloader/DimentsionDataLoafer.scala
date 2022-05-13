package cn.itcast.shop.realtime.etl.dataloader

import java.sql.{Connection, DriverManager, ResultSet, Statement}

import cn.itcast.shop.realtime.etl.bean.{DimGoodsCatDBEntity, DimGoodsDBEntity, DimOrgDBEntity, DimShopCatDBEntity, DimShopsDBEntity}
import cn.itcast.shop.realtime.etl.utils.{GlobalConfigUtil, RedisUtil}
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import redis.clients.jedis.Jedis

/*
* 维度数据的全量装载同步到redis
* 将五个维度表的数据同步到redis
* （1）商品维度表
* （2）商品分类维度表
* （3）店铺表
* （4）组织机构表
* （5）门店商品分类表
*/
object DimentsionDataLoafer {
  def main(args: Array[String]): Unit = {
    // (1)   注册mysql驱动
    Class.forName("com.mysql.jdbc.Driver")
    // (2) 创建连接
    val connection: Connection = DriverManager.getConnection(
      s"jdbc:mysql://${GlobalConfigUtil.`mysql.server.ip`}:${GlobalConfigUtil.`mysql.server.port`}/${GlobalConfigUtil.`mysql.server.database`}",
      GlobalConfigUtil.`mysql.server.username`,
      GlobalConfigUtil.`mysql.server.password`
      //      "aqws199785"
    )
    // (3)   创建redis连接
    val jedis: Jedis = RedisUtil.getJedis()
    // (4) reids中默认有16个数据库 ，需要指定一下维度数据保存到那个数据库中 默认是第一个
    jedis.select(1)
    //    加载数据到redis中
    LoadDimGoods(connection, jedis)
    LoadDimShops(connection,jedis)
    LoadDimGoodsCats(connection,jedis)
    LoadDimOrg(connection,jedis)
    LoadDimShopsCats(connection,jedis)
/// 退出程序
    System.exit(0)
  }

  // TODO 加载商品维度数据到redis
  def LoadDimGoods(connection: Connection, jedis: Jedis): Unit = {
    val sql =
      """
        | select
        |   goodsId,
        |   goodsName,
        |   shopID,
        |   goodsCatId,
        |   shopPrice
        | from
        |   itcast_goods
        |""".stripMargin
    //    创建statement
    val statement: Statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(sql)
    //    遍历商品表的数据
    while (resultSet.next()) {
      //      将获取的商品维度表数据写入到redis中

      //      需要将获取的数据写入到redis中
      //      redis是一个k/v数据库 需要将五个字段封装成json结构保存到redis中
      val entity: DimGoodsDBEntity = DimGoodsDBEntity(
        resultSet.getLong("goodsId"),
        resultSet.getString("goodsName"),
        resultSet.getLong("shopId"),
        resultSet.getLong("goodsCatId"),
        resultSet.getDouble("shopPrice")
      )
      //      将样例类转换成json字符串写入到redis中
      //      导包 import com.alibaba.fastjson.JSON
      val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
      println(json)
      jedis.hset(
        "itcast_shop:dim_goods",
        resultSet.getLong("goodsId").toString,
        json)
    }
    resultSet.close()
    statement.close()
  }

  // TODO 加载店铺维度数据到redis
  def LoadDimShops(connection: Connection, jedis: Jedis): Unit = {
    val sql =
      """
        | select
        |   shopId,
        |   areaId,
        |   shopName,
        |   shopCompany
        | from
        |   itcast_shops
        |""".stripMargin
    val statement: Statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(sql)
    while (resultSet.next()) {
      val shopId: String = resultSet.getString("shopId")
      val areaId: String = resultSet.getString("areaId")
      val shopName: String = resultSet.getString("shopName")
      val shopCompany: String = resultSet.getString("shopCompany")
      val dimShop: DimShopsDBEntity = DimShopsDBEntity(shopId, areaId, shopName, shopCompany)
      print(dimShop)
      val json: String = JSON.toJSONString(dimShop, SerializerFeature.DisableCircularReferenceDetect)
      jedis.hset("itcast_shop:dim_shops", shopId, json)
    }
    resultSet.close()
    statement.close()
  }

  // TODO 加载商品分类维度数据到redis
  def LoadDimGoodsCats(connection: Connection, jedis: Jedis): Unit = {
    val sql =
      """
        |select
        | catId,
        | parentId,
        | catName,
        | cat_level
        |from
        | itcast_goods_cats
        |""".stripMargin
    val statement: Statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(sql)
    while (resultSet.next()) {
      val catId: String = resultSet.getString("catId")
      val parentId: String = resultSet.getString("parentId")
      val catName: String = resultSet.getString("catName")
      val cat_level: String = resultSet.getString("cat_level")
      val entity: DimGoodsCatDBEntity = DimGoodsCatDBEntity(catId, parentId, catName, cat_level)
      val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
      print(json)
      jedis.hset("itcast_shop:dim_goods_cats", catId, json)
    }
    resultSet.close()
    statement.close()
  }

  //  TODO 加载组织机构数据到redis
  def LoadDimOrg(connection: Connection, jedis: Jedis): Unit = {
    var sql =
      """
        | select
        |   orgid,
        |   parentid,
        |   orgName,
        |   orgLevel
        |  from
        |   itcast_org
        |""".stripMargin
    val statement: Statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(sql)
    while(resultSet.next()){
      val orgId: String = resultSet.getString("orgId")
      val parentId: String = resultSet.getString("parentId")
      val orgName: String = resultSet.getString("orgName")
      val orgLevel: String = resultSet.getString("orgLevel")
      val entity: DimOrgDBEntity = DimOrgDBEntity(orgId, parentId, orgName, orgLevel)
      val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
      print(json)
      jedis.hset("itcast_shop:dim_org",orgId,json)
    }
    resultSet.close()
    statement.close()
  }

  // TODO 加载门店商品分类维度数据到redis
  def LoadDimShopsCats(connection: Connection, jedis: Jedis): Unit = {
    val sql=
    """
      |select
      | catId,
      | parentId,
      | catName,
      | catSort
      |from
      | itcast_shop_cats
      |""".stripMargin
    val statement: Statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(sql)
    while (resultSet.next()){
      val catId: String = resultSet.getString("catId")
      val parentId: String = resultSet.getString("parentId")
      val catName: String = resultSet.getString("catName")
      val catSort: String = resultSet.getString("catSort")
      val entity: DimShopCatDBEntity = DimShopCatDBEntity(catId, parentId, catName, catSort)
      val json: String = JSON.toJSONString(entity, SerializerFeature.DisableCircularReferenceDetect)
      jedis.hset("itcast_shop:dim_shop_cats",catId,json)
    }
    resultSet.close()
    statement.close()
  }
}










