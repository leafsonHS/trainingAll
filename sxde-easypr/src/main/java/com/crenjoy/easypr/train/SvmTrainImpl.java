package com.crenjoy.easypr.train;

import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_ml.ROW_SAMPLE;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_ml.SVM;
import org.bytedeco.javacpp.opencv_ml.TrainData;
import org.bytedeco.javacpp.annotation.ByRef;

import com.crenjoy.easypr.core.Features;
import com.crenjoy.easypr.util.OpenCVUtils;

/**
 * 机器学习，是否车牌判断 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * 
 * @author CGD
 * 
 */
public class SvmTrainImpl extends Train {

	private static Log log = LogFactory.getLog(SvmTrainImpl.class);

	/** 训练、测试图片比例 */
	private static float kSvmPercentage = 1.0f; // 0.7

	/** SVM */
	private SVM svm_;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#clear()
	 */
	@Override
	public void clear() {
		svm_ = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#init()
	 */
	@Override
	public void init() {
		if (null == svm_) {
			svm_ = SVM.create();
		}
		svm_.setType(SVM.C_SVC);
		svm_.setKernel(SVM.RBF);
		svm_.setDegree(0.1);
		// 1.4 bug fix: old 1.4 ver gamma is 1
		svm_.setGamma(0.1);
		svm_.setCoef0(0.1);
		svm_.setC(1);
		svm_.setNu(0.1);
		svm_.setP(0.1);
		svm_.setTermCriteria(new org.bytedeco.javacpp.opencv_core.TermCriteria(org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_ITER, 100000, 0.00001f));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#load(java.lang.String)
	 */
	@Override
	public void load(String xmlFile) {
		if (null == svm_) {
			svm_ = SVM.create();
		}
		svm_=OpenCVUtils.loadAlgorithm(svm_, xmlFile, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#train(java.util.Vector)
	 */
	@Override
	public Long train(Vector<TrainItem> trainItem) {
		log.debug(">> Training SVM model, please wait...");
		long start = Calendar.getInstance().getTimeInMillis();

		TrainData trainData = trainData(trainItem);
		// svm_.trainAuto(train_data, 10, SVM::getDefaultGrid(SVM::C),
		// SVM::getDefaultGrid(SVM::GAMMA), SVM::getDefaultGrid(SVM::P),
		// SVM::getDefaultGrid(SVM::NU), SVM::getDefaultGrid(SVM::COEF),
		// SVM::getDefaultGrid(SVM::DEGREE), true);
		svm_.train(trainData);

		long end = Calendar.getInstance().getTimeInMillis();
		log.debug(String.format(">> Training done. Time elapse: %d s", (end - start) / 1000));
		return (end - start) / 1000;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#save(java.lang.String)
	 */
	@Override
	public void save(String xmlFile) {
		// 创建文件夹
		String filePath = FilenameUtils.getFullPath(xmlFile);
		if (!(new File(filePath)).exists()) {
			(new File(filePath)).mkdirs();
		}
		svm_.save(xmlFile);
		log.debug(String.format(">> Your SVM Model was saved to %s", xmlFile));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#test(java.util.Vector)
	 */
	@Override
	public Map<Integer, StatItem> test(Vector<TrainItem> trainItem) {
		Map<Integer, StatItem> stats = new HashMap<Integer, StatItem>();

		for (TrainItem item : trainItem) {
			Mat image = OpenCVUtils.imread(item.getFile());
			if (null == image.data()) {
				log.debug(String.format(">> Invalid image: %s  ignore.", item.getFile()));
				continue;
			}
			Mat feature = Features.getHistogramFeatures(image);
			int label = (int) svm_.predict(feature);

			if (!stats.containsKey(label)) {
				stats.put(label, new StatItem(label));
			}

			log.debug(label+"----"+item.getFile());
			// 统计数据
			stats.get(label).match(label == item.getLabel(), item.getFile());
		}

		return stats;
	}

	/**
	 * 学习数据项转换为训练数据
	 * 
	 * @param tdata
	 * @return
	 */
	private TrainData trainData(Vector<TrainItem> tdata) {
		Mat samples = new Mat();
		Vector<Integer> responses = new Vector<Integer>();
		// 学习数据特征及对应标签
		for (TrainItem f : tdata) {
			Mat image = OpenCVUtils.imread(f.getFile());
			if (null == image.data()) {
				log.debug(String.format(">> Invalid image: %s  ignore.", f.getFile()));
				continue;
			}
			Mat feature = Features.getHistogramFeatures(image);
			samples.push_back(feature);
			responses.add(f.getLabel());
		}
		// 学习图片数据
		Mat samples_ = new Mat();
		samples.convertTo(samples_, CV_32FC1);
		try {
			samples.close();
		} catch (Exception ex) {

		}

		// 学习数据标签
		int[] r = new int[responses.size()];
		for (int i = 0; i < responses.size(); i++) {
			r[i] = responses.get(i);
		}
		Mat responses_ = new Mat(r);
		// 学习数据生成
		return TrainData.create(samples_, ROW_SAMPLE, responses_);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#trainTestData(java.lang.String,
	 * java.util.Vector, java.util.Vector, boolean)
	 */
	@Override
	public void trainTestData(String dir, @ByRef Vector<TrainItem> trainList, @ByRef Vector<TrainItem> testList, boolean isTest) {
		
		Vector<TrainItem> noTrainTest = loadDir(dir + File.separatorChar + "no", 0);
		Vector<TrainItem> hasTrainTest = loadDir(dir + File.separatorChar + "has", 1);
		// 不测试
		if (!isTest) {
			trainList.addAll(noTrainTest);
			trainList.addAll(hasTrainTest);
			return;
		}
		if (1 != (int) SvmTrainImpl.kSvmPercentage) {
			splitTrainData(hasTrainTest, SvmTrainImpl.kSvmPercentage, trainList, testList);
			splitTrainData(noTrainTest, SvmTrainImpl.kSvmPercentage, trainList, testList);
		} else {
			trainList.addAll(noTrainTest);
			trainList.addAll(hasTrainTest);
			testList.addAll(noTrainTest);
			testList.addAll(hasTrainTest);
		}
	}

}
