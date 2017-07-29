package com.kafkademon;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
/**
 * 生产者 负责向kafka写数据
 * @author yeguanwen
 *
 */
public class HelloWorldProducer1 extends Thread {
	 public  void run() {
         Random rnd = new Random();
    
         Properties props = new Properties();
         //props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.100.242:9093,192.168.100.242:9092,192.168.160.64:9092,192.168.160.64:9093,192.168.160.64:9094,192.168.100.160.64:9095");
         props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.100.242:9092,192.168.100.242:9093,192.168.160.64:9092,192.168.160.64:9093,192.168.160.64:9094,192.168.160.64:9095");
         
         props.put(ProducerConfig.CLIENT_ID_CONFIG, "1022");
//         props.put("acks", "all");
//         props.put("retries", 0);
//         props.put("batch.size", 16384);
//         props.put("linger.ms", 1);
//         props.put("buffer.memory", 33554432);
         props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
         props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
         //配置partitionner选择策略，可选配置
        props.put("partitioner.class", "com.kafkademon.SimplePartitionertopic8");
         Producer<String, String> producer = new KafkaProducer<String, String>(props);
         System.out.println(new Date());
         for (long nEvents = 0; nEvents < 10000000; nEvents++) { 
        	 
//        	  String messageStr = "Message_" + nEvents;
//              System.out.println("Send:" + messageStr);
//              producer.send(new ProducerRecord<String, String>("myreptopic", messageStr));
//        	 
                long runtime = new Date().getTime();  
                String ip =  ""+nEvents;
              String msg =  ip+",1,1,1501210253,1,1030036,1,1,1,1,1,1,1,31011639001041,1,1,1"; 
            //  String ip =  "111";
              //  String msg =  "111,1,1,1501210253,1,1030036,1,1,1,1,1,1,1,31011639001041,1,1,1"; 
               
                ProducerRecord data = new ProducerRecord("myreptopic8", ip, msg);
                producer.send(data);
//                producer.send(data,
//                         new Callback() {
//                     public void onCompletion(RecordMetadata metadata, Exception e) {
//                         if(e != null) {
//                            e.printStackTrace();
//                         } else {
//                            System.out.println("The offset of the record we just sent is: " + metadata.offset());
//                         }
//                     }
//                 });
         }
         
         System.out.println(new Date());
         producer.close();
    }
}
