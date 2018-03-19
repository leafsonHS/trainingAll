package com.crenjoy.easypr.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.LogFactory;

import com.crenjoy.easypr.core.Plate;

/**
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public class Utils {

	/**
	 * 重命名文件夹下全部文件
	 * 
	 * @param folder
	 * @param name
	 */
	public static void renameFile(String folder, String name) {
		File folderFile = new File(folder);
		if (!folderFile.exists() || !folderFile.isDirectory()) {
			return;
		}
		File[] files = folderFile.listFiles();
		if (null==files){
			return;
		}
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.exists() && file.isFile()) {
				String dest = FilenameUtils.getFullPath(file.getAbsolutePath());
				dest = String.format("%s%s_%d.jpg", dest, name, i);
				file.renameTo(new File(dest));
			}
		}
	}

	/**
	 * 拼接文件夹
	 * 
	 * @param folder
	 * @param filename
	 * @return
	 */
	public static String splitPath(String folder, String filename) {
		return folder + filename.replace('/', File.separatorChar);
	}

	/**
	 * 保存文件前，创建保存的文件夹
	 * 
	 * @param fileName
	 */
	public static void mkdirsFile(String fileName) {
		File dirFile = new File(FilenameUtils.getFullPath(fileName));
		if (!dirFile.exists()) {
			dirFile.mkdirs();
		}
	}

	/**
	 * 拷贝文件
	 * 
	 * @param clazz
	 * @param src
	 *            包内文件相对路径
	 * @param fileName
	 *            目标文件
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static boolean copyFile(Class clazz, String src, String fileName) {
		InputStream in = null;
		FileOutputStream writer = null;

		File extractedLibFile = new File(fileName);

		try {
			// 创建文件夹
			String filePath = FilenameUtils.getFullPath(fileName);
			if (!(new File(filePath)).exists()) {
				(new File(filePath)).mkdirs();
			}

			// 拷贝
			in = clazz.getResourceAsStream(src);
			if (in == null) {
				return false;
			}
			BufferedInputStream reader = new BufferedInputStream(in);
			writer = new FileOutputStream(extractedLibFile);

			byte[] buffer = new byte[1024];

			while (reader.read(buffer) > 0) {
				writer.write(buffer);
				buffer = new byte[1024];
			}
		} catch (IOException e) {
			LogFactory.getLog(Utils.class).error(e.getMessage());
			return false;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				LogFactory.getLog(Utils.class).error(e.getMessage());
			}
		}

		LogFactory.getLog(Utils.class).info(String.format("%s copy to %s", src, fileName));
		return true;
	}
	
	/**
	 * 车牌排序
	 * @param oriMap
	 * @return
	 */
	public static Map<String, Plate> sortMapByValue(Map<String, Plate> oriMap) {
		Map<String, Plate> sortedMap = new LinkedHashMap<String, Plate>();
		if (oriMap != null && !oriMap.isEmpty()) {
			List<Map.Entry<String, Plate>> entryList = new ArrayList<Map.Entry<String, Plate>>(oriMap.entrySet());
			Collections.sort(entryList,
					new Comparator<Map.Entry<String, Plate>>() {
						public int compare(Entry<String, Plate> entry1,
								Entry<String, Plate> entry2) {
							int result=entry1.getValue().getQuantity()-entry2.getValue().getQuantity();
							if (result>0){
								return 1;
							}
							if (result<0){
								return -1;
							}
							return 0;
						}
					});
			Iterator<Map.Entry<String, Plate>> iter = entryList.iterator();
			Map.Entry<String, Plate> tmpEntry = null;
			while (iter.hasNext()) {
				tmpEntry = iter.next();
				sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
			}
		}
		return sortedMap;
	}
}
