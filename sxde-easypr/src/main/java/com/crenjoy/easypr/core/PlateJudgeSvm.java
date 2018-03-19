package com.crenjoy.easypr.core;

import java.io.File;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_ml.SVM;

import com.crenjoy.easypr.util.OpenCVUtils;
import com.crenjoy.easypr.util.PlateTestUtils;

/**
 * 车牌判断
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public class PlateJudgeSvm {

	private SVM svm_;
	
	/** 重新学习路径 如设置保存重新训练数据 */
	private String trainPath;

	/**
	 * 重新学习路径 如设置保存重新训练数据
	 * 
	 * @return
	 */
	public String getTrainPath() {
		return trainPath;
	}

	/**
	 * 重新学习路径 如设置保存重新训练数据
	 * 
	 * @param trainPath
	 */
	public void setTrainPath(String trainPath) {
		this.trainPath = trainPath;
	}

	/**
	 * SVM XML 地址
	 * @param svm_xml
	 */
	public PlateJudgeSvm(String svm_xml) {
		svm_=SVM.create();
		svm_=OpenCVUtils.loadAlgorithm(svm_,svm_xml,null);
	}
	
	

	/**
	 * 对单幅图像进行SVM判断
	 * 
	 * @param inMat
	 * @return
	 */
	public int plateJudge(final Mat inMat) {
		Mat features = Features.getHistogramFeatures(inMat);
		
		float response = svm_.predict(features);

		return Float.valueOf(response).intValue();
	}


	/**
	 * 对多幅图像进行SVM判断
	 * @param inVec
	 * @return
	 */
	public Vector<Mat> plateJudge(final Vector<Mat> inVec) {
		Vector<Mat> resultVec=new Vector<Mat>();
		for (Mat inMat : inVec) {
			int response = plateJudge(inMat);

			if (response == 1) {
				resultVec.add(inMat);
			}
		}
		return resultVec;
	}

	/**
	 * 对多幅车牌进行SVM判断
	 * 
	 * @param inVec
	 * @return
	 */
	public Vector<Plate> plateJudgePlate(final Vector<Plate> inVec) {
		Vector<Plate> resultVec = new Vector<Plate>();
		for (Plate inPlate :  inVec) {
			Mat inMat = inPlate.getPlateMat();
			int response = plateJudge(inMat);
			
			if (StringUtils.isNotEmpty(trainPath)) {
				long p= (new Date()).getTime();
				String path = trainPath + File.separatorChar + "svm_"+String.format("%d", response) + File.separatorChar;
				PlateTestUtils.imwrite(inMat,path,String.format("%d", p));
			}
			
			if (response == 1){
				resultVec.add(inPlate);
			}
			else {
//				int w = inMat.cols();
//				int h = inMat.rows();
//
//				// 再取中间部分判断一次
//				Mat tmpmat = new Mat(inMat, new Rect((int) (w * 0.05f), (int) (h * 0.1f), (int) (w * 0.9f), (int) (h * 0.8f)));
//				Mat tmpDes = inMat.clone();
//				resize(tmpmat, tmpDes, inMat.size());
//				response = plateJudge(tmpDes);
//				if (response == 1) {
//					resultVec.add(inPlate);
//				}
			}
		}
		return resultVec;
	}

}
