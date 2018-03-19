package com.crenjoy.easypr.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.crenjoy.easypr.Config;
import com.crenjoy.easypr.test.PlateRecognizeTest;

public class ReTrain {

	private static final Log log = LogFactory.getLog(PlateRecognizeTest.class);

	private PlateRecognition pr;

	public ReTrain(String easyPath) {
		// 加载识别机器XML
		pr = new PlateRecognitionImpl(easyPath);
	}
	
	/**
	 * 从文件名中获取车牌
	 * @param fileName
	 * @return
	 */
	public List<String> plates(String fileName){
		// 图片中包含的车牌
		String[] carPlates = fileName.split("_");
		List<String> plates=new ArrayList<String>();
		for(String plate: carPlates){
			if (plate.length()<6){
				continue;
			}
			if (plate.indexOf("@")==-1){
				continue;
			}
			String province="zh_"+plate.substring(0,plate.indexOf("@"));
			//log.debug(province);
			if (Config.getProvinceMap().containsKey(province)){
				String p=Config.getProvinceMap().get(province)+plate.substring(plate.indexOf("@")+1);
			//	log.debug(p);
				plates.add(p);
			}			
		}
		return plates;
	}

	/**
	 * 替换文件名称
	 * @param platePath
	 * @throws IOException 
	 */
	public void replateFileName(String platePath) throws IOException{
		File files = new File(platePath);
		// 识别车牌
		for (File file : files.listFiles()) {
			if (file.isDirectory()) {
				continue;
			}
			String fileName =file.getAbsolutePath();
			String destName=fileName;
			for(Entry<String,String> entry:Config.getProvinceMap().entrySet()){
				String key=entry.getKey().replace("zh_", "")+"@";
				destName=destName.replace(entry.getValue(), key);
			}
		
			if (!StringUtils.equals(fileName, destName)){
				FileUtils.moveFile(new File(fileName), new File(destName));
			}
		}
	}
	
	/**
	 * 第一轮识别
	 * @param platePath 识别车牌
	 * @param destPath  未能识别的车牌
	 * @throws IOException
	 */
	public void recognize_1(String platePath, String destPath) throws IOException {
		log.debug("recognize 1 Start");
		File files = new File(platePath);
		// 识别车牌
		for (File file : files.listFiles()) {
			if (file.isDirectory()) {
				continue;
			}
			String fileName = file.getName();
			
			log.debug(file.getAbsolutePath());
			pr.setTrainPath(null);
			// 路径循环
			String absolutePath = file.getAbsolutePath();
			//
			List<String> carPlates=plates(fileName);
			
			// 车牌识别
			Map<String, Plate> ps = pr.plateRecognize(absolutePath);
			for (Plate p : ps.values()) {
				if (!carPlates.contains(p.getLicense())){
					FileUtils.copyFile(new File(file.getAbsolutePath()), new File(destPath + File.separatorChar + file.getName()));
				}
			}
			if (ps.size() < 1) {
				FileUtils.copyFile(new File(file.getAbsolutePath()), new File(destPath + File.separatorChar + file.getName()));
			}
		}
		log.debug("recognize 1 End");
	}
	
	
	/**
	 * 第二轮识别
	 * @param platePath  未正常识别车牌的路径
	 * @param trainPath  识别车牌中生成 车牌和字符路径
	 * @throws IOException
	 */
	public void recognize_2(String platePath, String trainPath) throws IOException {
		log.debug("recognize 2 Start");
		File files = new File(platePath);
		// 识别车牌
		for (File file : files.listFiles()) {
			if (file.isDirectory()) {
				continue;
			}
			String fileName = file.getName();
			
			log.debug(file.getAbsolutePath());
			pr.setTrainPath(trainPath);
			// 路径循环
			String absolutePath = file.getAbsolutePath();
			//
			List<String> carPlates=plates(fileName);
			
			// 车牌识别
			Map<String, Plate> ps = pr.plateRecognize(absolutePath);
		}
		log.debug("recognize 2 End");
	}
	


}
