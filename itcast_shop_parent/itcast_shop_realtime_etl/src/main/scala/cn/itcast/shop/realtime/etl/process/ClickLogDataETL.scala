package cn.itcast.shop.realtime.etl.process

import java.io.File

import cn.itcast.canal.uitil.IPSeeker
import cn.itcast.shop.realtime.etl.`trait`.MQBaseETL
import cn.itcast.shop.realtime.etl.bean.{ClickLogEntity, ClickLogWideEntity}
import cn.itcast.shop.realtime.etl.utils.DateUtil.{customDateFormat, dateLoad}
import cn.itcast.shop.realtime.etl.utils.GlobalConfigUtil
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import nl.basjes.parse.httpdlog.HttpdLoglineParser
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._

class ClickLogDataETL(env: StreamExecutionEnvironment) extends MQBaseETL(env) {


  override def process(): Unit = {
    // TODO   (1) 获取点击流日志的数据源
    val clickLogDataStream: DataStream[String] = getKafkaDataStream(GlobalConfigUtil.`input.topic.click_log`)
    // TODO   (2) 将 nginx 的点击流日志转化为拉宽后的点击流对象
    val clickLogWideEntity: DataStream[ClickLogWideEntity] = etl(clickLogDataStream)
    // TODO  (3) 将点击流对象进行实时拉宽 返回拉宽后的点击流实体对象
    // TODO  (4) 将拉宽后点击流实体类转换为json字符串
    val clickLogJsonDataStream: DataStream[String] = clickLogWideEntity.map(log => {
      JSON.toJSONString(log, SerializerFeature.DisableCircularReferenceDetect)
    })
    clickLogJsonDataStream.printToErr("打印测试数据")
    // TODO  (5) 将json字符串写入kafka集群
    clickLogJsonDataStream.addSink(kafkaProducer(GlobalConfigUtil.`output.topic.clicklog`))
  }

  def etl(clickLogDataStream: DataStream[String]): DataStream[ClickLogWideEntity] = {
    /*
    * 1 )将点击流日志字符串转换成点击流对象 使用 log parsing 解析
    * 2）根据ip 地址 获取到 ip 地址对应的省份 城市 访问时间等信息 需要一个ip地址库（包含ip 对应的省份 和 城市信息）
    * 3）将获取到的省份和城市拉宽后的参数传递进去 将拉宽后的点击流对象返回
    */
    val ClickLogEntityDataStream: DataStream[ClickLogEntity] = clickLogDataStream.map(new RichMapFunction[String, ClickLogEntity] {
      var parser: HttpdLoglineParser[ClickLogEntity] = _

      override def open(parameters: Configuration): Unit = {
        // 实例化解析器
        parser = ClickLogEntity.createClickLogParse()
      }

      override def map(value: String): ClickLogEntity = {
        // 将点击流字符串转换为点击流对象返回
        ClickLogEntity(value, parser)
      }
    })
    val clickLogWideDataStream: DataStream[ClickLogWideEntity] = ClickLogEntityDataStream.map(new RichMapFunction[ClickLogEntity, ClickLogWideEntity] {
      //  在公共类中自定义
      var ipSeeker: IPSeeker = _

      // 读取分布式缓存文件
      override def open(parameters: Configuration): Unit = {
        val dataFile: File = getRuntimeContext.getDistributedCache.getFile("lo")
        ipSeeker = new IPSeeker(dataFile)
      }

      override def map(clickLogEntity: ClickLogEntity): ClickLogWideEntity = {
        //        将点击流日志对象数据拷贝到拉宽后的点击流对象中
        val clickLogWideEntity: ClickLogWideEntity = ClickLogWideEntity(clickLogEntity)
        val country: String = ipSeeker.getCountry(clickLogWideEntity.ip)
        val countryString: String = country.replaceAll("省|市|区", "-")
        val countryArray: Array[String] = countryString.split("-")
        if (countryArray.size > 1) {
          clickLogWideEntity.province = countryArray(0)
          clickLogWideEntity.city = countryArray(1)
        } else {
          clickLogWideEntity.province = countryArray(0)
          clickLogWideEntity.city = "---"
        }
        clickLogWideEntity.requestDateTime = customDateFormat(dateLoad("05/Sep/2010:11:27:50 +0200"), "yyyy-MM-dd HH:mm:ss")
        println(clickLogWideEntity.province + " " + clickLogWideEntity.city)
        println(country)
        clickLogWideEntity
      }
    })
    //    返回拉宽后的点击流对象
    clickLogWideDataStream
  }
}
