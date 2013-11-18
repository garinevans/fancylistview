package com.garinevans.fancylistview.helpers;

/**
 * @author garinevans
 * Touch Position Helper
 */
public class TouchEventHelper{
	
	private float y;
	private float x;
	private int scrollTop;
	
	/**
	 * @param x
	 * @param y
	 * @param scrollTop
	 */
	public TouchEventHelper(float x, float y, int scrollTop){
		this.x = x;
		this.y = y;
		this.scrollTop = scrollTop;
	}
	
	/**
	 * @return y position
	 */
	public float getY(){
		return y;
	}
	
	/**
	 * @return x position
	 */
	public float getX(){
		return x;
	}
	
	/**
	 * @return the scroll top position
	 */
	public int getScrollTop(){
		return scrollTop;
	}
	
}
