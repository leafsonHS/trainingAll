package com.crenjoy.easypr.test;

import java.util.Vector;

import org.junit.Test;

import com.crenjoy.easypr.core.Plate;
import com.crenjoy.easypr.core.PlateLocateColor;
import com.crenjoy.easypr.enums.PlateColor;
import com.crenjoy.easypr.util.OpenCVUtils;
import com.crenjoy.easypr.util.PlateTestUtils;

/**
 * 测试通过颜色定位车牌
 * @author CGD
 *
 */
public class PlateLocateColorTest {

	@Test
	public void test(){
		String image="D:\\test\\easypr\\test\\5.jpg";
		PlateLocateColor colorLocate=new PlateLocateColor();
		
		Vector<Plate> locates=colorLocate.locate(OpenCVUtils.imread(image),PlateColor.BLUE);
		
		PlateTestUtils.imwrite(locates, "color_locate");
	}
}
