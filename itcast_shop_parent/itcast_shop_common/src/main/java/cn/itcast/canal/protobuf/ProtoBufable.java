package cn.itcast.canal.protobuf;

/*
 * 定义protobuf序列化接口
 * 这个接口的定义的是返回byte[] 二进制数组
 *   所有的能使用protobuf 序列化的bean都需要集成该接口
 */
public interface ProtoBufable {
//将对象转化成二进制数组
    byte[] toBytes();
}
