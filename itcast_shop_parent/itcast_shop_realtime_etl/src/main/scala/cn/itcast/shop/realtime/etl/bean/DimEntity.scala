package cn.itcast.shop.realtime.etl.bean

import com.alibaba.fastjson.{JSON, JSONObject}

import scala.beans.BeanProperty


/*
* 定义维度表的样例类
*/
//TODO 商品维度样例类
case class DimGoodsDBEntity(
                             @BeanProperty goodsId:Long = 0, //商品id
                             @BeanProperty goodsName:String ="", // 商品名称
                             @BeanProperty shopId:Long=0, // 店铺di
                             @BeanProperty goodsCatId:Long=0, // 商品分类id
                             @BeanProperty shopPrice:Double=0 // 商品价格
                           )
// 商品的伴生对象
object  DimGoodsDBEntity{
  def apply(json:String): DimGoodsDBEntity = {
    if (json != null){
      val jsonObject: JSONObject = JSON.parseObject(json)
      new DimGoodsDBEntity(
        jsonObject.getLong("goodsId"),
        jsonObject.getString("goodsName"),
        jsonObject.getLong("shopId"),
        jsonObject.getLong("goodsCatId"),
        jsonObject.getDouble("shopPrice")
      )
    }
    else {
      new DimGoodsDBEntity()
    }
  }
}

//TODO 商品的分类维度

// 商品分类id 商品分类父id 商品分类名称 商品分类级别
case class DimGoodsCatDBEntity(
                              @BeanProperty catId:String="",
                              @BeanProperty parentId:String="",
                              @BeanProperty CatName:String="",
                              @BeanProperty cat_level:String=""
                              )
object  DimGoodsCatDBEntity{
  def apply(json: String): DimGoodsCatDBEntity ={
    if (json != null){
      val jsonObject: JSONObject = JSON.parseObject(json)
      new DimGoodsCatDBEntity(
        jsonObject.getString("catId"),
        jsonObject.getString("parentId"),
        jsonObject.getString("CatName"),
        jsonObject.getString("cat_level")
      )
    }else{
      new DimGoodsCatDBEntity()
    }
  }
}
// TODO 店铺维度样例类

// 店铺id 店铺所属区域id 店铺名称 公司名称
case class DimShopsDBEntity(
              @BeanProperty shopId:String="",
              @BeanProperty areaId:String="",
              @BeanProperty shopName:String="",
              @BeanProperty shopCompany:String=""
              )
object DimShopsDBEntity{
  def apply(json:String): DimShopsDBEntity ={
    if (json != null){
      val jsonObject: JSONObject = JSON.parseObject(json)
      new DimShopsDBEntity(
        jsonObject.getString("shopId"),
        jsonObject.getString("areaId"),
        jsonObject.getString("shopName"),
        jsonObject.getString("shopCompany")
      )
    }else {
      new DimShopsDBEntity()
    }
  }
}

//TODO 组织机构样例类

// 机构id 机构父id 组织机构名称 组织机构级别
case class DimOrgDBEntity(
                         @BeanProperty orgId: String="",
                         @BeanProperty parentId: String="",
                         @BeanProperty orgName: String="",
                         @BeanProperty orgLevel: String=""
                         )
object DimOrgDBEntity{
  def apply(json:String): DimOrgDBEntity ={
    if (json != null){
      val jsonObject: JSONObject = JSON.parseObject(json)
      new DimOrgDBEntity(
        jsonObject.getString("orgId"),
        jsonObject.getString("parentId"),
        jsonObject.getString("orgName"),
        jsonObject.getString("orgLevel")
      )
    }else{
      new DimOrgDBEntity()
    }
  }
}

//TODO 门店商品分类维度样例类

// 商品分类id 商品分类父id 商品分类名称 商品分类级别
case class DimShopCatDBEntity(
                             @BeanProperty catId:String="",
                             @BeanProperty parentId:String="",
                             @BeanProperty catName:String="",
                             @BeanProperty catSort:String=""
                             )
object DimShopCatDBEntity{
  def apply(json:String): DimShopCatDBEntity ={
    if (json != null){
      val jsonObject: JSONObject = JSON.parseObject(json)
      new DimShopCatDBEntity(
        jsonObject.getString("catId"),
        jsonObject.getString("parentId"),
        jsonObject.getString("catName"),
        jsonObject.getString("catSort")
      )
    }else{
      new DimShopCatDBEntity()
    }
  }
}