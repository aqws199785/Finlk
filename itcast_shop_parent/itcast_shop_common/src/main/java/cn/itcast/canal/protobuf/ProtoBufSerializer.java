package cn.itcast.canal.protobuf;

import org.apache.kafka.common.serialization.Serializer;
import java.util.Map;
/*
 * 实现kafka-value的序列化对象
 *  要求传递的泛型必须是继承ProtoBufable接口的实现类 才可以被序列化成功
 */

public class ProtoBufSerializer implements Serializer<ProtoBufable> {
    @Override
    public void configure(Map map, boolean b) {

    }

    @Override
    public byte[] serialize(String s, ProtoBufable data) {
        return data.toBytes();
    }

    @Override
    public void close() {

    }
}
