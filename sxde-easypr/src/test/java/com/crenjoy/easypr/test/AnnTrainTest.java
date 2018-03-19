package com.crenjoy.easypr.test;

import org.junit.Test;

import com.crenjoy.easypr.train.AnnTrainImpl;
import com.crenjoy.easypr.train.Train;

/**
 * 测试字符识别
 * @author CGD
 *
 */
public class AnnTrainTest {
	
	/**
	 * 字符识别训练
	 */
	@Test
	public void testAnn(){
		String plates_folder_="D:\\test\\easypr\\resources\\train\\ann";
		String xml="D:\\test\\easypr\\resources\\model\\ann.xml";
		
		Train annImpl=new AnnTrainImpl();
		annImpl.firstTrain(plates_folder_, xml, true);
	}
}
