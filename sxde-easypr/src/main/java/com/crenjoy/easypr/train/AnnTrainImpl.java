package com.crenjoy.easypr.train;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_ml.ROW_SAMPLE;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_ml.ANN_MLP;
import org.bytedeco.javacpp.opencv_ml.TrainData;
import org.bytedeco.javacpp.annotation.ByRef;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;

import com.crenjoy.easypr.Config;
import com.crenjoy.easypr.core.CoreFunc;
import com.crenjoy.easypr.util.OpenCVUtils;

/**
 * 人工神经网络 字符识别学习程序
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 * 
 */
public class AnnTrainImpl extends Train {

	private static Log log = LogFactory.getLog(SvmTrainImpl.class);

	private static final int kNeurons = 40;

	/** ANN */
	private ANN_MLP ann_;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#clear()
	 */
	@Override
	public void clear() {
		ann_ = null;
	}

	@Override
	public void init() {
		if (null == ann_) {
			ann_ = ANN_MLP.create();
		}
		Mat layers = new Mat(1, 3, CV_32SC1);
		IntIndexer layIdx = layers.createIndexer();
		layIdx.put(0, 0, 120);// the input layer
		layIdx.put(0, 1, kNeurons);// the neurons
		layIdx.put(0, 2, Config.kCharsTotalNumber);// the output layer

		ann_.setLayerSizes(layers);
		ann_.setActivationFunction(ANN_MLP.SIGMOID_SYM, 1, 1);
		ann_.setTrainMethod(ANN_MLP.BACKPROP);
		ann_.setBackpropWeightScale(0.1);
		ann_.setBackpropMomentumScale(0.1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#load(java.lang.String)
	 */
	@Override
	public void load(String xmlFile) {
		if (null == ann_) {
			ann_ = ANN_MLP.create();
		}
		ann_=OpenCVUtils.loadAlgorithm(ann_, xmlFile, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#train(java.util.Vector)
	 */
	@Override
	public Long train(Vector<TrainItem> trainItem) {

		log.debug(">>Training ANN model, please wait...");

		long start = Calendar.getInstance().getTimeInMillis();
		TrainData trainData = trainData(trainItem);

		ann_.train(trainData);

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
		ann_.save(xmlFile);
		log.debug(String.format(">> Your ANN Model was saved to %s", xmlFile));
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
			Mat image = imread(item.getFile(), 0);
			if (null == image.data()) {				
				log.debug(String.format(">> Invalid image: %s  ignore.", item.getFile()));
				continue;
			}
			Mat feature = CoreFunc.features(image, Config.kPredictSize);

			int label = (int) ann_.predict(feature);

			if (!stats.containsKey(label)) {
				stats.put(label, new StatItem(label));
			}

			// 统计数据
			stats.get(label).match(label == item.getLabel(),item.getFile());
		}

		return stats;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.crenjoy.easypr.train.ITrain#trainTestData(java.lang.String,
	 * java.util.Vector, java.util.Vector, boolean)
	 */
	@Override
	public void trainTestData(String dir, @ByRef Vector<TrainItem> trainList, @ByRef Vector<TrainItem> testList, boolean isTest) {
		if (StringUtils.isEmpty(dir)) {
			log.error(">> chars_folder_ is empty ");
			return;
		}
		log.debug(String.format("Collecting chars in %s", dir));

		for (int i = 0; i < Config.kCharsTotalNumber; i++) {
			String char_key = Config.kChars[i];
			Vector<TrainItem> charTrainList = loadDir(dir + File.separatorChar + char_key, i);
			trainList.addAll(charTrainList);
		}

		// 需要测试
		if (isTest) {
			testList.addAll(trainList);
		}

	}

	/**
	 * 学习数据项转换为训练数据
	 * 
	 * @param tdata
	 * @return
	 */
	private TrainData trainData(Vector<TrainItem> tdata) {
		Mat samples = new Mat();
		Vector<Integer> labels = new Vector<Integer>();

		for (TrainItem item : tdata) {
			Mat img = imread(item.getFile(), 0);// a grayscale image
			if (null == img.data()) {
				continue;
			}
			Mat fps = CoreFunc.features(img, Config.kPredictSize);
			samples.push_back(fps);
			labels.add(item.getLabel());
		}

		Mat samples_ = new Mat();
		samples.convertTo(samples_, CV_32F);
		try {
			samples.close();
		} catch (Exception ex) {

		}

		Mat train_classes = Mat.zeros(labels.size(), Config.kCharsTotalNumber, CV_32F).asMat();
		for (int i = 0; i < train_classes.rows(); ++i) {
			FloatIndexer indexer = train_classes.createIndexer();
			indexer.put(i, labels.get(i), 1.0f);
		}
		return TrainData.create(samples_, ROW_SAMPLE, train_classes);
	}

}
