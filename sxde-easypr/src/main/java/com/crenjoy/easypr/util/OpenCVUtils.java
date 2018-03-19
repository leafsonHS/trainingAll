package com.crenjoy.easypr.util;

import static org.bytedeco.javacpp.opencv_core.CV_8UC3;
import static org.bytedeco.javacpp.opencv_core.addWeighted;
import static org.bytedeco.javacpp.opencv_highgui.CV_WINDOW_AUTOSIZE;
import static org.bytedeco.javacpp.opencv_highgui.destroyWindow;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.namedWindow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_AREA;
import static org.bytedeco.javacpp.opencv_imgproc.line;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.io.File;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.Algorithm;
import org.bytedeco.javacpp.opencv_core.FileNode;
import org.bytedeco.javacpp.opencv_core.FileStorage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;

import com.crenjoy.easypr.core.Plate;
import com.crenjoy.easypr.enums.LocateType;

/**
 * OpenCV 工具类 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * 
 * @author CGD
 * 
 */
public class OpenCVUtils {

	/**
	 * load Algorithm
	 * 
	 * @param filename
	 *            文件名
	 * @param objname
	 *            节点名
	 * @return
	 */
	public static <T extends Algorithm> T loadAlgorithm(T algorithm, String filename, String objname) {
		FileStorage fs = null;
		try {
			fs = new FileStorage(filename, FileStorage.READ);
			FileNode fn = StringUtils.isEmpty(objname) ? fs.getFirstTopLevelNode() : fs.get(objname);
			algorithm.read(fn);
			return algorithm;
		} finally {
			try {
				if (null != fs) {
					fs.close();
					fs = null;
				}
			} catch (Exception e) {
				LogFactory.getLog(Utils.class).error("关闭文件错误", e.fillInStackTrace());
			}
		}
	}

	/**
	 * Loading image Mat
	 * 
	 * @param image
	 * @return
	 */
	public static Mat imread(String image) {
		return org.bytedeco.javacpp.opencv_imgcodecs.imread(image);
	}

	/**
	 * 将图片保存成文件
	 * 
	 * @param filename
	 *            文件地址
	 * @param image
	 *            图片
	 * @return
	 */
	public static boolean imwrite(String filename, final Mat image) {
		String folder = FilenameUtils.getFullPath(filename);
		// 文件夹不存在创建文件夹
		File folderFile = new File(folder);
		if (!folderFile.exists()) {
			folderFile.mkdirs();
		}
		return org.bytedeco.javacpp.opencv_imgcodecs.imwrite(filename, image);
	}

	/**
	 * 显示Mat图片
	 * 
	 * @param result
	 * @return
	 */
	public static int showResult(final Mat result) {
		namedWindow("EasyPR", CV_WINDOW_AUTOSIZE);

		final int RESULTWIDTH = 640; // 640 930
		final int RESULTHEIGHT = 540; // 540 710

		Mat img_window = new Mat();
		img_window.create(RESULTHEIGHT, RESULTWIDTH, CV_8UC3);

		int nRows = result.rows();
		int nCols = result.cols();

		Mat result_resize = null;
		if (nCols <= img_window.cols() && nRows <= img_window.rows()) {
			result_resize = result;

		} else if (nCols > img_window.cols() && nRows <= img_window.rows()) {
			result_resize=new Mat();
			float scale = (float) (img_window.cols()) / (float) (nCols);
			resize(result, result_resize, new Size(), scale, scale, CV_INTER_AREA);

		} else if (nCols <= img_window.cols() && nRows > img_window.rows()) {
			result_resize=new Mat();
			float scale = (float) (img_window.rows()) / (float) (nRows);
			resize(result, result_resize, new Size(), scale, scale, CV_INTER_AREA);

		} else if (nCols > img_window.cols() && nRows > img_window.rows()) {
			Mat result_middle = new Mat();
			float scale = (float) (img_window.cols()) / (float) (nCols);
			resize(result, result_middle, new Size(), scale, scale, CV_INTER_AREA);

			if (result_middle.rows() > img_window.rows()) {
				result_resize=new Mat();
				float scalel = (float) (img_window.rows()) / (float) (result_middle.rows());
				resize(result_middle, result_resize, new Size(), scalel, scalel, CV_INTER_AREA);

			} else {
				result_resize = result_middle;
			}
		} else {
			result_resize = result;
		}

		Mat imageRoi = new Mat(img_window, new Rect((RESULTWIDTH - result_resize.cols()) / 2, (RESULTHEIGHT - result_resize.rows()) / 2, result_resize.cols(),
				result_resize.rows()));

		addWeighted(imageRoi, 0, result_resize, 1, 0, imageRoi);

		imshow("EasyPR", img_window);
		waitKey();

		destroyWindow("EasyPR");

		return 0;
	}

	/**
	 * 图片上用颜色标记出车牌
	 * 
	 * @param src
	 * @param plateVec
	 */
	public static Mat markMat(Mat src, Vector<Plate> plateVec) {
		// 如果是Debug模式，则还需要将定位的图片显示在原图左上角
		int index = 0;
		Mat result = new Mat();
		src.copyTo(result);
		for (int j = 0; j < plateVec.size(); j++) {
			Plate item = plateVec.get(j);
			Mat plate = item.getPlateMat();

			int height = 36;
			int width = 136;
			if (height * index + height < result.rows()) {
				Mat imageRoi = new Mat(result, new Rect(0, 0 + height * index, width, height));
				addWeighted(imageRoi, 0, plate, 1, 0, imageRoi);
			}
			RotatedRect minRect = item.getPlatePos();
			index++;
			Scalar lineColor = null;

			if (LocateType.SOBEL.equals(item.getLocateType())) {
				lineColor = new Scalar(255, 0, 0, 0);
			}else if (LocateType.COLOR.equals(item.getLocateType())) {
				lineColor = new Scalar(0, 255, 0, 0);
			}else{
				lineColor = new Scalar(255, 255, 255, 0);
			}
			result=markMat(result,minRect,lineColor);
		}
       return result;
	}

	/**
	 * 标记矩形位置
	 * 
	 * @param src
	 * @param rect
	 * @param scalar
	 * @return
	 */
	public static Mat markMat(Mat src, RotatedRect minRect, Scalar lineColor) {
		Point2f rect_points = new Point2f();
		rect_points.put(new Point2f());
		rect_points.put(new Point2f());
		rect_points.put(new Point2f());
		rect_points.put(new Point2f());
		minRect.points(rect_points);

		for (int i = 0; i < 4; i++) {
			Point p1 = new Point(Float.valueOf(rect_points.position(i).x()).intValue(), Float.valueOf(rect_points.position(i).y()).intValue());
			Point p2 = new Point(Float.valueOf(rect_points.position((i + 1) % 4).x()).intValue(), Float.valueOf(rect_points.position((i + 1) % 4).y())
					.intValue());

			line(src, p1, p2, lineColor, 2, 8, 0);
		}

		return src;
	}

}
