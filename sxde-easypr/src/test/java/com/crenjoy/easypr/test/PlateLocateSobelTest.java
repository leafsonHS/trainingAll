package com.crenjoy.easypr.test;

import java.util.Vector;

import org.junit.Test;

import com.crenjoy.easypr.core.Plate;
import com.crenjoy.easypr.core.PlateLocate;
import com.crenjoy.easypr.core.PlateLocateSobel;
import com.crenjoy.easypr.util.OpenCVUtils;
import com.crenjoy.easypr.util.PlateTestUtils;

/**
 * 测试通过Sobel定位车牌
 * @author CGD
 *
 */
public class PlateLocateSobelTest {

	
	@Test
	public void test(){
		String image="D:\\test\\easypr\\test\\1.jpg";
		PlateLocate sobelLocate=new PlateLocateSobel();
		
		Vector<Plate> locates=sobelLocate.locate(OpenCVUtils.imread(image));
		
		PlateTestUtils.imwrite(locates, "sobel_locate");
	}
}
