package com.crenjoy.easypr.core;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import com.crenjoy.easypr.enums.LocateType;
import com.crenjoy.easypr.enums.PlateColor;

/**
 * 车牌
 * 基于EasyPR 1.4 C++版本改写，参考EasyPR1.1 JAVA版本
 * @author CGD
 * 
 */
public class Plate {

	private boolean bColored = true;
	/** 车牌的图块 */
	private Mat m_plateMat;

	/** 车牌在原图的位置 */
	private RotatedRect m_platePos;

	/** 车牌字符串 */
	private String m_license;

	/** 车牌定位的方法 */
	private LocateType m_locateType;
	
	/** 车牌颜色 */
	private PlateColor m_plateColor;
	/** 识别相同车牌的次数 1-2 颜色识别 sobel识别  */
	private int quantity =1;
	
	
	

	@Override
	public String toString() {
		return "Plate [车牌号码=" + m_license + ", 车牌识别类型=" + m_locateType + ", 车牌颜色=" + m_plateColor + ", 识别数量=" + quantity + "]";
	}

	public boolean isColored() {
		return bColored;
	}

	public void setColored(boolean bColored) {
		this.bColored = bColored;
	}

	public Mat getPlateMat() {
		return m_plateMat;
	}

	public void setPlateMat(Mat m_plateMat) {
		this.m_plateMat = m_plateMat;
	}

	public RotatedRect getPlatePos() {
		return m_platePos;
	}

	public void setPlatePos(RotatedRect m_platePos) {
		this.m_platePos = m_platePos;
	}

	public String getLicense() {
		return m_license;
	}

	public void setLicense(String m_license) {
		this.m_license = m_license;
	}

	public LocateType getLocateType() {
		return m_locateType;
	}

	public void setLocateType(LocateType m_locateType) {
		this.m_locateType = m_locateType;
	}

	public PlateColor getPlateColor() {
		return m_plateColor;
	}

	public void setPlateColor(PlateColor m_plateColor) {
		this.m_plateColor = m_plateColor;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
	

}
