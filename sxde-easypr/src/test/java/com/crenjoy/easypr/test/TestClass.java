package com.crenjoy.easypr.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.junit.Test;

import com.crenjoy.easypr.ObjectPointer;
import com.crenjoy.easypr.core.CoreFunc;
import com.crenjoy.easypr.util.OpenCVUtils;
import com.crenjoy.easypr.util.PlateTestUtils;

public class TestClass {

	private static final Log log = LogFactory.getLog(TestClass.class);

	@Test
	public void test() {
		String image = "D:\\test\\easypr\\resources\\image\\tmp\\test.jpg";

		Mat img = OpenCVUtils.imread(image);

		int[] border = CoreFunc.clearLiuDingBorder(img);
		int top = border[0];
		int bottom = border[1];
		log.debug(String.format("top %d bottom %d", border[0], border[1]));

		ObjectPointer<Integer> posLeft = new ObjectPointer<Integer>(0);
		ObjectPointer<Integer> posRight = new ObjectPointer<Integer>(0);

		if (CoreFunc.bFindLeftRightBound1(img, posLeft, posRight)) {
			log.debug(String.format("left %d right %d", posLeft.get(), posRight.get()));

			Mat inmat = new Mat(img, new Rect(posLeft.get(), top, posRight.get() - posLeft.get(), bottom - top));

			PlateTestUtils.imwrite(inmat, "inputgray2_ok");

		}

	}
	
	
}
