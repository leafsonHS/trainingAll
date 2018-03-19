package com.crenjoy.easypr.enums;

/**
 * 车牌颜色
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 * 
 */
public enum PlateColor {
	

	BLUE("蓝牌",100,140), 
	
	YELLOW("黄牌",15,40), 
	
	WHITE("白牌",0,30),
	
	UNKNOWN("未识别",0,0);

	private String chName;
	
	// 不同颜色的H值范围
	private int  minH;
	
	private int  maxH;
	

	private PlateColor(String chName,int minH,int maxH) {
		this.chName = chName;
		this.minH = minH;
		this.maxH = maxH;
	}

	public String getChName() {
		return chName;
	}

	public int getMinH() {
		return minH;
	}

	public int getMaxH() {
		return maxH;
	}
	
	

}
