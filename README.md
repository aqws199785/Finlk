
`#0969DA`itcast_shop_canal_client
该包描述的主要是CanalClient客户端的开发


`#0969DA`itcast_shop_common
cn.itcast.canal
  protobuf
    ProtoBufable 
      一个接口 定义了一个 byte[] toBytes()方法
    ProtoBufSerializer 
      一个普通类实现Serializer<ProtoBufable>接口 
    在kafka value序列化的方式中会调用该类的serialize(String topic, ProtoBufable data)方法
      该方法会返回return data.toBytes()
  
  bean
     CanalRowData 
      定义了CanalRowData对象 定义了将Map和byte[] 转换为CanalRowData对象的构造方法 
      toByte() 将CancalRowData对象转换为二进制数组
      在kafaka value的序列化方式设置为 kafka.value_serializer_class_config=cn.itcast.canal.protobuf.ProtoBufSerializer
      因此需要实现toByte方法将CancalRowData对象转换为二进制数组
      ProtoBufSerializer的序列化方法中会调用对象的toByte方法




`#0969DA`cn.itcast.shop.realtime.etl
    AsyncOrderDetailRedisRequest 描述异步IO的处理逻辑
  process
    OrderDetailDataETL 描述订单明细调用异步IO


