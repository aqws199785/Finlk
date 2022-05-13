package cn.itcast.shop.realtime.etl.bean

import nl.basjes.parse.core.Parser
import nl.basjes.parse.httpdlog.HttpdLoglineParser

import scala.beans.BeanProperty

class ClickLogEntity {
  //用户id信息
  private[this] var _connectionClientUser: String = _
  def setConnectionClientUser (value: String): Unit = { _connectionClientUser = value }
  def getConnectionClientUser = { _connectionClientUser }

  //ip地址
  private[this] var _ip: String = _
  def setIp (value: String): Unit = { _ip = value }
  def getIp = {  _ip }

  //请求时间
  private[this] var _requestTime: String = _
  def setRequestTime (value: String): Unit = { _requestTime = value }
  def getRequestTime = { _requestTime }

  //请求方式
  private[this] var _method:String = _
  def setMethod(value:String) = {_method = value}
  def getMethod = {_method}

  //请求资源
  private[this] var _resolution:String = _
  def setResolution(value:String) = { _resolution = value}
  def getResolution = { _resolution }

  //请求协议
  private[this] var _requestProtocol: String = _
  def setRequestProtocol (value: String): Unit = { _requestProtocol = value }
  def getRequestProtocol = { _requestProtocol }

  //响应码
  private[this] var _responseStatus: String = _
  def setRequestStatus (value: String): Unit = { _responseStatus = value }
  def getRequestStatus = { _responseStatus }

  //返回的数据流量
  private[this] var _responseBodyBytes: String = _
  def setResponseBodyBytes (value: String): Unit = { _responseBodyBytes = value }
  def getResponseBodyBytes = { _responseBodyBytes }

  //访客的来源url
  private[this] var _referer: String = _
  def setReferer (value: String): Unit = { _referer = value }
  def getReferer = { _referer }

  //客户端代理信息
  private[this] var _userAgent: String = _
  def setUserAgent (value: String): Unit = { _userAgent = value }
  def getUserAgent = { _userAgent }

  //跳转过来页面的域名:HTTP.HOST:request.referer.host
  private[this] var _referDomain: String = _
  def setReferDomain (value: String): Unit = { _referDomain = value }
  def getReferDomain = { _referDomain }
}
//  传递点击流日志解析出来赋值scala类
object ClickLogEntity {
  //  定义点击流日志解析规则
  val getLogFormat: String = "%u %h %l %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\""

  // TODO 只创建一次解析器 而不需要每来一条数据创建一次解析器
  def apply(clickLog: String, parser: HttpdLoglineParser[ClickLogEntity]): ClickLogEntity = {
    val clickLogEntity = new ClickLogEntity()
//    parser.parse(clickLogEntity, "2001:980:91c0:1:8d31:a232:25e5:85d 222.68.172.190 - [05/Sep/2010:11:27:50 +0200] \"GET /images/my.jpg HTTP/1.1\" 404 23617 \"http://www.angularjs.cn/A00n\" \"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; nl-nl) AppleWebKit/533.17.8 (KHTML, like Gecko) Version/5.0.1 Safari/533.17.8\"")
    parser.parse(clickLogEntity,clickLog)
    //    将解析后的日志对象返回
    println("******************************");
    clickLogEntity
  }

  // TODO 创建解析器
  def createClickLogParse(): HttpdLoglineParser[ClickLogEntity] = {
    //    2.1 创建解析器
    val parser = new HttpdLoglineParser[ClickLogEntity](classOf[ClickLogEntity], getLogFormat)
    //    2.2 建立映射关系

    parser.addParseTarget("setConnectionClientUser", "STRING:connection.client.user")
    parser.addParseTarget("setIp", "IP:connection.client.host")
    parser.addParseTarget("setRequestTime", "TIME.STAMP:request.receive.time")
    parser.addParseTarget("setMethod", "HTTP.METHOD:request.firstline.method")
    parser.addParseTarget("setResolution", "HTTP.URI:request.firstline.uri")
    parser.addParseTarget("setRequestProtocol", "HTTP.PROTOCOL_VERSION:request.firstline.protocol")
    parser.addParseTarget("setResponseBodyBytes", "BYTES:response.body.bytes")
    parser.addParseTarget("setReferer", "HTTP.URI:request.referer")
    parser.addParseTarget("setUserAgent", "HTTP.USERAGENT:request.user-agent")
    parser.addParseTarget("setReferDomain", "HTTP.HOST:request.referer.host")

//    返回解析器 注意返回值参数类型为 HttpdLoglineParser[ClickLogEntity] 不可是父类的 Parser
    parser
  }

  //
  def main(args: Array[String]): Unit = {
    //  解析数据样本
    val logline = "2001:980:91c0:1:8d31:a232:25e5:85d 222.68.172.190 - [05/Sep/2010:11:27:50 +0200] \"GET /images/my.jpg HTTP/1.1\" 404 23617 \"http://www.angularjs.cn/A00n\" \"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; nl-nl) AppleWebKit/533.17.8 (KHTML, like Gecko) Version/5.0.1 Safari/533.17.8\""
//    解析器
    val clickLogEntity = new ClickLogEntity
    val parser: Parser[ClickLogEntity] = createClickLogParse()
    parser.parse(clickLogEntity,logline)

    println(clickLogEntity.getConnectionClientUser)
    println(clickLogEntity.getIp)
    println(clickLogEntity.getRequestTime)
    println(clickLogEntity.getMethod)
    println(clickLogEntity.getResolution)
    println(clickLogEntity.getRequestProtocol)
    println(clickLogEntity.getResponseBodyBytes)
    println(clickLogEntity.getReferer)
    println(clickLogEntity.getUserAgent)
    println(clickLogEntity.getReferDomain)
  }
}

case class ClickLogWideEntity(
//             因为后续需要将拉宽后的点击流对象序列化
//             @BeanProperty 序列化之后可以获取 get() 和 set()
                               @BeanProperty uid:String,
                               @BeanProperty ip:String,
                               @BeanProperty requestTime:String,
                               @BeanProperty requestMethod:String,
                               @BeanProperty requestUrl:String,
                               @BeanProperty requestProtocol:String,
                               @BeanProperty requestStatus:String,
                               @BeanProperty requestBodyBytes:String,
                               @BeanProperty referer:String,
                               @BeanProperty userAgent:String,
                               @BeanProperty refererDomain:String,
                               @BeanProperty var province:String,
                               @BeanProperty var city:String,
                               @BeanProperty var requestDateTime:String
                             )

object ClickLogWideEntity {
//  将原先的日志数据根据IP解析出 省 市 请求时间 拓宽数据
  def apply(clickLogEntity: ClickLogEntity): ClickLogWideEntity = {
    ClickLogWideEntity(
      clickLogEntity.getConnectionClientUser,
      clickLogEntity.getIp,
      clickLogEntity.getRequestTime,
      clickLogEntity.getMethod,
      clickLogEntity.getResolution,
      clickLogEntity.getRequestProtocol,
      clickLogEntity.getRequestStatus,
      clickLogEntity.getResponseBodyBytes,
      clickLogEntity.getReferer,
      clickLogEntity.getUserAgent,
      clickLogEntity.getReferDomain,
      "",
      "",
      ""
    )
  }
}