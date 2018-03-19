package com.crenjoy.easypr.preproccess;

import java.io.File;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.crenjoy.easypr.Config;
import com.crenjoy.easypr.train.SvmTrainImpl;
import com.crenjoy.easypr.util.Utils;

/**
 * 学习数据预处理
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public class PreTrainData {
	
	private static Log log = LogFactory.getLog(SvmTrainImpl.class);

	/**
	 * 重新命名SVM 下文件的文件名
	 * @param svmFolder
	 */
	public static void svmRenameFile(String svmFolder){
		log.debug("Process Rename SVM FileName start ...");
		Utils.renameFile(svmFolder+File.separatorChar+"has","t_has_1");
		Utils.renameFile(svmFolder+File.separatorChar+"no","t_no_0");
		log.debug("Process Rename SVM FileName Finish.");
	}
	
	/**
	 * 重新命名ANN 下文件的文件名
	 * @param annFolder
	 */
	public static void annRenameFile(String annFolder){
		log.debug("Process Rename ANN FileName start ...");
		for (int i = 0; i < Config.kCharsTotalNumber; i++) {
			String char_key = Config.kChars[i];
			Utils.renameFile(annFolder+File.separatorChar+char_key,char_key);
		}
		log.debug("Process Rename ANN FileName Finish.");
	}
	
	/**
	 * 识别失败的文件处理
	 * @param failFile
	 */
	public static void failFileProc(Vector<String> failFiles){
		for(String file : failFiles){
			String dest=FilenameUtils.getFullPathNoEndSeparator(file)+"_fail"+File.separatorChar+FilenameUtils.getName(file);
			Utils.mkdirsFile(dest);
			(new File(file)).renameTo(new File(dest));
			log.debug(String.format("Move %s to %s", file,dest));
		}
	}
	
	/**
	 * 识别失败的文件处理
	 * @param failFile
	 */
	public static void failFilePrint(Vector<String> failFiles){
		for(String file : failFiles){
			log.debug(String.format("Error File: %s", file));
		}
	}
	
	
	public static void main(String[] args){
		String svmFolder="D:\\test\\easypr\\resources\\train\\svm";
		svmRenameFile(svmFolder);
//		String annFolder="D:\\test\\easypr\\resources\\train\\ann";
//		annRenameFile(annFolder);
		
	}
}
