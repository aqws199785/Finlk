package cn.itcast.shop.realtime.etl.utils

import cn.itcast.shop.realtime.etl.utils
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

object RedisUtil {
  val config = new JedisPoolConfig()
//  是否采用先进先出 默认为true
//  config.setLifo(true)
//  最大空闲连接数据 默认8个
  config.setMaxIdle(8)
//  最大连接个数 默认8个
  config.setMaxTotal(1000)
//  获取连接时的最大等待毫秒数 (如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
  config.setMaxWaitMillis(-1)
  //  退出连接的最小空闲时间 默认30分钟 1800000毫秒
  config.setMinEvictableIdleTimeMillis(1800000)
//  最小空闲连接数，默认0
  config.setMinIdle(0)
//  每次退出检查时 退出的最大数目 如果为负数就是1/abs(n),默认3
  config.setNumTestsPerEvictionRun(3)
//  对象空闲多久后退出 当空闲时间>该值 且 空闲连接>最大空闲数 时直接逐出,不再根据MinEvictableIdleTimeMillis判断  (默认逐出策略)
  config.setSoftMinEvictableIdleTimeMillis(1800000)
//  在获取连接时检查有效性 默认false
  config.setTestOnBorrow(false)
//  在空闲时间检查有效性 默认false
  config.setTestWhileIdle(false)

//  初始化连接对象
 var jedisPool = new JedisPool(
  config,
  GlobalConfigUtil.`redis.server.ip`,
  GlobalConfigUtil.`redis.server.port`.toInt,
   1000,
   GlobalConfigUtil.`redis.server.password`)
//  获取Redis连接
  def getJedis(): Jedis ={
    jedisPool.getResource
  }
}


//import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}
//
////使用redis连接池的工具类，该工具类主要是针对redis的单机环境
////如果大家需要在redis分布式集群环境运行的话，这个代码不适用
//object RedisUtil {
//  //定义redis的配置对象
//  val config = new JedisPoolConfig()
//  config.setMaxTotal(1000)    //设置最大的连接数
//  config.setMaxIdle(8)     //设置最大的空闲连接数
//
//  //实例化redis连接池对象
//  private lazy val pool = new JedisPool(config, GlobalConfigUtil.`redis.server.ip`, GlobalConfigUtil.`redis.server.port`.toInt)
//
//  //获取一个redis的连接对象
//  def getJedis()= {
//    val jedis: Jedis = pool.getResource
//    //返回jedis对象
//    jedis
//  }
//}