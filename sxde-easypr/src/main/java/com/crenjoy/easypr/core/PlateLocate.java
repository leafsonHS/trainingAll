package com.crenjoy.easypr.core;

import static org.bytedeco.javacpp.opencv_core.BORDER_CONSTANT;
import static org.bytedeco.javacpp.opencv_core.CV_8UC3;
import static org.bytedeco.javacpp.opencv_core.addWeighted;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_AREA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_CUBIC;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY_INV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.INTER_AREA;
import static org.bytedeco.javacpp.opencv_imgproc.INTER_CUBIC;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.getAffineTransform;
import static org.bytedeco.javacpp.opencv_imgproc.getRectSubPix;
import static org.bytedeco.javacpp.opencv_imgproc.getRotationMatrix2D;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_core.Size2f;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;

import com.crenjoy.easypr.ObjectPointer;
import com.crenjoy.easypr.enums.PlateColor;

/**
 * 车牌定位 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * 
 * @author CGD
 * 
 */
public abstract class PlateLocate {

	private static final Log log = LogFactory.getLog(PlateLocate.class);

	/** 车牌标准尺寸 */
	static final int WIDTH = 136;
	static final int HEIGHT = 36;
	static final int TYPE = CV_8UC3;

	/** 角度判断所用常量 */
	static final int DEFAULT_ANGLE = 60; // 30
	/** 允许车牌比例误差 */
	static final float DEFAULT_ERROR = 0.6f; // 0.6
	/** 通常车牌比例 */
	static final float DEFAULT_ASPECT = 3.5f; // 3.75
	/** 最小的车牌 34 * 8 的倍数 */
	static final int DEFAULT_VERIFY_MIN = 2; // 3 车牌图片在小识别不了
	/** 最大的车牌 34 * 8 的倍数 */
	static final int DEFAULT_VERIFY_MAX = 60; // 40 车牌图片像素高，离的近

	/** 允许车牌比例误差 */
	private float m_error = DEFAULT_ERROR;
	/** 通常车牌比例 */
	private float m_aspect = DEFAULT_ASPECT;
	/** 最小的车牌 34 * 8 的倍数 */
	private int m_verifyMin = DEFAULT_VERIFY_MIN;
	/** 最大的车牌 34 * 8 的倍数 */
	private int m_verifyMax = DEFAULT_VERIFY_MAX;

	public abstract Vector<Plate> locate(Mat src);

	/**
	 * 生活模式与工业模式切换 如果为真，则设置各项参数为定位生活场景照片（如百度图片）的参数，否则恢复默认值。
	 * 
	 * @param param
	 */
	public void setLifemode(boolean param) {
		if (param) {
			// setGaussianBlurSize(5);
			// setMorphSizeWidth(10);
			// setMorphSizeHeight(3);
			this.m_error = 0.75f;
			this.m_aspect = 4.0f;
			this.m_verifyMin = 1;
			this.m_verifyMax = 200;
		} else {
			// setGaussianBlurSize(DEFAULT_GAUSSIANBLUR_SIZE);
			// setMorphSizeWidth(DEFAULT_MORPH_SIZE_WIDTH);
			// setMorphSizeHeight(DEFAULT_MORPH_SIZE_HEIGHT);
			this.m_error = DEFAULT_ERROR;
			this.m_aspect = DEFAULT_ASPECT;
			this.m_verifyMin = DEFAULT_VERIFY_MIN;
			this.m_verifyMax = DEFAULT_VERIFY_MAX;
		}
	}

	/**
	 * 车牌的尺寸验证 对minAreaRect获得的最小外接矩形，用纵横比进行判断
	 * 
	 * @param mr
	 * @return
	 */
	public boolean verifySizes(RotatedRect mr) {
		int min = 34 * 8 * m_verifyMin; // minimum area 2倍
		int max = 34 * 8 * m_verifyMax; // maximum area 60倍
		float area = mr.size().height() * mr.size().width();
		// 区域不匹配
		if ((area < min) || (area > max)) {
			return false;
		}

		float error = m_error;
		// Spain car plate size: 52x11 aspect 4,7272
		// Real car plate size: 136 * 32, aspect 3.5
		// China car plate size: 440mm*140mm，aspect 3.142857
		float aspect = m_aspect;

		// Get only patchs that match to a respect ratio.
		float rmin = aspect * (1.0f - error);
		float rmax = aspect * (1.0f + error);

		// 车牌比例
		float r = (float) mr.size().width() / (float) mr.size().height();
		// 车牌方向调整
		if (r < 1) {
			r = (float) mr.size().height() / (float) mr.size().width();
		}

		// 车牌比例是否合适
		return !((r < rmin) || (r > rmax));
	}

	/**
	 * 调整RotatedRect 为宽、高方向
	 * 
	 * @param src
	 * @return
	 */
	public RotatedRect adjustRotatedRect(RotatedRect src) {
		float r = (float) src.size().width() / (float) src.size().height();
		float roi_angle = src.angle();

		Size2f roi_rect_size = src.size();
		// 车牌需要旋转90度
		if (r < 1) {
			roi_angle = 90 + roi_angle;
			float swap = roi_rect_size.width();
			roi_rect_size.width(roi_rect_size.height());
			roi_rect_size.height(swap);
			src.size(roi_rect_size);
			src.angle(roi_angle);
		}
		return src;
	}

	/**
	 * 调整0-60度的车牌
	 * 
	 * @param src
	 * @param src_b
	 * @param roi_rect
	 * @return
	 */
	public Mat rotatedMat(final Mat src, final Mat src_b, RotatedRect roi_rect) {
		Mat deskew_mat = new Mat();
		float roi_angle = roi_rect.angle();
		Rect safeBoundRect = calcSafeRect(roi_rect.boundingRect(), src);
		if (null == safeBoundRect) {
			return deskew_mat;
		}

		Mat bound_mat = new Mat(src, safeBoundRect);
		Mat bound_mat_b = new Mat(src_b, safeBoundRect);

		Point2f roi_ref_center = new Point2f(roi_rect.center().x() - safeBoundRect.tl().x(), roi_rect.center().y() - safeBoundRect.tl().y());

		if ((roi_angle - 3 < 0 && roi_angle + 3 > 0) || 90.0 == roi_angle || -90.0 == roi_angle) {
			deskew_mat = bound_mat;
		} else {

			// 角度在5到60度之间的，首先需要旋转 rotation
			Mat rotated_mat = new Mat();
			if (!rotation(bound_mat, rotated_mat, roi_rect.size(), roi_ref_center, roi_angle)) {
				return deskew_mat;
			}
			Mat rotated_mat_b = new Mat();
			if (!rotation(bound_mat_b, rotated_mat_b, roi_rect.size(), roi_ref_center, roi_angle)) {
				return deskew_mat;
			}

			// 如果图片偏斜，还需要视角转换 affine
			FloatPointer roi_slope = new FloatPointer(0f);
			// imshow("1roated_mat",rotated_mat);
			// imshow("rotated_mat_b",rotated_mat_b);
			if (isdeflection(rotated_mat_b, roi_angle, roi_slope)) {
				affine(rotated_mat, deskew_mat, roi_slope.get());
			} else {
				deskew_mat = rotated_mat;
			}
		}
		return deskew_mat;
	}

	/**
	 * 抗扭斜处理
	 * 
	 * @param src
	 * @param src_b
	 * @param inRects
	 * @param outPlates
	 * @return
	 */
	public int deskew(final Mat src, final Mat src_b, Vector<RotatedRect> inRects, Vector<Plate> outPlates) {
		Mat mat_debug = new Mat();
		src.copyTo(mat_debug);
		for (int i = 0; i < inRects.size(); i++) {
			// 调整角度
			RotatedRect roi_rect = adjustRotatedRect(inRects.get(i));

			// 标记出车牌位置
			// PlateTestUtils.markMat(mat_debug, roi_rect, "bj");

			Mat deskew_mat = null;
			// 调整角度为60度以上的无法调整 DEFAULT_ANGLE=60
			float roi_angle = roi_rect.angle();
			if (roi_angle - DEFAULT_ANGLE < 0 && roi_angle + DEFAULT_ANGLE > 0) {
				deskew_mat = rotatedMat(src, src_b, roi_rect);
			}

			if (null == deskew_mat || null== deskew_mat.data() ||deskew_mat.data().isNull()) {
				continue;
			}
		
			// haitungaga添加，删除非区域，这个函数影响了25%的完整定位率
			deskew_mat = deleteNotArea(deskew_mat);
			// 这里对deskew_mat进行了一个筛选
			// 使用了经验数值： 车牌宽高比例 2.3-6
			double r = deskew_mat.cols() * 1.0 / deskew_mat.rows();
			if (r < 2.3 || r > 6) {
				continue;
			}

			Mat plate_mat = new Mat();
			plate_mat.create(HEIGHT, WIDTH, TYPE);
			// 如果图像大于我们所要求的图像，对图像进行一个大小变更
			if (deskew_mat.cols() >= WIDTH || deskew_mat.rows() >= HEIGHT) {
				resize(deskew_mat, plate_mat, plate_mat.size(), 0, 0, INTER_AREA);
			} else {
				resize(deskew_mat, plate_mat, plate_mat.size(), 0, 0, INTER_CUBIC);
			}
			Plate plate = new Plate();
			plate.setPlatePos(roi_rect);
			plate.setPlateMat(plate_mat);
			outPlates.add(plate);
		}
		return 0;
	}

	/**
	 * 删除非区域
	 * 
	 * @param inmat
	 */
	Mat deleteNotArea(Mat inmat) {

		int w = inmat.cols();
		int h = inmat.rows();

		// 判断车牌颜色以此确认threshold方法
		
		Rect colorRect = calcSafeRect(new Rect((int) (w * 0.15), (int) (h * 0.1), (int) (w * 0.7), (int) (h * 0.7)), inmat);
		PlateColor plateType = null;
		if (null!=colorRect){
			Mat tmpMat = new Mat(inmat, colorRect);
			plateType=CoreFunc.getPlateType(tmpMat, true);
		}else{
			plateType=CoreFunc.getPlateType(inmat, true);
		}
				

		Mat input_grey = new Mat();
		cvtColor(inmat, input_grey, CV_BGR2GRAY);
		Mat img_threshold = null;
		if (PlateColor.BLUE.equals(plateType)) {
			img_threshold = input_grey.clone();
			Mat tmp = new Mat(input_grey, new Rect((int) (w * 0.15), (int) (h * 0.15), (int) (w * 0.7), (int) (h * 0.7)));
			int threadHoldV = CoreFunc.ThresholdOtsu(tmp);

			threshold(input_grey, img_threshold, threadHoldV, 255, CV_THRESH_BINARY);
			// threshold(input_grey, img_threshold, 5, 255, CV_THRESH_OTSU +
			// CV_THRESH_BINARY);
			// PlateTestUtils.imwrite(img_threshold, "inputgray2_blue");

		} else if (PlateColor.YELLOW.equals(plateType)) {
			img_threshold = input_grey.clone();

			Mat tmp = new Mat(input_grey, new Rect((int) (w * 0.1), (int) (h * 0.1), (int) (w * 0.8), (int) (h * 0.8)));

			int threadHoldV = CoreFunc.ThresholdOtsu(tmp);

			threshold(input_grey, img_threshold, threadHoldV, 255, CV_THRESH_BINARY_INV);

			// PlateTestUtils.imwrite(img_threshold, "inputgray2_yellow");
			// threshold(input_grey, img_threshold, 10, 255, CV_THRESH_OTSU +
			// CV_THRESH_BINARY_INV);
		} else {
			img_threshold = input_grey.clone();
			threshold(input_grey, img_threshold, 10, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);
		}
		int[] border = CoreFunc.clearLiuDingBorder(img_threshold);
		int top = border[0];
		int bottom = border[1];
		// log.debug(String.format("top %d bottom %d", border[0],border[1]));
		ObjectPointer<Integer> posLeft = new ObjectPointer<Integer>(0);
		ObjectPointer<Integer> posRight = new ObjectPointer<Integer>(0);

		if (CoreFunc.bFindLeftRightBound1(img_threshold, posLeft, posRight)) {

			if ((inmat.cols() - posLeft.get())<=0 || (bottom - top)<=0){
				return inmat;
			}
			Rect rect=new Rect((int)posLeft.get(), (int)top, (int)(inmat.cols() - posLeft.get()),(int) (bottom - top));
			Rect bolderRect = calcSafeRect(rect, inmat);
			if (null != bolderRect) {
				inmat = new Mat(inmat, bolderRect);
			}
		}
		// PlateTestUtils.imwrite(inmat, "inputgray2_ok");
		return inmat;
	}

	/**
	 * 旋转操作
	 * 
	 * @param in
	 * @param out
	 * @param rect_size
	 * @param center
	 * @param angle
	 * @return
	 */
	public boolean rotation(final Mat in, Mat out, final Size2f rect_size, final Point2f center, final float angle) {
		Mat in_large = new Mat();
		in_large.create((int) (in.rows() * 1.5), (int) (in.cols() * 1.5), in.type());

		float x = in_large.cols() / 2 - center.x() > 0 ? in_large.cols() / 2 - center.x() : 0;
		float y = in_large.rows() / 2 - center.y() > 0 ? in_large.rows() / 2 - center.y() : 0;

		float width = x + in.cols() < in_large.cols() ? in.cols() : in_large.cols() - x;
		float height = y + in.rows() < in_large.rows() ? in.rows() : in_large.rows() - y;

		/*
		 * assert(width == in.cols); assert(height == in.rows);
		 */
		if (((int)width != in.cols()) || ((int)height != in.rows())) {
			try {
				in_large.close();
			} catch (Exception ex) {

			}
			return false;
		}

		Mat imageRoi = new Mat(in_large, new Rect(Float.valueOf(x).intValue(), Float.valueOf(y).intValue(), Float.valueOf(width).intValue(), Float.valueOf(
				height).intValue()));
		addWeighted(imageRoi, 0, in, 1, 0, imageRoi);

		// Point2f center_diff = new Point2f(in.cols() / 2.f, in.rows() / 2.f);
		Point2f new_center = new Point2f(in_large.cols() / 2.f, in_large.rows() / 2.f);

		Mat rot_mat = getRotationMatrix2D(new_center, angle, 1);

		/*
		 * imshow("in_copy", in_large); waitKey(0);
		 */

		Mat mat_rotated = new Mat();
		warpAffine(in_large, mat_rotated, rot_mat, new Size(in_large.cols(), in_large.rows()), CV_INTER_CUBIC, BORDER_CONSTANT, new Scalar());

		/*
		 * imshow("mat_rotated", mat_rotated); waitKey(0);
		 */

		getRectSubPix(mat_rotated, new Size(Float.valueOf(rect_size.width()).intValue(), Float.valueOf(rect_size.height()).intValue()), new_center, out, -1);

		/*
		 * imshow("img_crop", img_crop); waitKey(0);
		 */

		return true;
	}

	/**
	 * 是否偏斜 输入二值化图像，输出判断结果
	 * 
	 * @param in
	 * @param angle
	 * @param slope
	 * @return
	 */
	public boolean isdeflection(final Mat in, final float angle, FloatPointer slope) {
		int nRows = in.rows();
		int nCols = in.cols();

		if (in.channels() != 1) {
			log.error("Error ");
		}

		int[] comp_index = new int[3];
		int[] len = new int[3];

		comp_index[0] = nRows / 4;
		comp_index[1] = nRows / 4 * 2;
		comp_index[2] = nRows / 4 * 3;

		UByteRawIndexer bi = in.createIndexer();

		for (int i = 0; i < 3; i++) {
			int index = comp_index[i];
			int j = 0;
			int value = 0;
			while (0 == value && j < nCols) {
				value = (int) (bi.get(index, j++));
			}
			len[i] = j;
		}

		// cout << "len[0]:" << len[0] << endl;
		// cout << "len[1]:" << len[1] << endl;
		// cout << "len[2]:" << len[2] << endl;

		// len[0]/len[1]/len[2]这三个应该是取车牌边线的值，来计算车牌边线的斜率
		double maxlen = Math.max(len[2], len[0]);
		double minlen = Math.min(len[2], len[0]);
		// double difflen = Math.abs(len[2] - len[0]);

		// angle是根据水平那根直线的斜率转换过来的角度
		double g = Math.tan(angle * Math.PI / 180.0);

		if (maxlen - len[1] > nCols / 32 || len[1] - minlen > nCols / 32) {

			// 如果斜率为正，则底部在下，反之在上
			// 求直线的斜率
			float slope_can_1 = (float) (len[2] - len[0]) / (float) (comp_index[1]);
			float slope_can_2 = (float) (len[1] - len[0]) / (float) (comp_index[0]);
			// float slope_can_3 = (float) (len[2] - len[1]) / (float)
			// (comp_index[0]);
			// cout<<"angle:"<<angle<<endl;
			// cout<<"g:"<<g<<endl;
			// cout << "slope_can_1:" << slope_can_1 << endl;
			// cout << "slope_can_2:" << slope_can_2 << endl;
			// cout << "slope_can_3:" << slope_can_3 << endl;
			// if(g>=0)
			slope.put(0, (Math.abs(slope_can_1 - g) <= Math.abs(slope_can_2 - g)) ? slope_can_1 : slope_can_2);
			// cout << "slope:" << slope << endl;
			return true;
		} else {
			slope.put(0, 0f);
		}

		return false;
	}

	/**
	 * 计算一个安全的Rect 如果不存在，返回null
	 * 
	 * @param roi_rect
	 * @param src
	 * @return
	 */
	public static Rect calcSafeRect(final Rect boudRect, final Mat src) {
		// boudRect的左上的x和y有可能小于0
		int tl_x = (boudRect.x() > 0) ? boudRect.x() : 0;
		int tl_y = (boudRect.y() > 0) ? boudRect.y() : 0;
		// boudRect的右下的x和y有可能大于src的范围
		float br_x = (boudRect.x() + boudRect.width()) < src.cols() ? (boudRect.x() + boudRect.width() - 1): (src.cols() - 1);
		float br_y = (boudRect.y() + boudRect.height()) < src.rows() ? (boudRect.y() + boudRect.height() - 1):(src.rows() - 1);

		float roi_width = br_x - tl_x;
		float roi_height = br_y - tl_y;
		if (roi_width <= 0 || roi_height <= 0) {
			return null;
		}
		// 新建一个mat，确保地址不越界，以防mat定位roi时抛异常
		return new Rect((int) tl_x, (int) tl_y, (int) roi_width, (int) roi_height);
	}

	/**
	 * 扭变操作,通过opencv的仿射变换
	 * 
	 * @param in
	 * @param out
	 * @param slope
	 */
	public void affine(final Mat in, Mat out, final float slope) {
		// imshow("in", in);
		// waitKey(0);

		// 这里的slope是通过判断是否倾斜得出来的倾斜率
		Point2f[] dstTri = new Point2f[3];
		Point2f[] plTri = new Point2f[3];

		float height = (float) in.rows();
		float width = (float) in.cols();
		float xiff = (float) Math.abs(slope) * height;

		if (slope > 0) {

			// 右偏型，新起点坐标系在xiff/2位置

			plTri[0] = new Point2f(0, 0);
			plTri[1] = new Point2f(width - xiff - 1, 0);
			plTri[2] = new Point2f(0 + xiff, height - 1);

			dstTri[0] = new Point2f(xiff / 2, 0);
			dstTri[1] = new Point2f(width - 1 - xiff / 2, 0);
			dstTri[2] = new Point2f(xiff / 2, height - 1);
		} else {

			// 左偏型，新起点坐标系在 -xiff/2位置

			plTri[0] = new Point2f(0 + xiff, 0);
			plTri[1] = new Point2f(width - 1, 0);
			plTri[2] = new Point2f(0, height - 1);

			dstTri[0] = new Point2f(xiff / 2, 0);
			dstTri[1] = new Point2f(width - 1 - xiff + xiff / 2, 0);
			dstTri[2] = new Point2f(xiff / 2, height - 1);
		}

		Mat warp_mat = getAffineTransform(plTri[0], dstTri[0]);

		Mat affine_mat = out;
		affine_mat.create((int) height, (int) width, TYPE);

		if (in.rows() > HEIGHT || in.cols() > WIDTH) {

			// 仿射变换

			warpAffine(in, affine_mat, warp_mat, affine_mat.size(), CV_INTER_AREA, BORDER_CONSTANT, new Scalar());
		} else {
			warpAffine(in, affine_mat, warp_mat, affine_mat.size(), CV_INTER_CUBIC, BORDER_CONSTANT, new Scalar());
		}
	}

}
