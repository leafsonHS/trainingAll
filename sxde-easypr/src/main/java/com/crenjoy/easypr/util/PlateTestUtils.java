package com.crenjoy.easypr.util;

import java.io.File;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_core.Scalar;

import com.crenjoy.easypr.core.Plate;
import com.crenjoy.easypr.core.PlateLocate;

/**
 * 辅助测试类
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public class PlateTestUtils {

	private static final Log log = LogFactory.getLog(PlateTestUtils.class);

	private static int count = 1;

	/**
	 * 是否开启Debug模式
	 * 
	 * @return
	 */
	public static boolean isDebug() {
		return log.isDebugEnabled();
	}
	
	/**
	 * 写Debug 信息
	 * @param msg
	 * @param args
	 */
	public static void debug(String msg,Object... args){
		log.debug(String.format(count+" :"+msg,args));
	}
	
	/**
	 * 写Mat 到文件
	 * 
	 * @param img
	 * @param name
	 */
	public static void imwriteVector(Vector<Mat> imgs, String filename){
		for(Mat img :imgs){
			imwrite(img,filename);
		}
	}
	
	/**
	 * 写Mat 到文件
	 * 
	 * @param img
	 * @param name
	 */
	public static void imwrite(Mat img,String path ,String filename) {
		if (!isDebug()) {
			return;
		}
		if (count==1){
			File file=new File(path);
			try{
				FileUtils.deleteDirectory(file);
			}catch(Exception e){
				
			}
		}
		OpenCVUtils.imwrite(Utils.splitPath("", String.format(path+"%d_%s.jpg", count++, filename)), img);
	}

	/**
	 * 写Mat 到文件
	 * 
	 * @param img
	 * @param name
	 */
	public static void imwrite(Mat img, String filename) {
		imwrite(img,File.separatorChar+"tmp"+File.separatorChar,filename);
	}

	/**
	 * 写Rect Mat到文件 
	 * @param img
	 * @param filename
	 * @param rects
	 */
	public static void imwrite(Mat img, String filename, RotatedRect... rects) {
		if (!isDebug()) {
			return;
		}
		for (RotatedRect roi_rect : rects) {
			//判断是否安全方形图片
			Rect safeBoundRect = PlateLocate.calcSafeRect(roi_rect.boundingRect(), img);
			if (null!=safeBoundRect) {
				Mat t = new Mat(img, safeBoundRect);
				imwrite(t, filename);
			}
		}
	}
	
	/**
	 * 标记图片
	 * @param img
	 * @param rect
	 * @param filename
	 */
	public static void markMat(Mat img,RotatedRect rect,String filename){
		if (!isDebug()) {
			return;
		}
		Mat out=OpenCVUtils.markMat(img, rect, new Scalar(0, 255, 255, 0));
		imwrite(out,filename);
	}
	
	/**
	 * 写Rect Mat到文件 
	 * @param img
	 * @param filename
	 * @param rects
	 */
	public static void imwrite(Mat img, String filename, Rect... rects) {
		if (!isDebug()) {
			return;
		}
		for (Rect roi_rect : rects) {
			Mat t = new Mat(img, roi_rect);
			imwrite(t, filename);
		}
	}
	
	/**
	 * 写车牌  Mat 
	 * @param plates
	 * @param filename
	 */
	public static void imwrite(Vector<Plate> plates, String filename){
		if (!isDebug()) {
			return;
		}
		for (Plate plate : plates){
			imwrite(plate.getPlateMat(),plate.getLocateType().name()+"_"+ filename);
		}
	}

}
