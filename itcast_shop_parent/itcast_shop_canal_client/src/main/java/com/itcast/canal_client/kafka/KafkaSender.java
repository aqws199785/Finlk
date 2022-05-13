package com.itcast.canal_client.kafka;

import cn.itcast.canal.bean.CanalRowData;
import com.itcast.canal_client.util.ConfigUitl;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import sun.applet.Main;

import java.util.Properties;
/*
 * kafka 生产者工具类
 */

public class KafkaSender {
//    定义properties对象 封装kafka的相关参数
    private Properties kafkaProperties = new Properties();
//    定义生产者对象，value使用的是自定义序列化方式 该序列化方式要求传递的是一个Protobufable的子类
    private KafkaProducer<String,CanalRowData> kafkaProducer;
    public KafkaSender(){
        kafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigUitl.kafkaBootstrap_servers_config());
        kafkaProperties.put(ProducerConfig.BATCH_SIZE_CONFIG,ConfigUitl.kafkaBatch_size_config());
        kafkaProperties.put(ProducerConfig.ACKS_CONFIG,ConfigUitl.kafkaAcks());
        kafkaProperties.put(ProducerConfig.RETRIES_CONFIG,ConfigUitl.kafkaRetries());
        kafkaProperties.put(ProducerConfig.CLIENT_ID_CONFIG,ConfigUitl.kafkaClient_id_config());
        kafkaProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,ConfigUitl.kafkaKey_serializer_class_config());
        kafkaProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,ConfigUitl.kafkaValue_serializer_class_config());
        kafkaProducer = new KafkaProducer<String,CanalRowData>(kafkaProperties);
    }

    public static void main(String[] args) {

    }
    public void send(CanalRowData rowData){
        System.out.println(kafkaProducer);
        kafkaProducer.send(new ProducerRecord<>(ConfigUitl.kafkaTopic(),rowData));
    }
}
