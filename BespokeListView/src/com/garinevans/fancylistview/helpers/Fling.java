package com.garinevans.fancylistview.helpers;

import android.view.animation.AnimationUtils;

/**
 * @author garinevans
 * Handles the fling dynamics
 */
public class Fling{

	/** the current fling velocity **/
	private float velocity;
	
	/** what time did the fling start? **/
	private long startTime;
	
	/** how much to allow leeway for a bounce **/
	private int bounceOffset;
	
	/** the current scroll status **/
	private int status = STATUS_SCROLLING;
	
	/** how much have we overflowed by **/
	private int overflowAmount = 0;
	
	/** the bottom overflow amount that we started with **/
	private int bottomOverflowStartAmount = 0;
	
	/** the time the overflow correction takes **/
	private final static int OVERFLOW_CORRECTION_INTERVAL = 10;

	/** we're scrolling **/
	public final static int STATUS_SCROLLING = 0;
	
	/** we're done flinging **/
	public final static int STATUS_FINISHED = 1;
	
	/** we've exceeded the overflow and are bouncing back **/
	public final static int STATUS_BOUNCE_TOP = 2;
	
	/** we've exceeded the overflow and are bouncing back **/
	public final static int STATUS_BOUNCE_BOTTOM = 3;
	
	/** a higher friction causes a slower stoppage time **/
	private static final float FRICTION = 0.75f;
	
	/**
	 * @param velocty - the current velocity
	 * @param now - the current time in milliseconds
	 * @param bounceOffset - how much to spring distance to allow
	 */
	public Fling(float velocty, long now, int bounceOffset){
		velocity = velocty;
		this.startTime = now;
		this.bounceOffset = bounceOffset;
	}
	
	/**
	 * @return the current status
	 */
	public int getStatus(){
		return status;
	}
	
	/**
	 * @param current top position
	 * @return updated top position
	 */
	public int getUpdatedTopPosition(int top){
		long now = AnimationUtils.currentAnimationTimeMillis();
		int overflowCorrectionIncrement = 0;
		
		switch(getStatus()){ 
			case STATUS_SCROLLING:
				top += getDistanceTravelled(now);
				
				if(velocity == 0) status = STATUS_FINISHED;
				
				//check if we're overflowing
				if(top - bounceOffset > 0){
					status = STATUS_BOUNCE_TOP;
					
					overflowAmount = top;
				}
				
				break;
				
			case STATUS_BOUNCE_TOP:
				overflowCorrectionIncrement = overflowAmount / OVERFLOW_CORRECTION_INTERVAL;
				overflowAmount -= overflowCorrectionIncrement;
				
				if(top - overflowCorrectionIncrement < 0)
					top = 0;
				else
					top -= overflowCorrectionIncrement;
				
				if(top <= 0) status = STATUS_FINISHED;
				
				break;
				
			case STATUS_BOUNCE_BOTTOM:
				overflowCorrectionIncrement = overflowAmount / OVERFLOW_CORRECTION_INTERVAL;
				
				if(bottomOverflowStartAmount - overflowCorrectionIncrement < 0){
					int overkill = bottomOverflowStartAmount - overflowCorrectionIncrement;
					overflowCorrectionIncrement -= overkill;
				}
				
				overflowAmount -= overflowCorrectionIncrement;
				
				top += overflowCorrectionIncrement;
				if(overflowCorrectionIncrement <= 0)  {
					status = STATUS_FINISHED;
				}
				
				break;
		}
		
		return top;
		
	}
	
	/**
	 * @return the overflow amount
	 */
	public int getOverflowAmount(){
		return overflowAmount;
	}
	
	/**
	 * how far have we travelled
	 * @param now - current time in milliseconds
	 * @return distance traveled
	 */
	private int getDistanceTravelled(long now){
		long dt = (now - startTime);
		
		float distance = velocity * dt / 2000;
		
		velocity *= FRICTION;
		
		return (int)distance;
	}
	
	/**
	 * @return true if we're not in the middle of something that can't be stopped, false otherwise
	 */
	public boolean canBeRemoved(){
		return true;
	}
	
	/**
	 * Puts us in bottom overflow mode.
	 * @param overflow
	 */
	public void setBottomOverflowAmount(int overflow){
		if(getStatus() == STATUS_BOUNCE_BOTTOM)
			return;
			
		bottomOverflowStartAmount = overflow;
		overflowAmount = overflow;
		status = STATUS_BOUNCE_BOTTOM;
	}
	
}
