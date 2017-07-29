package com.kafkademon;

public class KafkaConsumerProducerDemo {

	
	
	public static void main(String [] aa){
		
		new KafkaConsumerProducerDemo().go();
		
	}
	public  void go()
	{
		new HelloWorldProducer1().start();
		new HelloWorldProducer2().start();
		//new HelloWorldConsumer_group1_1().start();
		//new HelloWorldConsumer_group2_0().start();
		//new HelloWorldConsumer_group2_1().start();
	
		
		
	}
}
