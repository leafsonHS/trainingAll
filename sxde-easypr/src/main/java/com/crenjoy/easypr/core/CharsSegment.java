package com.crenjoy.easypr.core;

import static org.bytedeco.javacpp.opencv_core.BORDER_CONSTANT;
import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.countNonZero;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY_INV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.FloatIndexer;

import com.crenjoy.easypr.enums.PlateColor;

/**
 * 字符分割
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 * 
 */
public class CharsSegment {

	/** 是否开启调试模式常量，默认0代表关闭 */
	private static final int DEFAULT_DEBUG = 1;

	/** preprocessChar所用常量 */
	private static final int CHAR_SIZE = 20;
//	private static final int HORIZONTAL = 1;
//	private static final int VERTICAL = 0;

	/** preprocessChar所用常量 */

	private static final int DEFAULT_LIUDING_SIZE = 7;
	private static final int DEFAULT_MAT_WIDTH = 136;
	private static final int DEFAULT_COLORTHRESHOLD = 150;

	private static final float DEFAULT_BLUEPERCEMT = 0.3f;
	private static final float DEFAULT_WHITEPERCEMT = 0.1f;

	/** 柳钉判断参数 */
	private int m_LiuDingSize;

	/** 车牌大小参数 */
	private int m_theMatWidth;

	/** 车牌颜色判断参数 */
	private int m_ColorThreshold;
	private float m_BluePercent;
	private float m_WhitePercent;
	/** 是否开启调试模式，0关闭，非0开启 */
	private int m_debug;

	public CharsSegment() {
		this.m_LiuDingSize = DEFAULT_LIUDING_SIZE;
		this.m_theMatWidth = DEFAULT_MAT_WIDTH;

		// ！车牌颜色判断参数
		this.m_ColorThreshold = DEFAULT_COLORTHRESHOLD;
		this.m_BluePercent = DEFAULT_BLUEPERCEMT;
		this.m_WhitePercent = DEFAULT_WHITEPERCEMT;

		this.m_debug = DEFAULT_DEBUG;
	}

	/**
	 * 字符分割
	 * 
	 * @param input
	 * @param resultVec
	 * @return
	 */
	public int charsSegment(Mat input, Vector<Mat> resultVec) {
		if (input.data().isNull()) {
			return 0x01;
		}

		int w = input.cols();
		int h = input.rows();

		// 判断车牌颜色以此确认threshold方法
		Mat tmpMat = new Mat(input, new Rect((int) (w * 0.1), (int) (h * 0.1), (int) (w * 0.8), (int) (h * 0.8)));
		PlateColor plateType = CoreFunc.getPlateType(tmpMat, true);

		Mat input_grey = new Mat();
		cvtColor(input, input_grey, CV_RGB2GRAY);

		Mat img_threshold = null;
		// 二值化
		// 根据车牌的不同颜色使用不同的阈值判断方法
		// 使用MSER来提取这些轮廓
		if (PlateColor.BLUE.equals(plateType)) {
			img_threshold = input_grey.clone();

			w = input_grey.cols();
			h = input_grey.rows();

			Mat tmp = new Mat(input_grey, new Rect((int) (w * 0.1), (int) (h * 0.1), (int) (w * 0.8), (int) (h * 0.8)));

			int threadHoldV = CoreFunc.ThresholdOtsu(tmp);

			threshold(input_grey, img_threshold, threadHoldV, 255, CV_THRESH_BINARY);
		} else if (PlateColor.YELLOW.equals(plateType)) {
			img_threshold = input_grey.clone();
			w = input_grey.cols();
			h = input_grey.rows();

			Mat tmp = new Mat(input_grey, new Rect((int) (w * 0.1), (int) (h * 0.1), (int) (w * 0.8), (int) (h * 0.8)));

			int threadHoldV = CoreFunc.ThresholdOtsu(tmp);

			threshold(input_grey, img_threshold, threadHoldV, 255, CV_THRESH_BINARY_INV);
		} else if (PlateColor.WHITE.equals(plateType)) {
			img_threshold = input_grey.clone();
			threshold(input_grey, img_threshold, 10, 255, CV_THRESH_OTSU + CV_THRESH_BINARY_INV);
		} else {
			img_threshold = input_grey.clone();
			threshold(input_grey, img_threshold, 10, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);
		}

		// 去除车牌上方的柳钉以及下方的横线等干扰
		// 并且也判断了是否是车牌
		// 并且在此对字符的跳变次数以及字符颜色所占的比重做了是否是车牌的判别条件
		// 如果不是车牌，返回ErrorCode=0x02
		if (!CoreFunc.clearLiuDing(img_threshold)) {
			return 0x02;
		}

		//PlateTestUtils.imwrite(img_threshold, "clearliuding");

		// 在二值化图像中提取轮廓
		Mat img_contours = new Mat();
		img_threshold.copyTo(img_contours);
		
		MatVector contours = new MatVector();
		findContours(img_contours, contours, // a vector of contours
				CV_RETR_EXTERNAL, // retrieve the external contours
				CV_CHAIN_APPROX_NONE); // all pixels of each contours

		Vector<Rect> vecRect = new Vector<Rect>();
		// 将不符合特定尺寸的字符块排除出去
		for (int i = 0; i < contours.size(); i++) {
			Mat mat = contours.get(i);
			Rect mr = boundingRect(mat);
			Mat auxRoi = new Mat(img_threshold, mr);
		//	PlateTestUtils.imwrite(auxRoi, "auxRoi");
			if (verifyCharSizes(auxRoi)) {
				//PlateTestUtils.imwrite(auxRoi, "auxRoi");
				vecRect.add(mr);
			}
		}
		// 如果找不到任何字符块，则返回ErrorCode=0x03
		if (vecRect.size() == 0) {
			return 0x03;
		}

		// 对符合尺寸的图块按照从左到右进行排序;
		// 直接使用stl的sort方法，更有效率
		Collections.sort(vecRect, new Comparator<Rect>() {
			public int compare(Rect o1, Rect o2) {
				if (o1.x() == o2.x()) {
					return 0;
				}
				if (o1.x() > o2.x()) {
					return 1;
				} else {
					return -1;
				}
			}
		});
		
		
		//PlateTestUtils.imwriteRect(input, "sort", vecRect);

		// 获得特殊字符对应的Rectt,如苏A的"A"
		int specIndex = GetSpecificRect(vecRect);

		// 根据特定Rect向左反推出中文字符
		// 这样做的主要原因是根据findContours方法很难捕捉到中文字符的准确Rect，因此仅能
		// 退过特定算法来指定
		if (specIndex >= vecRect.size()) {
			return 0x04;
		}
		Rect chineseRect = GetChineseRect(vecRect.get(specIndex));

		// 新建一个全新的排序Rect
		// 将中文字符Rect第一个加进来，因为它肯定是最左边的
		// 其余的Rect只按照顺序去6个，车牌只可能是7个字符！这样可以避免阴影导致的“1”字符
		Vector<Rect> newSortedRect = new Vector<Rect>();

		newSortedRect.add(chineseRect);
		
		RebuildRect(vecRect, newSortedRect, specIndex);
		if (newSortedRect.size() == 0) {
			return 0x05;
		}
//		PlateTestUtils.imwrite(input, "sort_rect", newSortedRect.toArray(new Rect[0]));

		// 开始截取每个字符
		for (int i = 0; i < newSortedRect.size(); i++) {
			Rect mr = newSortedRect.get(i);
			// 使用灰度图来截取图块，然后依次对每个图块进行大津阈值来二值化
			Mat auxRoi = new Mat(input_grey, mr);
			Mat newRoi = new Mat();

			if (PlateColor.BLUE.equals(plateType)) {
				threshold(auxRoi, newRoi, 5, 255, CV_THRESH_BINARY + CV_THRESH_OTSU);
			} else if (PlateColor.YELLOW.equals(plateType)) {
				threshold(auxRoi, newRoi, 5, 255, CV_THRESH_BINARY_INV + CV_THRESH_OTSU);
			} else if (PlateColor.WHITE.equals(plateType)) {
				threshold(auxRoi, newRoi, 5, 255, CV_THRESH_OTSU + CV_THRESH_BINARY_INV);
			} else {
				threshold(auxRoi, newRoi, 5, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);
			}
			// 归一化大小
			newRoi = preprocessChar(newRoi);
			// 每个字符图块输入到下面的步骤进行处理
			resultVec.add(newRoi);
		}
		return 0;

	}

	/**
	 * 根据特殊车牌来构造猜测中文字符的位置和大小
	 * 
	 * @return
	 */
	public Rect GetChineseRect(final Rect rectSpe) {
		int height = rectSpe.height();
		float newwidth = rectSpe.width() * 1.15f;
		int x = rectSpe.x();
		int y = rectSpe.y();
		int newx = x - (int) (newwidth * 1.15);
		newx = newx > 0 ? newx : 0;
		Rect a = new Rect(newx, y, (int) (newwidth), height);
		return a;
	}

	/**
	 * 找出指示城市的字符的Rect，例如苏A7003X，就是A的位置
	 * 
	 * @return
	 */
	public int GetSpecificRect(final List<Rect> vecRect) {
		Vector<Integer> xpositions = new Vector<Integer>();
		int maxHeight = 0;
		int maxWidth = 0;

		for (int i = 0; i < vecRect.size(); i++) {
			xpositions.add(vecRect.get(i).x());

			if (vecRect.get(i).height() > maxHeight) {
				maxHeight = vecRect.get(i).height();
			}
			if (vecRect.get(i).width() > maxWidth) {
				maxWidth = vecRect.get(i).width();
			}
		}

		int specIndex = 0;
		for (int i = 0; i < vecRect.size(); i++) {
			Rect mr = vecRect.get(i);
			int midx = mr.x() + mr.width() / 2;

			// 如果一个字符有一定的大小，并且在整个车牌的1/7到2/7之间，则是我们要找的特殊字符
			// 当前字符和下个字符的距离在一定的范围内

			if ((mr.width() > maxWidth * 0.8 || mr.height() > maxHeight * 0.8)
					&& (midx < (int) (m_theMatWidth / 7) * 2 && midx > (int) (m_theMatWidth / 7) * 1)) {
				specIndex = i;
			}
		}

		return specIndex;
	}

	// ! 这个函数做两个事情
	// 1.把特殊字符Rect左边的全部Rect去掉，后面再重建中文字符的位置。
	// 2.从特殊字符Rect开始，依次选择6个Rect，多余的舍去。

	public int RebuildRect(final List<Rect> vecRect, List<Rect> outRect, int specIndex) {
		int count = 6;
		for (int i = specIndex; (i < vecRect.size()) && (count > 0); ++i, --count) {
			outRect.add(vecRect.get(i));
		}
		return 0;
	}

	/**
	 * 字符尺寸验证
	 * 
	 * @param r
	 * @return
	 */
	public boolean verifyCharSizes(Mat r) {
		  // Char sizes 45x90
		  float aspect = 45.0f / 90.0f;
		  float charAspect = (float)r.cols() / (float)r.rows();
		  float error = 0.7f;
		  float minHeight = 10.f;
		  float maxHeight = 35.f;
		  // We have a different aspect ratio for number 1, and it can be ~0.2
		  float minAspect = 0.05f;
		  float maxAspect = aspect + aspect * error;
		  // area of pixels
		  int area = countNonZero(r);
		  // bb area
		  int bbArea = r.cols() * r.rows();
		  //% of pixel in area
		  int percPixels = area / bbArea;

		  if (percPixels <= 1 && charAspect > minAspect && charAspect < maxAspect &&
		      r.rows() >= minHeight && r.rows() < maxHeight)
		    return true;
		  else
		    return false;
	}

	/**
	 * 字符预处理
	 * 
	 * @param in
	 * @return
	 */
	public Mat preprocessChar(Mat in) {
		// Remap image
		int h = in.rows();
		int w = in.cols();

		// 统一每个字符的大小

		int charSize = CHAR_SIZE;
		Mat transformMat = Mat.eye(2, 3, CV_32F).asMat();
		int m = Math.max(w, h);

		FloatIndexer idx = transformMat.createIndexer();
		idx.put(0, 2, (m - w) / 2f);
		idx.put(1, 2, (m - h) / 2f);

		Mat warpImage = new Mat(m, m, in.type());
		warpAffine(in, warpImage, transformMat, warpImage.size(), INTER_LINEAR, BORDER_CONSTANT, new Scalar(0));

		Mat out = new Mat();
		resize(warpImage, out, new Size(charSize, charSize));

		return out;

	}


	public int getLiuDingSize() {
		return m_LiuDingSize;
	}

	public void setLiuDingSize(int m_LiuDingSize) {
		this.m_LiuDingSize = m_LiuDingSize;
	}

	public int gettheMatWidth() {
		return m_theMatWidth;
	}

	public void settheMatWidth(int m_theMatWidth) {
		this.m_theMatWidth = m_theMatWidth;
	}

	public int getColorThreshold() {
		return m_ColorThreshold;
	}

	public void setColorThreshold(int m_ColorThreshold) {
		this.m_ColorThreshold = m_ColorThreshold;
	}

	public float getBluePercent() {
		return m_BluePercent;
	}

	public void setBluePercent(float m_BluePercent) {
		this.m_BluePercent = m_BluePercent;
	}

	public float getWhitePercent() {
		return m_WhitePercent;
	}

	public void setWhitePercent(float m_WhitePercent) {
		this.m_WhitePercent = m_WhitePercent;
	}

	public int getDebug() {
		return m_debug;
	}

	public void setDebug(int m_debug) {
		this.m_debug = m_debug;
	}
}
