package com.crenjoy.easypr.core;

import java.io.File;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.Mat;

import com.crenjoy.easypr.util.Utils;

/**
 * 车牌识别具体实现 
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 * 
 */
public class PlateRecognitionImpl extends PlateRecognition {

	static final String kDefaultSvmPath = "/model/svm.xml";

	static final String kDefaultAnnPath = "/model/ann.xml";

	// 颜色定位车牌
	private PlateLocate plateLocateColor;
	// sobel定位车牌
	private PlateLocate plateLocateSobel;
	// 判断是否车牌
	private PlateJudgeSvm plateJudgeSvm;
	// 车牌分割
	private CharsSegment charsSegment;
	// 字符识别
	private CharsRecognise charsRecognise;

	/**
	 * 车牌识别
	 * 
	 * @param easyPath
	 */
	public PlateRecognitionImpl(String easyPath) {
		super();
		String svm_xml = Utils.splitPath(easyPath, kDefaultSvmPath);
		String ann_xml = Utils.splitPath(easyPath, kDefaultAnnPath);
		if (!(new File(svm_xml)).exists() || (new File(svm_xml)).length() == 0) {
			Utils.copyFile(PlateRecognitionImpl.class, kDefaultSvmPath, svm_xml);
		}
		if (!(new File(ann_xml)).exists() || (new File(ann_xml)).length() == 0) {
			Utils.copyFile(PlateRecognitionImpl.class, kDefaultAnnPath, ann_xml);
		}

		plateLocateColor = new PlateLocateColor();
		plateLocateSobel = new PlateLocateSobel();
		plateJudgeSvm = new PlateJudgeSvm(svm_xml);
		charsSegment = new CharsSegment();
		charsRecognise = new CharsRecognise(ann_xml);
	}

	/**
	 * 重新学习路径 如设置保存重新训练数据
	 * 
	 * @param trainPath
	 */
	public void setTrainPath(String trainPath) {
		this.charsRecognise.setTrainPath(trainPath);
		this.plateJudgeSvm.setTrainPath(trainPath);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.crenjoy.easypr.core.PlateRecognition#locateByColor(org.bytedeco.javacpp
	 * .opencv_core.Mat)
	 */
	@Override
	public Vector<Plate> locateByColor(Mat src) {
		return plateLocateColor.locate(src);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.crenjoy.easypr.core.PlateRecognition#locateBySobel(org.bytedeco.javacpp
	 * .opencv_core.Mat)
	 */
	@Override
	public Vector<Plate> locateBySobel(Mat src) {
		return plateLocateSobel.locate(src);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.crenjoy.easypr.core.PlateRecognition#judgePlate(org.bytedeco.javacpp
	 * .opencv_core.Mat)
	 */
	@Override
	public boolean judgePlate(Mat src) {
		return plateJudgeSvm.plateJudge(src) == 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.crenjoy.easypr.core.PlateRecognition#judgePlate(java.util.Vector)
	 */
	@Override
	public Vector<Plate> judgePlate(Vector<Plate> srcVec) {
		return plateJudgeSvm.plateJudgePlate(srcVec);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.crenjoy.easypr.core.PlateRecognition#segmentPlate(com.crenjoy.easypr
	 * .core.Plate)
	 */
	@Override
	public Vector<Mat> segmentPlate(Mat src) {
		Vector<Mat> resultVec = new Vector<Mat>();
		charsSegment.charsSegment(src, resultVec);
		return resultVec;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.crenjoy.easypr.core.PlateRecognition#recognisePlate(java.util.Vector)
	 */
	@Override
	public String recognisePlate(Vector<Mat> srcVec) {
		StringBuffer plateLicense = new StringBuffer();
		for (Mat block : srcVec) {
			String[] s = this.charsRecognise.recognise(block);
			plateLicense.append(s[1]);
		}
		if (plateLicense.length() < 7) {
			return null;
		}
		return plateLicense.substring(0, 7);
	}

}
