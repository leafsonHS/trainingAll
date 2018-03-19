package com.crenjoy.easypr.train;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.annotation.ByRef;

import com.crenjoy.easypr.preproccess.PreTrainData;


/**
 * 机器学习接口、人工神经网络学习接口
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public abstract class Train{

	private static Log log = LogFactory.getLog(SvmTrainImpl.class);
	
	/**
	 * 清理数据
	 */
	public abstract void clear(); 
	
	/**
	 * 初始化机器学习、人工神经网络学习参数
	 */
	public abstract void init(); 
	
	/**
	 * 加载已学习的XML文件
	 * @param xmlFile
	 */
	public abstract void load(String xmlFile);
	
	/**
	 * 学习
	 * @param trainItem 学习数据
	 * @return 返回学习时长 单位秒
	 */
	public abstract Long train(Vector<TrainItem> trainItem);

	/**
	 * 保存XML 文件 
	 * @param xmlFile
	 */
	public abstract void save(String xmlFile);
	
	/**
	 * 测试数据 并返回统计结果
	 * @param trainItem 测试数据
	 * @return
	 */
	public abstract Map<Integer,StatItem> test(Vector<TrainItem> trainItem);
	

	/**
	 * 准备学习和测试数据
	 * @param dir
	 * @param trainList 学习数据
	 * @param testList 测试数据 
	 * @param isTest 是否测试
	 */
	public abstract void trainTestData(String dir,@ByRef Vector<TrainItem> trainList,@ByRef Vector<TrainItem> testList,boolean isTest);
	
    /**
	 * 首次学习
	 * @param dir 学习文件目录
	 * @param xmlFile 保存文件目录
	 * @param isTest 是否测试
	 */
	public  void firstTrain(String dir,String xmlFile,boolean isTest){
		//清空数据
		clear();
		//初始化
		init();
		//学习
		train(dir,xmlFile,isTest);
	}
	/**
	 * 追加学习
	 * @param dir 学习文件目录
	 * @param xmlFile 保存文件目录
	 * @param isTest 是否测试
	 */
	public  void appendTrain(String dir,String xmlFile,boolean isTest){
		//清空数据
		clear();
		//加载xml
		load(xmlFile);
		//初始化
		init();
		//学习
		train(dir,xmlFile,isTest);
	}
	/**
	 * 学习
	 * @param dir 学习文件目录
	 * @param xmlFile 保存文件目录
	 * @param isTest 是否测试
	 */
	private  void train(String dir,String xmlFile,boolean isTest){
		//准备数据
		Vector<TrainItem> trainList=new Vector<TrainItem>();
		Vector<TrainItem> testList=new Vector<TrainItem>();
		trainTestData(dir,trainList,testList,isTest);
		//学习
		long s=train(trainList);
		log.info(String.format("学习%d条数据，总用时 %d秒 ", trainList.size(),s));
		//保存
		save(xmlFile);
		
		//测试
		if(!isTest || testList.size()==0){
			return;
		}
		Map<Integer,StatItem> stat= test(testList);
		StatItem total =new StatItem();
		for(StatItem item :  stat.values()){
			total.setTotalCount(item.getTotalCount()+total.getTotalCount());
			total.setFailCount(item.getFailCount()+total.getFailCount());
			//识别错误文件处理
			//PreTrainData.failFileProc(item.getErrorFile());
			//打印识别错误的文件
			PreTrainData.failFilePrint(item.getErrorFile());
			log.info(String.format("测试 Label %d 共%d条  失败%d 条  失败率 %4$.3f",item.label,item.totalCount,item.failCount,item.failCount*1.0f/item.totalCount));
		}
		log.info(String.format("测试  SVM 共%d条  失败%d 条  失败率 %3$.3f",total.totalCount,total.failCount,total.failCount*1.0f/total.totalCount));
	}
	
	/**
	 * 加载目录下的学习数据
	 * @param dir
	 * @param label
	 * @return
	 */
	public Vector<TrainItem> loadDir(String dir,int label){
		Vector<TrainItem> trainList = new Vector<TrainItem>();
		File dirFile = new File(dir);
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			log.error("Image Folder Not Find:" + dirFile);
			return trainList;
		}
		
		File[] files = dirFile.listFiles();
		if (null==files){
			return trainList;
		}
		
		for(File file:files){
			if (file.isFile()){
				trainList.add(new TrainItem(file.getAbsolutePath(), label));
			}
		}
		return trainList;
	}
	
	/**
	 * 按比例分割学习和测试数据 
	 * @param totalList 全部数据
	 * @param percentage 比例
	 * @param trainList 学习数据
	 * @param testList  测试数据
	 */
	public void splitTrainData(Vector<TrainItem> totalList,float percentage, @ByRef Vector<TrainItem> trainList,@ByRef Vector<TrainItem> testList){
		if (percentage>=1 || percentage <=0){
			return;
		}
		// 随机排序
		Collections.shuffle(totalList);
		int trainNum = Math.round(totalList.size() * percentage);
		trainList.addAll(totalList.subList(0, trainNum));
		testList.addAll(totalList.subList(trainNum, totalList.size()-1));
	}
	
	/**
	 * 
	 * 学习数据项
	 * @author CGD
	 * 
	 */
	public static class TrainItem {
		/** 图片文件 */
		private String file;
		/** 标签  */
		private int label;

		private TrainItem(String file, int label) {
			super();
			this.file = file;
			this.label = label;
		}

		public String getFile() {
			return file;
		}

		public void setFile(String file) {
			this.file = file;
		}

		public int getLabel() {
			return label;
		}

		public void setLabel(int label) {
			this.label = label;
		}
	}
	/**
	 * 测试统计项
	 * @author CGD
	 *
	 */
	public static class StatItem {
		/** 标签  */
		private int label=0;
		/** 测试总数量 */
		private int totalCount=0;
		/** 失败数 */
		private int failCount=0;
		
		private Vector<String> errorFile=new Vector<String>();
		
		public StatItem() {
			super();
		}
		
		public StatItem(int label) {
			super();
			this.label= label;
		}
		
		/**
		 * 结果匹配，true 匹配 ，false 不匹配 
		 * 相应统计数据变化
		 * @param match
		 */
		public void match(boolean match,String file){
			totalCount++;
			//不匹配
			if(!match){
				failCount++;
				errorFile.add(file);
			}
		}
		
		
		public int getLabel() {
			return label;
		}
		public void setLabel(int label) {
			this.label = label;
		}
		public int getTotalCount() {
			return totalCount;
		}
		public void setTotalCount(int totalCount) {
			this.totalCount = totalCount;
		}
		public int getFailCount() {
			return failCount;
		}
		public void setFailCount(int failCount) {
			this.failCount = failCount;
		}

		public Vector<String> getErrorFile() {
			return errorFile;
		}

		public void setErrorFile(Vector<String> errorFile) {
			this.errorFile = errorFile;
		}
		
	}

	
}
