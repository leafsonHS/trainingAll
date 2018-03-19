package com.crenjoy.easypr;

import java.util.Vector;

/**
 * 对象指针
 * 
 * @author CGD
 *
 * @param <T>
 */
public class ObjectPointer<T> {
	
	private Vector<T> obj=new Vector<T>();
	
	public ObjectPointer(T... array){
		for(T t :array){
			obj.add(t);
		}
	}
	
	public void put(int i,T t){
		obj.add(i, t);
	}
	
	public void put(T t){
		obj.add(0, t);
	}
	
	public T get(int i){
		return obj.get(i);
	}
	
	public T get(){
		return obj.get(0);
	}
	

}
