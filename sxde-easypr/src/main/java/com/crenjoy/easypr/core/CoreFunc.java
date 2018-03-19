package com.crenjoy.easypr.core;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_core.countNonZero;
import static org.bytedeco.javacpp.opencv_core.merge;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import static org.bytedeco.javacpp.opencv_core.split;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_HSV2BGR;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.annotation.ByRef;
import org.bytedeco.javacpp.indexer.FloatBufferIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;

import com.crenjoy.easypr.ObjectPointer;
import com.crenjoy.easypr.enums.PlateColor;

/**
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public class CoreFunc {
	
//	private static Log log=LogFactory.getLog(CoreFunc.class);

	/**
	 * 根据一幅图像与颜色模板获取对应的二值图
	 * 
	 * @param src
	 *            输入RGB图像
	 * @param r
	 *            颜色模板（蓝色、黄色）
	 * @param adaptive_minsv
	 *            默认为false
	 *            <ul>
	 *            <li>S和V的最小值由adaptive_minsv这个bool值判断</li>
	 *            <li>如果为true，则最小值取决于H值，按比例衰减</li>
	 *            <li>如果为false,则不再自适应，使用固定的最小值minabs_sv</li>
	 *            </ul>
	 * @return 输出灰度图（只有0和255两个值，255代表匹配，0代表不匹配）
	 */
	public static Mat colorMatch(final Mat src, final PlateColor r, final boolean adaptive_minsv) {
		final int max_sv = 255;
		final int minref_sv = 64;//64
		final int minabs_sv = 95;//95

		Mat src_hsv = new Mat();

		// 转到HSV空间进行处理，颜色搜索主要使用的是H分量进行蓝色与黄色的匹配工作
		cvtColor(src, src_hsv, CV_BGR2HSV);

		MatVector hsvSplit = new MatVector();
		split(src_hsv, hsvSplit);
		equalizeHist(hsvSplit.get(2), hsvSplit.get(2));
		merge(hsvSplit, src_hsv);
		
		//PlateTestUtils.imwrite(src_hsv, "colorMatch_s");
		
		// 匹配模板基色,切换以查找想要的基色
		int min_h = r.getMinH();
		int max_h = r.getMaxH();

		int channels = src_hsv.channels();
		int nRows = src_hsv.rows();

		// 图像数据列需要考虑通道数的影响；
		int nCols = src_hsv.cols() * channels;
		// 连续存储的数据，按一行处理
		if (src_hsv.isContinuous()) {
			nCols = nCols * nRows;
			nRows = 1;
		}

		for (int i = 0; i < nRows; ++i) {
			BytePointer p = src_hsv.ptr(i);
			for (int j = 0; j < nCols; j += 3) {
				int H = p.get(j) & 0xFF; // 0-180
				int S = p.get(j + 1) & 0xFF; // 0-255
				int V = p.get(j + 2) & 0xFF; // 0-255
				
				boolean colorMatched = false;

				if (H > min_h && H < max_h) {
					// adaptive_minsv==true S和V的最小值动态调节
					int min_sv = adaptive_minsv?adaptiveMinsv(H,min_h,max_h,minref_sv) :minabs_sv;
					if ((S > min_sv && S < max_sv) && (V > min_sv && V < max_sv)) {
						colorMatched = true;
					}
				}

				if (colorMatched == true) {
					p.put(j, (byte) 0);
					p.put(j + 1, (byte) 0);
					p.put(j + 2, (byte) 255);
				} else {
					p.put(j, (byte) 0);
					p.put(j + 1, (byte) 0);
					p.put(j + 2, (byte) 0);
				}
			}
		}
		
		// 获取颜色匹配后的二值灰度图
		MatVector hsvSplit_done = new MatVector();
		split(src_hsv, hsvSplit_done);
		Mat src_grey = hsvSplit_done.get(2);
		
		//PlateTestUtils.imwrite(src_hsv, "colorMatch_e");
		
		return src_grey;
	}
	
	/**
	 * S和V的最小值由adaptive_minsv这个bool值判断
	 * 如果为true，则最小值取决于H值，按比例衰减
	 * 如果为false，则不再自适应，使用固定的最小值minabs_sv
	 * @param H
	 * @param min_h
	 * @param max_h
	 * @param minref_sv
	 * @return
	 */
	private static int adaptiveMinsv(int H,int min_h,int max_h,int minref_sv){	
		int min_sv = 0;
		
		float diff_h = (max_h - min_h) / 2.0f;
		float avg_h = min_h + diff_h;
		
		float Hdiff = 0;
		if (H > avg_h) {
			Hdiff = H - avg_h;
		} else {
			Hdiff = avg_h - H;
		}

		float Hdiff_p = Hdiff * 1.0f / diff_h;


		min_sv = Float.valueOf(minref_sv - minref_sv / 2 * (1 - Hdiff_p)).intValue(); 
	    return min_sv;
	}

	/**
	 * 判断一个车牌的颜色
	 * 
	 * @param src
	 *            车牌mat
	 * @param r
	 *            颜色模板
	 * @param adaptive_minsv
	 *            S和V的最小值由adaptive_minsv这个bool值判断
	 *            <ul>
	 *            <li>如果为true，则最小值取决于H值，按比例衰减
	 *            <li>如果为false，则不再自适应，使用固定的最小值minabs_sv
	 *            </ul>
	 * @return
	 */
	public static float plateColorJudge(final Mat src, final PlateColor r, final boolean adaptive_minsv) {
		// 判断阈值
		// final float thresh = 0.45f;
		Mat gray = colorMatch(src, r, adaptive_minsv);
		float percent = (float) countNonZero(gray) / (gray.rows() * gray.cols());
		// return (percent > thresh);
		return percent;
	}

	/**
	 * getPlateType 判断车牌的类型
	 * 
	 * @param src
	 * @param adaptive_minsv
	 *            S和V的最小值由adaptive_minsv这个bool值判断
	 *            <ul>
	 *            <li>如果为true，则最小值取决于H值，按比例衰减
	 *            <li>如果为false，则不再自适应，使用固定的最小值minabs_sv
	 *            </ul>
	 * @return
	 */
	public static PlateColor getPlateType(final Mat src, final boolean adaptive_minsv) {
		final float thresh = 0.45f;
		float blue_percent = plateColorJudge(src, PlateColor.BLUE, adaptive_minsv);
		if (Float.compare(blue_percent, thresh) > 0) {
			return PlateColor.BLUE;
		}
		float yellow_percent = plateColorJudge(src, PlateColor.YELLOW, adaptive_minsv);
		if (Float.compare(yellow_percent, thresh) > 0) {
			return PlateColor.YELLOW;
		}
		float white_percent = plateColorJudge(src, PlateColor.WHITE, adaptive_minsv);
		if (Float.compare(white_percent, thresh) > 0) {
			return PlateColor.WHITE;
		}
		// 如果任意一者都不大于阈值，则取值最大者。
		PlateColor max_color = null;

		max_color = Float.compare(blue_percent, yellow_percent) > 0 ? PlateColor.BLUE : PlateColor.YELLOW;
		max_color = Float.compare(Math.max(blue_percent, yellow_percent), white_percent) > 0 ? max_color : PlateColor.WHITE;

		return max_color;
	}

	public static boolean bFindLeftRightBound(Mat bound_threshold, @ByRef IntPointer posLeft, @ByRef IntPointer posRight) {
		// 从两边寻找边界
		float span = bound_threshold.rows() * 0.2f;

		// 左边界检测
		for (int i = 0; i < bound_threshold.cols() - span - 1; i += 2) {
			int whiteCount = 0;
			for (int k = 0; k < bound_threshold.rows(); k++) {
				for (int l = i; l < i + span; l++) {
					int data= bound_threshold.data().get(k * bound_threshold.step(0) + l) & 0xFF;
					if (data == 255) {
						whiteCount++;
					}
				}
			}
			
//			PlateTestUtils.debug("whiteCount :%d  rows: %d ",whiteCount,bound_threshold.rows());
			
			if (whiteCount * 1.0 / (span * bound_threshold.rows()) > 0.36) {
				posLeft.put(0, i);
				break;
			}
		}
		span = bound_threshold.rows() * 0.2f;

		// 右边界检测
		for (int i = bound_threshold.cols() - 1; i > span; i -= 2) {
			int whiteCount = 0;
			for (int k = 0; k < bound_threshold.rows(); k++) {
				for (int l = i; l > i - span; l--) {
					int data= bound_threshold.data().get(k * bound_threshold.step(0) + l) & 0xFF;
					if (data == 255) {
						whiteCount++;
					}
				}
			}
			
		//	PlateTestUtils.debug("whiteCount :%d  rows: %d ",whiteCount,bound_threshold.rows());

			if (whiteCount * 1.0 / (span * bound_threshold.rows()) > 0.26) {
				posRight.put(0, i);
				break;
			}
		}
		
//		PlateTestUtils.debug("left %d right %d", posLeft.get(),posRight.get());

		return (posLeft.get() < posRight.get());
	}

	/**
	 * 车牌左右两边边界
	 * @param bound_threshold
	 * @param posLeft
	 * @param posRight
	 * @return
	 */
	public static boolean bFindLeftRightBound1(Mat bound_threshold, @ByRef ObjectPointer<Integer> posLeft, @ByRef ObjectPointer<Integer> posRight) {
		// 从两边寻找边界
		float span = bound_threshold.rows() * 0.2f;
		// 左边界检测
		for (int i = 0; i < bound_threshold.cols() - span - 1; i += 3) {
			int whiteCount = 0;
			for (int k = 0; k < bound_threshold.rows(); k++) {
				for (int j = i; j < i + span; j++) {
					
					int data= bound_threshold.data().get(k * bound_threshold.step(0) + j) & 0xFF;
					
					if (data  == 255) {
						whiteCount++;
					}
				}
			}
			
			if (whiteCount * 1.0 / (span * bound_threshold.rows()) > 0.13) { //0.15 晋字截掉一半
				posLeft.put(0, i);
				break;
			}
		}
		

		// 右边界检测
		span = bound_threshold.rows() * 0.2f;
		for (int i = bound_threshold.cols() - 1; i > span; i -= 2) {
			int whiteCount = 0;
			for (int k = 0; k < bound_threshold.rows(); k++) {
				for (int j = i; j > i - span; j--) {
					int data= bound_threshold.data().get(k * bound_threshold.step(0) + j) & 0xFF;
					if (data == 255) {
						whiteCount++;
					}
				}
			}
			

			if (whiteCount * 1.0 / (span * bound_threshold.rows()) > 0.06) {
				posRight.put(0,i);
				if ((posRight.get() + 5) < bound_threshold.cols()) {
					posRight.put(0,posRight.get()+5);
				} else {
					posRight.put(0,bound_threshold.cols()-1);
				}

				break;
			}
		}

		return (posLeft.get() < posRight.get());
	}

	public static boolean bFindLeftRightBound2(Mat bound_threshold, @ByRef IntPointer posLeft, @ByRef IntPointer posRight) {
		// 从两边寻找边界
		float span = bound_threshold.rows() * 0.2f;

		// 左边界检测
		for (int i = 0; i < bound_threshold.cols() - span - 1; i += 3) {
			int whiteCount = 0;
			for (int k = 0; k < bound_threshold.rows(); k++) {
				for (int l = i; l < i + span; l++) {
					int data= bound_threshold.data().get(k * bound_threshold.step(0) + l) & 0xFF;
					if (data == 255) {
						whiteCount++;
					}
				}
			}
			if (whiteCount * 1.0 / (span * bound_threshold.rows()) > 0.32) {
				posLeft.put(0, i);
				break;
			}
		}
		span = bound_threshold.rows() * 0.2f;

		// 右边界检测
		for (int i = bound_threshold.cols() - 1; i > span; i -= 3) {
			int whiteCount = 0;
			for (int k = 0; k < bound_threshold.rows(); k++) {
				for (int l = i; l > i - span; l--) {
					int data= bound_threshold.data().get(k * bound_threshold.step(0) + l) & 0xFF;
					if (data == 255) {
						whiteCount++;
					}
				}
			}

			if (whiteCount * 1.0 / (span * bound_threshold.rows()) > 0.22) {
				posRight.put(0,i);
				break;
			}
		}
		return (posLeft.get() < posRight.get());
	}

	/**
	 * 去除车牌上方的柳丁 计算每行元素的阶跃数，如果小于X认为是柳丁，将此行全部填0（涂黑） X的推荐值为，可根据实际调整
	 * 
	 * @param img
	 * @return
	 */
	public static boolean clearLiuDing(Mat img) {
		List<Float> fJump = new ArrayList<Float>();
		int whiteCount = 0;
		final int x = 7;
		Mat jump = Mat.zeros(1, img.rows(), CV_32F).asMat();
		FloatIndexer idx = jump.createIndexer();
		for (int i = 0; i < img.rows(); i++) {
			int jumpCount = 0;
			for (int j = 0; j < img.cols() - 1; j++) {
				if ((img.ptr(i, j).get() & 0xFF) != (img.ptr(i, j + 1).get() & 0xFF)) {
					jumpCount++;
				}
				if ((img.ptr(i, j).get() & 0xFF) == 255) {
					whiteCount++;
				}
			}
			idx.put(0, i, (float) jumpCount);
		}

		int iCount = 0;
		for (int i = 0; i < img.rows(); i++) {
			fJump.add(idx.get(0, i));
			if (idx.get(0, i) >= 16 && idx.get(0, i) <= 45) {
				// 车牌字符满足一定跳变条件
				iCount++;
			}
		}
		// //这样的不是车牌
		if (iCount * 1.0 / img.rows() <= 0.40) {
			// 满足条件的跳变的行数也要在一定的阈值内
			return false;
		}

		// 不满足车牌的条件
		if (whiteCount * 1.0 / (img.rows() * img.cols()) < 0.15 || whiteCount * 1.0 / (img.rows() * img.cols()) > 0.50) {
			return false;
		}

		UByteRawIndexer imgIdx = img.createIndexer();
		for (int i = 0; i < img.rows(); i++) {
			if (idx.get(0, i) <= x) {
				for (int j = 0; j < img.cols(); j++) {
					imgIdx.put(i, j,  0);
				}
			}
		}
		return true;
	}

	public static void clearLiuDingOnly(Mat img) {
		final int x = 7;
		Mat jump = Mat.zeros(1, img.rows(), CV_32F).asMat();
		FloatBufferIndexer idx = jump.createIndexer();
		for (int i = 0; i < img.rows(); i++) {
			float jumpCount = 0;
//			int whiteCount = 0;

			for (int j = 0; j < img.cols() - 1; j++) {
				if ((img.ptr(i, j).get() & 0xFF) != (img.ptr(i, j + 1).get() & 0xFF)) {
					jumpCount++;
				}

//				if ((img.ptr(i, j).get() & 0xFF) == 255) {
//					whiteCount++;
//				}
			}
			idx.put(0, i, jumpCount);
		}

		UByteRawIndexer imgIdx = img.createIndexer();
		for (int i = 0; i < img.rows(); i++) {
			if ((int)idx.get(0, i) <= x) {
				for (int j = 0; j < img.cols(); j++) {
					imgIdx.put(i, j, 0);
				}
			}
		}
	}

	/**
	 * 柳丁,上下边距
	 * 
	 * @param mask
	 * @return 数组
	 */
	public static int[] clearLiuDingBorder(Mat mask) {
		final int x = 7;
		int top = 0;
		int bottom = mask.rows() - 1;

		for (int i = 0; i < mask.rows() / 2; i++) {
			int whiteCount = 0;
			int jumpCount = 0;
			for (int j = 0; j < mask.cols() - 1; j++) {
				if ((mask.ptr(i, j).get() & 0xFF) != (mask.ptr(i, j + 1).get() & 0xFF)) {
					jumpCount++;
				}

				if ((mask.ptr(i, j).get() & 0xFF) == 255) {
					whiteCount++;
				}
			}
			if ((jumpCount < x && whiteCount * 1.0 / mask.cols() > 0.15) || whiteCount < 4) {
				top = i;
			}
		}
		top -= 1;
		if (top < 0) {
			top = 0;
		}

		// ok,找到上下边界
		for (int i = mask.rows() - 1; i >= mask.rows() / 2; i--) {
			int jumpCount = 0;
			int whiteCount = 0;
			for (int j = 0; j < mask.cols() - 1; j++) {
				if ((mask.ptr(i, j).get() & 0xFF) != (mask.ptr(i, j + 1).get() & 0xFF)) {
					jumpCount++;
				}
				if ((mask.ptr(i, j).get() & 0xFF) == 255) {
					whiteCount++;
				}
			}
			if ((jumpCount < x && whiteCount * 1.0 / mask.cols() > 0.15) || whiteCount < 4) {
				bottom = i;
			}
		}
		bottom += 1;
		if (bottom >= mask.rows()) {
			bottom = mask.rows() - 1;
		}

		if (top >= bottom) {
			top = 0;
			bottom = mask.rows() - 1;
		}
		return new int[] { top, bottom };
	}

	/**
	 * 直方图均衡
	 * 
	 * @param in
	 * @return
	 */
	public static Mat histeq(Mat in) {
		Mat out = new Mat(in.size(), in.type());
		if (in.channels() == 3) {
			Mat hsv = new Mat();
			MatVector hsvSplit = new MatVector();
			cvtColor(in, hsv, CV_BGR2HSV);
			split(hsv, hsvSplit);
			equalizeHist(hsvSplit.get(2), hsvSplit.get(2));
			merge(hsvSplit, hsv);
			cvtColor(hsv, out, CV_HSV2BGR);
		} else if (in.channels() == 1) {
			equalizeHist(in, out);
		}
		return out;
	}

	public static int ThresholdOtsu(Mat mat) {
		int height = mat.rows();
		int width = mat.cols();
		// histogram
		float[] histogram = new float[256];
		for (int i = 0; i < 256; i++) {
			histogram[i] = 0;
		}

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int p = mat.data().get(i * mat.step(0) + j) & 0xFF;
				histogram[p]++;
			}
		}

		// normalize histogram
		int size = height * width;
		for (int i = 0; i < 256; i++) {
			histogram[i] = histogram[i] / size;
		}

		// average pixel value
		float avgValue = 0;
		for (int i = 0; i < 256; i++) {
			avgValue += i * histogram[i];
		}

		int thresholdV = 0;
		float maxVariance = 0;
		float w = 0, u = 0;
		for (int i = 0; i < 256; i++) {
			w += histogram[i];
			u += i * histogram[i];

			float t = avgValue * w - u;
			float variance = t * t / (w * (1 - w));
			if (variance > maxVariance) {
				maxVariance = variance;
				thresholdV = i;
			}
		}

		return thresholdV;
	}

	public static Mat CutTheRect(Mat in, Rect rect) {
		int size = in.cols();
		// (rect.width>rect.height)?rect.width:rect.height;
		Mat dstMat = new Mat(size, size, CV_8UC1);
		dstMat.put(new Scalar(0d,0d,0d,0d));

		
	     //  dstMat=dstMat.setTo(d);
		//PlateTestUtils.debug("setto");

		int x = (size - rect.width()) / 2;
		int y = (size - rect.height()) / 2;

		// 把rect中的数据 考取到dstMat的中间
		for (int i = 0; i < rect.height(); ++i) {

			// 宽
			for (int j = 0; j < rect.width(); ++j) {
				byte b = in.data().get(in.step(0) * (i + rect.y()) + j + rect.x());
				dstMat.data().put(dstMat.step(0) * (i + y) + j + x, b);
			}
		}
		//
		return dstMat;
	}

	public static Rect GetCenterRect(Mat in) {
		
		int top = 0;
		int bottom = in.rows() - 1;

		// 上下
		for (int i = 0; i < in.rows(); ++i) {
			boolean bFind = false;
			for (int j = 0; j < in.cols(); ++j) {
				if ((in.data().get(i * in.step(0) + j) & 0xFF) > 20) {
					top = i;
					bFind = true;
					break;
				}
			}
			if (bFind) {
				break;
			}
			// 统计这一行或一列中，非零元素的个数
		}
		for (int i = in.rows() - 1; i >= 0; --i) {
			boolean bFind = false;
			for (int j = 0; j < in.cols(); ++j) {
				if ((in.data().get(i * in.step(0) + j) & 0xFF) > 20) {
					bottom = i;
					bFind = true;
					break;
				}
			}
			if (bFind) {
				break;
			}
			// 统计这一行或一列中，非零元素的个数
		}

		// 左右
		int left = 0;
		int right = in.cols() - 1;
		for (int j = 0; j < in.cols(); ++j) {
			boolean bFind = false;
			for (int i = 0; i < in.rows(); ++i) {
				if ((in.data().get(i * in.step(0) + j)& 0xFF) > 20) {
					left = j;
					bFind = true;
					break;
				}
			}
			if (bFind) {
				break;
			}
			// 统计这一行或一列中，非零元素的个数
		}
		for (int j = in.cols() - 1; j >= 0; --j) {
			boolean bFind = false;
			for (int i = 0; i < in.rows(); ++i) {
				if ((in.data().get(i * in.step(0) + j)& 0xFF) > 20) {
					right = j;
					bFind = true;
					break;
				}
			}
			if (bFind) {
				break;
			}
			// 统计这一行或一列中，非零元素的个数
		}

		Rect _rect = new Rect();
		_rect.x(left);
		_rect.y(top);
		_rect.width(right - left + 1);
		_rect.height(bottom - top + 1);
		return _rect;
	}

	public static float countOfBigValue(Mat mat, int iValue) {
		float iCount = 0.0f;
		if (mat.rows() > 1) {
			for (int i = 0; i < mat.rows(); ++i) {
				int tmp=mat.data().get(i * mat.step(0)) & 0xFF;
				if (tmp > iValue) {
					iCount += 1.0;
				}
			}
			return iCount;

		} else {
			for (int i = 0; i < mat.cols(); ++i) {
				int tmp=mat.data().get(i) & 0xFF;
				if (tmp > iValue) {
					iCount += 1.0;
				}
			}

			return iCount;
		}
	}

	/**
	 * 获取垂直和水平方向直方图
	 * 
	 * @param img
	 * @param t
	 *            HORIZONTAL 1 VERTICAL 0
	 * @return
	 */
	public static Mat projectedHistogram(Mat img, int t) {
		int sz = (t == 1) ? img.rows() : img.cols();
		Mat mhist = Mat.zeros(1, sz, CV_32F).asMat();

		FloatIndexer idx = mhist.createIndexer();

		for (int j = 0; j < sz; j++) {
			Mat data = (t == 1) ? img.row(j) : img.col(j);
			// 统计这一行或一列中，非零元素的个数，并保存到mhist中
//			PlateTestUtils.debug("data%f", countOfBigValue(data, 20));
			idx.put(0, j, countOfBigValue(data, 20));
		}

		// Normalize histogram
		DoublePointer min = new DoublePointer(0.0f);
		DoublePointer max = new DoublePointer(0.0f);

		minMaxLoc(mhist, min, max, new Point(0,0), new Point(0,0), null);

//		PlateTestUtils.debug("max%f", max.get());
		// 用mhist直方图中的最大值，归一化直方图
		if (max.get() > 0) {
			mhist.convertTo(mhist, -1, 1.0f / max.get(), 0);
		}
		return mhist;
	}

    /**
     * Assign values to feature
     * <p>
     * 样本特征为水平、垂直直方图和低分辨率图像所组成的矢量
     * 
     * @param in
     * @param sizeData
     *            低分辨率图像size = sizeData*sizeData, 可以为0
     * @return
     */
	 public static Mat features(Mat in, int sizeData) {
		if (null==in.data()){
			throw new RuntimeException("Mat is empty");
		}
			
		final int VERTICAL = 0;
		final int HORIZONTAL = 1;
		// 抠取中间区域
		Rect _rect = GetCenterRect(in);
		Mat tmpIn = CutTheRect(in, _rect);
		// Mat tmpIn = in.clone();
		// Low data feature
		//PlateTestUtils.debug("rows:"+in.rows()+"cols:"+in.cols());

		
		Mat lowData = new Mat();
		resize(tmpIn, lowData, new Size(sizeData, sizeData));

		// Histogram features HORIZONTAL 1 VERTICAL 0
		Mat vhist = projectedHistogram(lowData, VERTICAL);
		FloatIndexer vhistIdx = vhist.createIndexer();
		Mat hhist = projectedHistogram(lowData, HORIZONTAL);
		FloatIndexer hhistIdx = hhist.createIndexer();

		// Last 10 is the number of moments components
		int numCols = vhist.cols() + hhist.cols() + lowData.cols() * lowData.cols();

		Mat out = Mat.zeros(1, numCols, CV_32F).asMat();
		FloatIndexer idx = out.createIndexer();
		// Asign values to

		// feature,ANN的样本特征为水平、垂直直方图和低分辨率图像所组成的矢量
		int j = 0;
		for (int i = 0; i < vhist.cols(); i++) {
			idx.put(0, j, vhistIdx.get(0, i));
			j++;
		}
		for (int i = 0; i < hhist.cols(); i++) {
			idx.put(0, j, hhistIdx.get(0, i));
			j++;
		}
		for (int x = 0; x < lowData.cols(); x++) {
			for (int y = 0; y < lowData.rows(); y++) {
				float v = idx.get(0, j) + (lowData.ptr(x, y).get() & 0xFF);
				idx.put(0, j, v);

				j++;
			}
		}

		return out;
	}

}
