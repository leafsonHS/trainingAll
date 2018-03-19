package com.crenjoy.easypr.core;

import static org.bytedeco.javacpp.opencv_core.BORDER_DEFAULT;
import static org.bytedeco.javacpp.opencv_core.CV_16S;
import static org.bytedeco.javacpp.opencv_core.addWeighted;
import static org.bytedeco.javacpp.opencv_core.convertScaleAbs;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.GaussianBlur;
import static org.bytedeco.javacpp.opencv_imgproc.MORPH_CLOSE;
import static org.bytedeco.javacpp.opencv_imgproc.MORPH_RECT;
import static org.bytedeco.javacpp.opencv_imgproc.Sobel;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.getStructuringElement;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;
import static org.bytedeco.javacpp.opencv_imgproc.morphologyEx;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;

import java.util.Vector;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_core.Size2f;

import com.crenjoy.easypr.enums.LocateType;
import com.crenjoy.easypr.enums.PlateColor;

/**
 * 通过Sobel基于垂直线条的车牌定位
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 * 
 */
public class PlateLocateSobel extends PlateLocate {

	static final int SOBEL_SCALE = 1;
	static final int SOBEL_DELTA = 0;
	static final int SOBEL_DDEPTH = CV_16S;
	static final int SOBEL_X_WEIGHT = 1;
	static final int SOBEL_Y_WEIGHT = 0; // 0 设置权重效果更好

	// ! PlateLocate所用常量
	static final int DEFAULT_GAUSSIANBLUR_SIZE = 5;
	static final int DEFAULT_MORPH_SIZE_WIDTH = 17; // 17
	static final int DEFAULT_MORPH_SIZE_HEIGHT = 3; // 3

	// ! 高斯模糊所用变量
	private int m_GaussianBlurSize = DEFAULT_GAUSSIANBLUR_SIZE;

	// ! 连接操作所用变量
	private int m_MorphSizeWidth = DEFAULT_MORPH_SIZE_WIDTH;
	private int m_MorphSizeHeight = DEFAULT_MORPH_SIZE_HEIGHT;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.core.PlateLocate#setLifemode(boolean)
	 */
	@Override
	public void setLifemode(boolean param) {
		super.setLifemode(param);
		if (param) {
			this.m_GaussianBlurSize = 5;
			this.m_MorphSizeWidth = 10;
			this.m_MorphSizeHeight = 3;
		} else {
			this.m_GaussianBlurSize = DEFAULT_GAUSSIANBLUR_SIZE;
			this.m_MorphSizeWidth = DEFAULT_MORPH_SIZE_WIDTH;
			this.m_MorphSizeHeight = DEFAULT_MORPH_SIZE_HEIGHT;
		}
	}

	/**
	 * Sobel定位法 基于垂直线条的车牌定位
	 * 
	 * @param src
	 * @return
	 */
	public Vector<Plate> locate(Mat src) {
		Vector<Plate> candPlates = new Vector<Plate>();

	//	Vector<RotatedRect> rects_sobel_sel = new Vector<RotatedRect>();
		Vector<Plate> plates = new Vector<Plate>();

		Vector<RotatedRect> bound_rects = new Vector<RotatedRect>();

		// Sobel第一次粗略搜索

		sobelFrtSearch(src, bound_rects);

//		PlateTestUtils.imwrite(src, "sobel_frt", bound_rects.toArray(new RotatedRect[0]));

		// Vector<Rect> bound_rects_part = new Vector<Rect>();
		// // 对不符合要求的区域进行扩展
		// for (Rect itemRect :bound_rects) {
		// float fRatio = itemRect.width() * 1.0f / itemRect.height();
		// if (fRatio < 3.0 && fRatio > 1.0 && itemRect.height() < 120) {
		// // 宽度过小，进行扩展
		// itemRect.x(Float.valueOf(itemRect.x() - itemRect.height() * (4 -
		// fRatio)).intValue());
		// if (itemRect.x() < 0) {
		// itemRect.x(0);
		// }
		// itemRect.width(Float.valueOf(itemRect.width() + itemRect.height() * 2
		// * (4 - fRatio)).intValue());
		// if ((itemRect.width() + itemRect.x()) >= src.cols()) {
		// itemRect.width(src.cols() - itemRect.x());
		// }
		//
		// itemRect.y(Float.valueOf(itemRect.y() - itemRect.height() *
		// 0.08f).intValue());
		// itemRect.height(Float.valueOf(itemRect.height() * 1.16f).intValue());
		//
		// bound_rects_part.add(itemRect);
		// }
		// }
		//
		// PlateTestUtils.imwriteRect(src, "sobel_frt_1", bound_rects_part);

		// 对断裂的部分进行二次处理
		// Vector<RotatedRect> rects_sobel = new Vector<RotatedRect>();
		// for (Rect bound_rect :bound_rects_part ) {
		// Point2f refpoint = new Point2f(bound_rect.x(), bound_rect.y());
		//
		// float x = bound_rect.x() > 0 ? bound_rect.x() : 0;
		// float y = bound_rect.y() > 0 ? bound_rect.y() : 0;
		//
		// float width = x + bound_rect.width() < src.cols() ?
		// bound_rect.width() : src.cols() - x;
		// float height = y + bound_rect.height() < src.rows() ?
		// bound_rect.height() : src.rows() - y;
		//
		// Rect safe_bound_rect = new Rect(Float.valueOf(x).intValue(),
		// Float.valueOf(y).intValue(), Float.valueOf(width).intValue(),
		// Float.valueOf(height)
		// .intValue());
		// Mat bound_mat = new Mat(src, safe_bound_rect);
		//
		// PlateTestUtils.imwrite(bound_mat, "sobel_test_1");
		// // Sobel第二次精细搜索(部分)
		// sobelSecSearchPart(bound_mat, refpoint, rects_sobel);
		// }
		// PlateTestUtils.imwrite(src, "sobel_test_1_1", rects_sobel);

		// for (Rect bound_rect : bound_rects ) {
		// Point2f refpoint = new Point2f(bound_rect.x(), bound_rect.y());
		//
		// float x = bound_rect.x() > 0 ? bound_rect.x() : 0;
		// float y = bound_rect.y() > 0 ? bound_rect.y() : 0;
		//
		// float width = x + bound_rect.width() < src.cols() ?
		// bound_rect.width() : src.cols() - x;
		// float height = y + bound_rect.height() < src.rows() ?
		// bound_rect.height() : src.rows() - y;
		//
		// Rect safe_bound_rect = new Rect(Float.valueOf(x).intValue(),
		// Float.valueOf(y).intValue(), Float.valueOf(width).intValue(),
		// Float.valueOf(height)
		// .intValue());
		//
		//
		//
		// Mat bound_mat = new Mat(src, safe_bound_rect);
		//
		// //PlateTestUtils.imwrite(bound_mat, "sobel_test_2");
		//
		// //PlateTestUtils.debug("x %f y %f", refpoint.x(),refpoint.y());
		// // Sobel第二次精细搜索
		// sobelSecSearch(bound_mat, refpoint, rects_sobel);
		// // sobelSecSearchPart(bound_mat, refpoint, rects_sobel);
		// }

		// PlateTestUtils.imwrite(src, "sobel_sec", rects_sobel);

		Mat src_b = sobelOper(src, 3, 10, 3);
		// 进行抗扭斜处理
		deskew(src, src_b, bound_rects, plates);
		
		for(Plate plate: plates){
			plate.setLocateType(LocateType.SOBEL);
			PlateColor plateColor = CoreFunc.getPlateType(plate.getPlateMat(), true);
			plate.setPlateColor(plateColor);
		}

//		PlateTestUtils.imwrite(plates, "sobel_plates");

		for (int i = 0; i < plates.size(); i++) {
			candPlates.add(plates.get(i));

		}

		return candPlates;
	}

	/**
	 * Sobel第一次搜索 不限制大小和形状，获取的BoundRect进入下一步
	 * 
	 * @param src
	 * @return
	 */
	public int sobelFrtSearch(final Mat src, Vector<RotatedRect> outRects) {

//		PlateTestUtils.imwrite(src, "sobel_oper_r");
		// soble操作，得到二值图像
		Mat src_threshold = sobelOper(src, m_GaussianBlurSize, m_MorphSizeWidth, m_MorphSizeHeight);

//		PlateTestUtils.imwrite(src_threshold, "sobel_oper");

		MatVector contours = new MatVector();

		findContours(src_threshold, contours, // a vector of contours
				CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE); // all pixels of each
															// contours

		// Vector<RotatedRect> first_rects = new Vector<RotatedRect>();
		for (int i = 0; i < contours.size(); i++) {
			Mat itc = contours.get(i);
			RotatedRect mr = minAreaRect(itc);
			if (verifySizes(mr)) {
				outRects.add(mr);
				// float area = mr.size().height() * mr.size().width();
				// float r = (float) mr.size().width() / (float)
				// mr.size().height();
				// if (r < 1) {
				// r = (float) mr.size().height() / (float) mr.size().width();
				// }
			}
		}

		// for (int i = 0; i < first_rects.size(); i++) {
		// RotatedRect roi_rect = first_rects.get(i);
		// Rect safeBoundRect = new Rect();
		// if (calcSafeRect(roi_rect, src, safeBoundRect)) {
		// outRects.add(safeBoundRect);
		// }
		// }
		return 0;
	}

	/**
	 * Sobel运算 输入彩色图像，输出二值化图像
	 * 
	 * @param in
	 * @param blurSize
	 * @param morphW
	 * @param morphH
	 * @return
	 */
	public Mat sobelOper(final Mat in, int blurSize, int morphW, int morphH) {
		Mat mat_blur = in.clone();
		GaussianBlur(in, mat_blur, new Size(blurSize, blurSize), 0, 0, BORDER_DEFAULT);

		Mat mat_gray = null;
		if (mat_blur.channels() == 3) {
			mat_gray = new Mat();
			cvtColor(mat_blur, mat_gray, CV_RGB2GRAY);
		} else {
			mat_gray = mat_blur;
		}

//		PlateTestUtils.imwrite(mat_gray, "mat_gray");

		int scale = SOBEL_SCALE;
		int delta = SOBEL_DELTA;
		int ddepth = SOBEL_DDEPTH;

		Mat grad_x = new Mat();
		Mat grad_y = new Mat();
		Mat abs_grad_x = new Mat();
		Mat abs_grad_y = new Mat();

		// 对X soble
		Sobel(mat_gray, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT);
		convertScaleAbs(grad_x, abs_grad_x);
//		PlateTestUtils.imwrite(abs_grad_x, "abs_grad_x");

		// 对Y soble
		Sobel(mat_gray, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT);
		convertScaleAbs(grad_y, abs_grad_y);
//		PlateTestUtils.imwrite(abs_grad_y, "abs_grad_y");
		// 在两个权值组合
		// 因为Y方向的权重是0，因此在此就不再计算Y方向的sobel了

		Mat grad = new Mat();
		addWeighted(abs_grad_x, SOBEL_X_WEIGHT, abs_grad_y, SOBEL_Y_WEIGHT, 0, grad);

//		PlateTestUtils.imwrite(grad, "grad");
		// 分割
		Mat mat_threshold = new Mat();
	//	double otsu_thresh_val =
		threshold(grad, mat_threshold, 0, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);

		// 腐蚀和膨胀

		Mat element = getStructuringElement(MORPH_RECT, new Size(morphW, morphH));
		Mat out = new Mat();
		morphologyEx(mat_threshold, out, MORPH_CLOSE, element);

//		PlateTestUtils.imwrite(out, "mat_threshold");
		return out;
	}

	/**
	 * Sobel第二次搜索,对断裂的部分进行再次的处理 对大小和形状做限制，生成参考坐标
	 * 
	 * @param bound
	 * @param refpoint
	 * @param outRects
	 * @return
	 */
	public int sobelSecSearchPart(Mat bound, Point2f refpoint, Vector<RotatedRect> outRects) {

		// /第二次参数比一次精细，但针对的是得到的外接矩阵之后的图像，再sobel得到二值图像
		Mat bound_threshold = sobelOperT(bound, 3, 15, 2);

		// //二值化去掉两边的边界

		// Mat mat_gray;
		// cvtColor(bound,mat_gray,CV_BGR2GRAY);

		// bound_threshold = mat_gray.clone();
		// //threshold(input_grey, img_threshold, 5, 255, CV_THRESH_OTSU +
		// / CV_THRESH_BINARY);
		// int w = mat_gray.cols;
		// int h = mat_gray.rows;
		// Mat tmp = mat_gray(Rect(w*0.15,h*0.2,w*0.6,h*0.6));
		// int threadHoldV = ThresholdOtsu(tmp);
		// threshold(mat_gray, bound_threshold,threadHoldV, 255,
		// CV_THRESH_BINARY);

		Mat tempBoundThread = bound_threshold.clone();
		// //
		CoreFunc.clearLiuDingOnly(tempBoundThread);

//		PlateTestUtils.imwrite(bound_threshold, "liudingonly");

		IntPointer posLeft = new IntPointer(new int[] { 0 });
		IntPointer posRight = new IntPointer(new int[] { 0 });

		if (CoreFunc.bFindLeftRightBound(tempBoundThread, posLeft, posRight)) {

			// 找到两个边界后进行连接修补处理

			if (posRight.get() != 0 && posLeft.get() != 0 && posLeft.get() < posRight.get()) {
				int posY = Float.valueOf((bound_threshold.rows() * 0.5f)).intValue();
				for (int i = posLeft.get() + (int) (bound_threshold.rows() * 0.1); i < posRight.get() - 4; i++) {
					bound_threshold.data().put(posY * bound_threshold.cols() + i, (byte) 255);
				}
			}

//			PlateTestUtils.imwrite(bound_threshold, "repaireimg1");

			// 两边的区域不要

			for (int i = 0; i < bound_threshold.rows(); i++) {
				bound_threshold.data().put(i * bound_threshold.cols() + posLeft.get(), (byte) 0);
				bound_threshold.data().put(i * bound_threshold.cols() + posRight.get(), (byte) 0);
			}

//			PlateTestUtils.imwrite(bound_threshold, "repaireimg2");
		}

		MatVector contours = new MatVector();
		findContours(bound_threshold, contours, // a vector of contours
				CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE); // all pixels of each
															// contours

		Vector<RotatedRect> second_rects = new Vector<RotatedRect>();

		for (int i = 0; i < contours.size(); i++) {
			Mat itc = contours.get(i);
			RotatedRect mr = minAreaRect(itc);
			second_rects.add(mr);
		}

		for (int i = 0; i < second_rects.size(); i++) {
			RotatedRect roi = second_rects.get(i);
			if (verifySizes(roi)) {
				Point2f refcenter = new Point2f(roi.center().x() + refpoint.x(), roi.center().y() + refpoint.y());
				Size2f size = roi.size();
				float angle = roi.angle();

				RotatedRect refroi = new RotatedRect(refcenter, size, angle);
				outRects.add(refroi);
			}
		}

		return 0;
	}

	/**
	 * Sobel第二次搜索 对大小和形状做限制，生成参考坐标
	 * 
	 * @param bound
	 * @param refpoint
	 * @param outRects
	 * @return
	 */
	public int sobelSecSearch(Mat bound, Point2f refpoint, Vector<RotatedRect> outRects) {

		// 第二次参数比一次精细，但针对的是得到的外接矩阵之后的图像，再sobel得到二值图像
		Mat bound_threshold = sobelOper(bound, 3, 15, 3);

//		PlateTestUtils.imwrite(bound_threshold, "sobelSecSearch");

		MatVector contours = new MatVector();
		findContours(bound_threshold, contours, // a vector of contours
				CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE); // all pixels of each
															// contours

		Vector<RotatedRect> second_rects = new Vector<RotatedRect>();

		for (int i = 0; i < contours.size(); i++) {
			Mat itc = contours.get(i);
			RotatedRect mr = minAreaRect(itc);
			second_rects.add(mr);
		}

		for (int i = 0; i < second_rects.size(); i++) {
			RotatedRect roi = second_rects.get(i);
			if (verifySizes(roi)) {
				Point2f refcenter = new Point2f(roi.center().x() + refpoint.x(), roi.center().y() + refpoint.y());
				Size2f size = roi.size();
				float angle = roi.angle();
				RotatedRect refroi = new RotatedRect(refcenter, size, angle);
				outRects.add(refroi);
			}
		}

		return 0;
	}

	/**
	 * Sobel运算 输入彩色图像，输出二值化图像
	 * 
	 * @param in
	 * @param blurSize
	 * @param morphW
	 * @param morphH
	 * @return
	 */
	public Mat sobelOperT(final Mat in, int blurSize, int morphW, int morphH) {
		Mat mat_blur = in.clone();
		GaussianBlur(in, mat_blur, new Size(blurSize, blurSize), 0, 0, BORDER_DEFAULT);

		Mat mat_gray = null;
		if (mat_blur.channels() == 3) {
			mat_gray = new Mat();
			cvtColor(mat_blur, mat_gray, CV_BGR2GRAY);
		} else {
			mat_gray = mat_blur;
		}

//		PlateTestUtils.imwrite(mat_gray, "grayblure");
		// equalizeHist(mat_gray, mat_gray);

		int scale = SOBEL_SCALE;
		int delta = SOBEL_DELTA;
		int ddepth = SOBEL_DDEPTH;

		Mat grad_x = new Mat();
		Mat grad_y = new Mat();
		Mat abs_grad_x = new Mat();
		Mat abs_grad_y = new Mat();

		Sobel(mat_gray, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT);
		convertScaleAbs(grad_x, abs_grad_x);

		// 因为Y方向的权重是0，因此在此就不再计算Y方向的sobel了
		Sobel(mat_gray, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT);
		convertScaleAbs(grad_y, abs_grad_y);

		Mat grad = new Mat();
		addWeighted(abs_grad_x, SOBEL_X_WEIGHT, abs_grad_y, SOBEL_Y_WEIGHT, 0, grad);

//		PlateTestUtils.imwrite(grad, "graygrad");

		Mat mat_threshold = new Mat();
		//double otsu_thresh_val = 
		threshold(grad, mat_threshold, 0, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);

//		PlateTestUtils.imwrite(mat_threshold, "grayBINARY");

		Mat element = getStructuringElement(MORPH_RECT, new Size(morphW, morphH));
		Mat out = new Mat();
		morphologyEx(mat_threshold, out, MORPH_CLOSE, element);

//		PlateTestUtils.imwrite(out, "phologyEx");
		return out;
	}

}
