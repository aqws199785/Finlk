package cn.itcast.shop.realtime.etl.app

import java.nio.file.FileSystemException

import cn.itcast.shop.realtime.etl.process.{ClickLogDataETL, GoodsDataETL, OrderDataETL, OrderDetailDataETL, syncDimData}
import cn.itcast.shop.realtime.etl.utils.GlobalConfigUtil
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.api.scala._

object APP {
  def main(args: Array[String]): Unit = {

    //TODO  1 初始化flink的运行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //TODO  2 设置flink的并行度 测试环境设置为 1 生产环境需要注意 尽可能的的使用client递交作业的时候指定的并行度
    env.setParallelism(1)
    //TODO  3 设置flink的检查点 每五秒进行一次checkpoint
    env.enableCheckpointing(5000)
    //  作业被取消时 保留之前的checkpoint 避免数据丢失
    env.getCheckpointConfig.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    //  设置同一个时间只能有一个检查点 检查点的操作是否可以并行
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    //    checkpoint的位置
    env.setStateBackend(new FsStateBackend("hdfs://five:9000/user/root/flink/checkpoint/"))
    //    checkpoint的最小时间间隔
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(1000)
    //    checkpoint的超时时间
    env.getCheckpointConfig.setCheckpointTimeout(60000)
    //  指定重启策略 默认是不停的重启
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(0, 5000))
    //    测试环境是否正确
    //    env.fromCollection(List("hadoop", "hive", "spark")).print()
    //    使用分布式缓存将ip地址资源库数据拷贝到taskManager节点上
    env.registerCachedFile(GlobalConfigUtil.`ip.file.path`, "lo")
    //TODO  4 接入kafka的数据源 消费kafak的数据


    //TODO  5 实现所有ETL

    // 5.1 维度数据增量更新到redis中
    //    val syncDataProcess: syncDimData = new syncDimData(env)
    //    syncDataProcess.process()
    //    5.2点击流日志实时ETL
    val clickLogDataETL = new ClickLogDataETL(env)
    clickLogDataETL.process()
    // 5.3订单数据实时ETL
//    val orderProcess = new OrderDataETL(env)
//    orderProcess.process()
    // 5.4订单明细数据实时ETL
    val orderDetailDataETL = new OrderDetailDataETL(env)
    orderDetailDataETL.process()

    // 5.5 商品数据的2实时ETL
    val goodsDataProcess = new GoodsDataETL(env)
    goodsDataProcess.process

    println("----------------------------------------")
    //TODO  6 执行任务
    env.execute()
  }
}
