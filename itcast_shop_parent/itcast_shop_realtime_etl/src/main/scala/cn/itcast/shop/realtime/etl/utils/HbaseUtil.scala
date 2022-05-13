package cn.itcast.shop.realtime.etl.utils

import cn.itcast.shop.realtime.etl.utils.pool.{ConnectionPoolConfig, HbaseConnectionPool}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration

object HbaseUtil {
  val config = new ConnectionPoolConfig
  //   最大连接数 默认20个
  config.setMaxTotal(20)
  //  最大空闲连接个数
  config.setMaxIdle(20)
  //  连接时最大等待毫秒数 如果设置为阻塞时BlockWhenExhausted 超时级抛出异常 小于零 阻塞时间不确定
  config.setMaxWaitMillis(1000)
  //  在获取连接时检查有效性
  config.setTestOnBorrow(false)
  //自动查找hbase的配置文件
  var hbaseConfig: Configuration = HBaseConfiguration.create
  hbaseConfig = HBaseConfiguration.create
  hbaseConfig.set("hbase.default.for.version.skip", "true")

  //  创建连接池对象
  lazy val pool = new HbaseConnectionPool(config, hbaseConfig)

  def getpool(): HbaseConnectionPool = {
    pool
  }
}
