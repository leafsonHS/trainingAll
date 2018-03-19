package com.crenjoy.easypr.test;

import org.junit.Test;

import com.crenjoy.easypr.train.SvmTrainImpl;
import com.crenjoy.easypr.train.Train;

/**
 * 是否是车牌训练学习
 * @author CGD
 *
 */
public class SvmTrainTest {
	
	/**
	 * 车牌图片识别
	 */
	@Test
	public void testSVM(){
	
		String xml="D:\\test\\easypr\\resources\\model\\svm.xml";
		
		
	
		Train svmImpl=new SvmTrainImpl();
		String plates_folder_="D:\\test\\easypr\\resources\\train\\svm";
		svmImpl.firstTrain(plates_folder_, xml, true);
		
		
//		String plates_folder_append_="D:\\test\\easypr\\resources\\train\\append_svm";
//		svmImpl.appendTrain(plates_folder_append_, xml, true);
		
	}

}
