package com.crenjoy.easypr.core;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_ml.ANN_MLP;

import com.crenjoy.easypr.Config;
import com.crenjoy.easypr.util.OpenCVUtils;
import com.crenjoy.easypr.util.PlateTestUtils;

/**
 * 单个字符识别 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * 
 * @author CGD
 * 
 */
public class CharsRecognise {

	private ANN_MLP ann_;

	private String ann_xml_;
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
	 * xml 路径
	 * 
	 * @param ann_xml
	 */
	public CharsRecognise(String ann_xml) {
		this.ann_xml_ = ann_xml;

		ann_ = ANN_MLP.create();
		ann_ = OpenCVUtils.loadAlgorithm(ann_, this.ann_xml_, null);
	}

	/**
	 * 单字符识别
	 * 
	 * @param input
	 * @return {"zh_sx","晋"}
	 */
	public String[] recognise(Mat input) {

		Mat feature = CoreFunc.features(input, Config.kPredictSize);

		int index = Float.valueOf(ann_.predict(feature)).intValue();

		// PlateTestUtils.debug("index : %d", index);
		
		String charName = null;
		if (index < 34) {
			charName = Config.kChars[index];
		} else {
			charName = Config.getProvinceMap().get(Config.kChars[index]);
		}

		if (StringUtils.isNotEmpty(trainPath)) {
			long p = (new Date()).getTime();
			String path = trainPath + File.separatorChar + Config.kChars[index] + File.separatorChar;
			
			PlateTestUtils.imwrite(input, path, String.format("%d", p));
		}
		return new String[] { Config.kChars[index], charName };
	}

}
