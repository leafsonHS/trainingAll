package com.kafkademon;


import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
/**
 * 消费者 group1 消费者1
 * @author yeguanwen
 * 
 *
 */
public class HelloWorldConsumer_group1_1 extends Thread{
	 public static int total=0;
    public  void run(){
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.160.64:9092,192.168.160.64:9093,192.168.160.64:9094");
        props.put(ConsumerConfig.GROUP_ID_CONFIG ,"test-consumer-groupa") ;
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        Consumer<String, String> consumer = new KafkaConsumer(props);
        consumer.subscribe(Arrays.asList("myreptopic8"));
       while (true) {
    	try {
			sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
            	total++;
            	System.out.printf(this.getName()+"offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
            }
            System.out.println("HelloWorldConsumer1="+total);
       }
    }
        
}