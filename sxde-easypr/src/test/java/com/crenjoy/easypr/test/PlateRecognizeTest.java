package com.crenjoy.easypr.test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.crenjoy.easypr.core.Plate;
import com.crenjoy.easypr.core.PlateRecognition;
import com.crenjoy.easypr.core.PlateRecognitionImpl;
import com.crenjoy.easypr.core.ReTrain;

/**
 * 车牌识别测试
 * 
 * @author CGD
 * 
 */
public class PlateRecognizeTest {

	private static final Log log = LogFactory.getLog(PlateRecognizeTest.class);

	

	@Test
	public void test() throws IOException {
		log.debug("TEST Start");
		ReTrain train=new ReTrain("C:\\DEV1\\workspace\\sxde-easypr\\src\\main\\resources");
	 train.replateFileName("C:\\Users\\leafson\\Desktop\\easypr\\test");
		train.recognize_1("C:\\Users\\leafson\\Desktop\\easypr\\test", "C:\\Users\\leafson\\Desktop\\easypr\\test_error");
		train.recognize_2("C:\\Users\\leafson\\Desktop\\easypr\\test_error", "C:\\Users\\leafson\\Desktop\\easypr\\test_train");
		log.debug("TEST End");
        
	}
	
//
//	@Test
//	public void test(){
//		log.debug("TEST Start");
//		
//		//加载识别机器XML 
//		PlateRecognition pr=new PlateRecognitionImpl("C:\\Users\\leafson\\Desktop\\easypr\\resources");
//		log.debug("TEST ----1");
//		
//		
//		File files=new File("C:\\Users\\leafson\\Desktop\\easypr\\test");
//		
//			//识别车牌 
//			for(File file : files.listFiles()){
//				if (file.isDirectory()){
//					continue;
//				}
//				System.out.println(file.getAbsolutePath());
//				Map<String,Plate> ps= pr.plateRecognize(file.getAbsolutePath());
//				for(Plate p:ps.values()){
//					System.out.println(p.toString());
//				}
//				if (ps.size()<=1){
//				//	File dest=new File("D:\\test\\easypr\\test_ok\\"+file.getName());
//				//	file.renameTo(dest);
//				}
//				
//				
//			}
//		
//			//识别车牌 
//			for(File file : files.listFiles()){
//				if (file.isDirectory()){
//					continue;
//				}
//				System.out.println(file.getAbsolutePath());
//				Map<String,Plate> ps= pr.plateRecognize(file.getAbsolutePath());
//				for(Plate p:ps.values()){
//					System.out.println(p.toString());
//				}
//				if (ps.size()<=1){
//				//	File dest=new File("D:\\test\\easypr\\test_ok\\"+file.getName());
//				//	file.renameTo(dest);
//				}
//				
//				
//			}
//	}
}
