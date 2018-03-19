package com.crenjoy.easypr.core;

import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.MORPH_CLOSE;
import static org.bytedeco.javacpp.opencv_imgproc.MORPH_RECT;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.getStructuringElement;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;
import static org.bytedeco.javacpp.opencv_imgproc.morphologyEx;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_core.Size;

import com.crenjoy.easypr.enums.LocateType;
import com.crenjoy.easypr.enums.PlateColor;

/**
 * 通过车牌颜色定位车牌 
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 * 
 */
public class PlateLocateColor extends PlateLocate {

	private static final Log log = LogFactory.getLog(PlateLocateColor.class);

	/**
	 * 车牌颜色定位车牌构造函数
	 * 
	 * @param plateColor
	 */
	public PlateLocateColor() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.crenjoy.easypr.core.PlateLocate#locate(org.bytedeco.javacpp.opencv_core
	 * .Mat)
	 */
	public Vector<Plate> locate(Mat src) {
		Vector<Plate> allPlate = new Vector<Plate>();

		Vector<Plate> bluePlate = locate(src, PlateColor.BLUE);
		allPlate.addAll(bluePlate);

		Vector<Plate> yellowPlate = locate(src, PlateColor.YELLOW);
		allPlate.addAll(yellowPlate);

		return allPlate;

	}

	/**
	 * 定位车牌
	 * 
	 * @param src
	 * @return
	 */
	public Vector<Plate> locate(Mat src, PlateColor plateColor) {
		Vector<Plate> plates = new Vector<Plate>();
		// 查找Color车牌
		Mat src_b = new Mat();
		
		Vector<RotatedRect> rects_color = new Vector<RotatedRect>();
		// 查找颜色匹配车牌
		colorSearch(src, plateColor, src_b, rects_color);
		log.debug("plateColorLocate  " + plateColor.name() + rects_color.size());
//		PlateTestUtils.imwrite(src, plateColor.name() + "_match", rects_color.toArray(new RotatedRect[0]));
		// 进行抗扭斜处理
		deskew(src, src_b, rects_color, plates);
		
		for(Plate plate: plates){
			plate.setLocateType(LocateType.COLOR);
			plate.setPlateColor(plateColor);
		}
		
//		PlateTestUtils.imwrite(plates, plateColor.name() + "_deskew");

		return plates;
	}

	/**
	 * Color搜索 基于HSV空间的颜色搜索方法
	 * 
	 * @return
	 */
	public int colorSearch(final Mat src, final PlateColor r, Mat out, Vector<RotatedRect> outRects) {
		// width值对最终结果影响很大，可以考虑进行多次colorSerch，每次不同的值
		// 另一种解决方案就是在结果输出到SVM之前，进行线与角的再纠正
		final int color_morph_width = 10;// 10
		final int color_morph_height = 2;// 2

		// 进行颜色查找
		Mat match_grey = CoreFunc.colorMatch(src, r, true);

//		PlateTestUtils.imwrite(match_grey, "match_grey");

		Mat src_threshold = new Mat();
		threshold(match_grey, src_threshold, 0, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);

		Mat element = getStructuringElement(MORPH_RECT, new Size(color_morph_width, color_morph_height));
		morphologyEx(src_threshold, src_threshold, MORPH_CLOSE, element);

//   	PlateTestUtils.imwrite(match_grey, "match_color");

		src_threshold.copyTo(out);

		// 查找轮廓
		MatVector contours = new MatVector();

		// 注意，findContours会改变src_threshold
		// 因此要输出src_threshold必须在这之前使用copyTo方法
		findContours(src_threshold, contours, // a vector of contours
				CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE); // all pixels of each
															// contours

		for (int i = 0; i < contours.size(); i++) {
			Mat itc = contours.get(i);
			RotatedRect mr = minAreaRect(itc);

			// 需要进行大小尺寸判断
			if (verifySizes(mr)) {
				outRects.add(mr);
			}
		}
		// PlateTestUtils.imwrite(src, "size", outRects);
		return 0;
	}

}
