package com.kafkademon;


import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

 /**
  * 负责根据key分发处理的分片号
  * @author yeguanwen
  *
  */
public class SimplePartitionertopic8 implements Partitioner  {
 
    @Override
    public void configure(Map<String, ?> configs) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
            Object value, byte[] valueBytes, Cluster cluster) {
            int partition = 0;
             List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
            int numPartitions = partitions.size();
            Integer offset = Integer.parseInt((String) key);
            if (offset > 0) {
               partition=  (offset+1) % numPartitions;
             //  System.out.println(partition);
            }
            
            return partition;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }
 
}