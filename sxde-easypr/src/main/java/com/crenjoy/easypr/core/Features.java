package com.crenjoy.easypr.core;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.indexer.FloatIndexer;

/**
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public class Features {
	
	/**
	 * 获得车牌的特征数
	 * @param in
	 * @return
	 */
	public static Mat getTheFeatures(Mat in){
		  final int VERTICAL = 0;
		  final int HORIZONTAL = 1;

		  // Histogram features
		  Mat vhist = CoreFunc.projectedHistogram(in, VERTICAL);
		  FloatIndexer vhistIdx = vhist.createIndexer();
		  Mat hhist = CoreFunc.projectedHistogram(in, HORIZONTAL);
		  FloatIndexer hhistIdx = hhist.createIndexer();

		  // Last 10 is the number of moments components
		  int numCols = vhist.cols() + hhist.cols();

		  Mat out = Mat.zeros(1, numCols, CV_32F).asMat();
		  FloatIndexer idx = out.createIndexer();

		  // Asign values to feature,样本特征为水平、垂直直方图
		  int j = 0;
		  for (int i = 0; i < vhist.cols(); i++) {
			idx.put(0, j, vhistIdx.get(0, i));
		    j++;
		  }
		  for (int i = 0; i < hhist.cols(); i++) {
			idx.put(0, j, hhistIdx.get(0, i));
		    j++;
		  }
		  return out;
	}

//	//! EasyPR的getFeatures回调函数
//	//! 用于从车牌的image生成svm的训练特征features
//
//	typedef void (*svmCallback)(final Mat image, Mat features){
//		
//	}


	/**
	 * EasyPR的getFeatures回调函数
	 * 本函数是获取垂直和水平的直方图图值
	 * @param image
	 * @return
	 */
	public static Mat getHistogramFeatures(final Mat image){
		  Mat grayImage=new Mat();
		  cvtColor(image, grayImage, CV_RGB2GRAY);
		  //grayImage = histeq(grayImage);
		  Mat img_threshold=new Mat();
		  threshold(grayImage, img_threshold, 0, 255,CV_THRESH_OTSU + CV_THRESH_BINARY);
		  
//		  PlateTestUtils.imwrite(img_threshold, "features_1");
		  return  getTheFeatures(img_threshold);
		  
	}

	/**
	 * 本函数是获取SIFT特征子
	 * @param image
	 * @param features
	 */
	public static void getSIFTFeatures(final Mat image, Mat features){
		  // TODO 待完善
	}

	/**
	 * 本函数是获取HOG特征子
	 * @param image
	 * @param features
	 */
	public static void getHOGFeatures(final Mat image, Mat features){
		  // TODO 待完善
	}

	/**
	 * 本函数是获取HSV空间量化的直方图特征子
	 * @param image
	 * @param features
	 */
	public static void getHSVHistFeatures(final Mat image, Mat features){
		  // TODO 待完善
	}
}
