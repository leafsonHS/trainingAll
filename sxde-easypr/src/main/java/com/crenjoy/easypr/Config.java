package com.crenjoy.easypr;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 *
 */
public class Config {

	/** 车牌字符 */
	public static final String kChars[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
	/* 10 */
	"A", "B", "C", "D", "E", "F", "G", "H", /* {"I", "I"} */
	"J", "K", "L", "M", "N", /* {"O", "O"} */
	"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
	/* 24 */
	"zh_cuan", "zh_e", "zh_gan", "zh_gan1", "zh_gui", "zh_gui1", "zh_hei", "zh_hu", "zh_ji", "zh_jin", "zh_jing", "zh_jl", "zh_liao", "zh_lu", "zh_meng",
			"zh_min", "zh_ning", "zh_qing", "zh_qiong", "zh_shan", "zh_su", "zh_sx", "zh_wan", "zh_xiang", "zh_xin", "zh_yu", "zh_yu1", "zh_yue", "zh_yun",
			"zh_zang", "zh_zhe"
	/* 31 */
	};

	/** 总识别字符数 数字、字母、省份  */
	public static final int kCharsTotalNumber = 65;
	
    /** 像素  */
	public static final int kPredictSize = 10; //10

	/** 省份字符名称对应Map */
	private static final Map<String, String> provinceMap = new HashMap<String, String>();

	public  static Map<String, String> getProvinceMap() {
		if (provinceMap.isEmpty()) {
			provinceMap.put("zh_cuan", "川");
			provinceMap.put("zh_e", "鄂");
			provinceMap.put("zh_gan", "赣");
			provinceMap.put("zh_gan1", "甘");
			provinceMap.put("zh_gui", "贵");
			provinceMap.put("zh_gui1", "桂");
			provinceMap.put("zh_hei", "黑");
			provinceMap.put("zh_hu", "沪");
			provinceMap.put("zh_ji", "冀");
			provinceMap.put("zh_jin", "津");
			provinceMap.put("zh_jing", "京");
			provinceMap.put("zh_jl", "吉");
			provinceMap.put("zh_liao", "辽");
			provinceMap.put("zh_lu", "鲁");
			provinceMap.put("zh_meng", "蒙");
			provinceMap.put("zh_min", "闽");
			provinceMap.put("zh_ning", "宁");
			provinceMap.put("zh_qing", "青");
			provinceMap.put("zh_qiong", "琼");
			provinceMap.put("zh_shan", "陕");
			provinceMap.put("zh_su", "苏");
			provinceMap.put("zh_sx", "晋");
			provinceMap.put("zh_wan", "皖");
			provinceMap.put("zh_xiang", "湘");
			provinceMap.put("zh_xin", "新");
			provinceMap.put("zh_yu", "豫");
			provinceMap.put("zh_yu1", "渝");
			provinceMap.put("zh_yue", "粤");
			provinceMap.put("zh_yun", "云");
			provinceMap.put("zh_zang", "藏");
			provinceMap.put("zh_zhe", "浙");
		}
		return provinceMap;
	}

}
