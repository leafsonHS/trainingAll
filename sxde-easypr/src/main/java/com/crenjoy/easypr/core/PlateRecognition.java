package com.crenjoy.easypr.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.Mat;

import com.crenjoy.easypr.util.OpenCVUtils;
import com.crenjoy.easypr.util.PlateTestUtils;
import com.crenjoy.easypr.util.Utils;

/**
 * 车牌识别
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public abstract class PlateRecognition {
	
	private static final Log log = LogFactory.getLog(PlateRecognition.class);
	/**
	 * 获取车牌
	 * @param image
	 * @return
	 */
	public  Map<String,Plate> plateRecognize(String  image) {
		
		Mat src=OpenCVUtils.imread(image);
		return plateRecognize(src);
	}
	/**
	 * 车牌检测与字符识别
	 * @param src
	 * @return 返回已识别多个车牌【号码,颜色】的Map
	 */
	public  Map<String,Plate> plateRecognize(Mat src) {
		//获取定位的车牌
		//long p= (new Date()).getTime();
		Vector<Plate> plateVec=locate(src);
		//PlateTestUtils.imwrite(plateVec, p+"_locate_train");
		//判断是否为车牌
		plateVec=judgePlate(plateVec);
	    //PlateTestUtils.imwrite(plateVec, "has_locate");
		
		//根据车牌出现次数排序
		Map<String,Plate> licenseMap =new HashMap<String,Plate>();
		//逐一识别
		for(Plate plate : plateVec){
			Vector<Mat> blocks=segmentPlate(plate.getPlateMat());
			//PlateTestUtils.imwriteVector(blocks, p+"_block_train");
			String license=recognisePlate(blocks);
			if (StringUtils.isEmpty(license)){
				continue;
			}
			plate.setLicense(license);
			if (!licenseMap.containsKey(license)){
				licenseMap.put(license, plate);
			}else{
				//车牌出现次数
				Plate temp=licenseMap.get(license);
				temp.setQuantity(temp.getQuantity()+1);
			}
			log.debug(plate.toString());
		}
		
		//排序
		licenseMap=Utils.sortMapByValue(licenseMap);
		
		// 完整识别过程到此结束
		if (PlateTestUtils.isDebug()) {
		    //	Mat result=OpenCVUtils.markMat(src, plateVec);
			// 显示定位框的图片
			//PlateTestUtils.imwrite(result, "finally");
		}

		
		return licenseMap;
	}
	
	/**
	 * 获取通过颜色、Sobel定位的车牌
	 * @param src
	 * @return
	 */
	public Vector<Plate> locate(Mat src){
		Vector<Plate> allPlate=new Vector<Plate>();
		allPlate.addAll(locateByColor(src));
		allPlate.addAll(locateBySobel(src));
		return allPlate;
	}
	

	/**
	 * 通过车牌颜色定位图片中的车牌
	 * @param src
	 * @return
	 */
	public abstract Vector<Plate> locateByColor(Mat src);
	/**
	 * 通过Sobel 定位图片中的车牌
	 * @param src
	 * @return
	 */
	public abstract Vector<Plate> locateBySobel(Mat src);
	
	/**
	 * 通过SVM判断是否是车牌
	 * @param src
	 * @return
	 */
	public abstract boolean judgePlate(Mat src);
	/**
	 * 判断是否是车牌，或是图片中间含有车牌
	 * 返回识别为车牌的图片
	 * @param srcVec
	 * @return
	 */
	public abstract Vector<Plate>  judgePlate(Vector<Plate> srcVec);
	
	/**
	 * 分割车牌
	 * @param src
	 * @return
	 */
	public abstract Vector<Mat> segmentPlate(Mat src);
	
	/**
	 * 通过分割字符识别车牌号码
	 * @param srcVec
	 * @return
	 */
	public abstract String recognisePlate(Vector<Mat> srcVec);
	
	
	/**
	 * 重新学习路径 如设置保存重新训练数据
	 * 
	 * @param trainPath
	 */
	public abstract void setTrainPath(String trainPath);
	
	
}
