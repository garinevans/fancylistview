package com.garinevans.fancylistview;

import java.util.LinkedList;

import com.garinevans.fancylistview.helpers.Fling;
import com.garinevans.fancylistview.helpers.TouchEventHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Adapter;

/**
 * @author garinevans
 * One fancy list view 
 */
public class FancyListView extends AdapterView<Adapter> {
	
	/** the adapter **/
	private Adapter mAdapter;
	
	/** The coordinates of the touch down event **/
	private TouchEventHelper mStartTouchPosition;
	
	/** The top of the list view. Any adjustment results in a scroll **/
	private int mTop = 0;
	
	/** how much have we overflowed by **/
	private int topOverflowAmount = 0;
	
	/** The minimum amount of distance that must be traveled to start a scroll **/
	private int mMinScrollDistance = 10;
	
	/**	tracks the velocity of swipes **/
	VelocityTracker velocityTracker;
	
	/** fling helper, used when the user swipes **/
	Fling fling;
	
	/** our fling action - runs the fling code **/
	Runnable flingAction;
	
	/** touch mode **/
	int touchMode = TOUCH_MODE_DORMANT;
	
	/** no current touch events **/
	private static final int TOUCH_MODE_DORMANT = 0;
	
	/** we're scrolling	**/
	private static final int TOUCH_MODE_SCROLLING = 1;
	
	/** the index of the first visible item in the adapter **/
	private int firstItemInAdapterPosition = 0;
	
	/** the index of the last visible item in the adapter **/
	private int lastItemInAdapterPosition = 0;
	
	/** when removing views from the top of the list we need to apply an offset so that the views below it remain in the same position they were in originally **/
	private int listTopOffset = 0;
	
	public FancyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public View getSelectedView() {
		//TODO: implement this
		return null;
	}
	
	@Override
	public void setSelection(int position) {
		//TODO: implement this
	}

	@Override
	public void setAdapter(Adapter adapter) {
		mAdapter = adapter;
		removeAllViewsInLayout();
        requestLayout();
	}

	/** The amount that the list will overflow by before springing back **/
	private final static int SPRING_DISTANCE = 10;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(flingAction!=null && fling.canBeRemoved()) removeCallbacks(flingAction);
		
		switch(event.getAction()){
			case  MotionEvent.ACTION_DOWN:
				touchMode = TOUCH_MODE_DORMANT;
				mStartTouchPosition = new TouchEventHelper(event.getX(), event.getY(), mTop);
				velocityTracker = VelocityTracker.obtain();
				velocityTracker.addMovement(event);
				break;
				
			case MotionEvent.ACTION_MOVE:
				TouchEventHelper currentPosition = new TouchEventHelper(event.getX(), event.getY(), mTop);
				float scrollDistance = mStartTouchPosition.getY() - currentPosition.getY();
				boolean isScrolling = scrollDistance > mMinScrollDistance || scrollDistance < -mMinScrollDistance;
				
				if(touchMode == TOUCH_MODE_SCROLLING || isScrolling){
					//we're scrolling
					mTop = mStartTouchPosition.getScrollTop() - (int)scrollDistance;
					if(mTop > 0) topOverflowAmount = mTop;
					
					touchMode = TOUCH_MODE_SCROLLING;
				}
				
				velocityTracker.addMovement(event);
				
				requestLayout();
				
				break;
				
			case MotionEvent.ACTION_UP:
				velocityTracker.addMovement(event);
				velocityTracker.computeCurrentVelocity(1000);
				float velocity = VelocityTrackerCompat.getYVelocity(velocityTracker, event.getActionIndex());
				
				fling = new Fling(velocity, AnimationUtils.currentAnimationTimeMillis(), SPRING_DISTANCE);
				
				velocityTracker.recycle();
				velocityTracker = null;
				
				if(touchMode == TOUCH_MODE_SCROLLING){

					flingAction = new Runnable(){
	
						@Override
						public void run() {
							
							mTop = fling.getUpdatedTopPosition(mTop);
							topOverflowAmount = 0;
							if(fling.getStatus() != Fling.STATUS_BOUNCE_BOTTOM){
								topOverflowAmount = fling.getOverflowAmount();
							}
							
							requestLayout();
							
							//are we done? if not lets go again
							if(fling.getStatus() != Fling.STATUS_FINISHED)
								postDelayed(this, 16);
						}
						
					};
					
					post(flingAction);
				}
				
				touchMode = TOUCH_MODE_DORMANT;
				
				break;
		}
		
		return true;
	}
	
	LinkedList<View> cachedViews = new LinkedList<View>();
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		if(mAdapter==null)
			return;
		
		removeHiddenViews();
		
		if(getChildCount() == 0){
			fillList();
		}else{
			updateList();
		}
		
		positionItems();
		
		invalidate();
	}
	
	/**
	 * removes hidden views (views that are out of the bounds of the screen)
	 */
	private void removeHiddenViews(){
		
		int childCount = getChildCount();
		if(childCount == 0)
			return;
		
		boolean reachedVisibleViews = false;
		//check the first items in the list and remove them if they're above the top fold
		while(childCount > 1 && reachedVisibleViews == false){
			View child = getChildAt(0);
			
			if(child.getBottom() < 0){
				//this child is beyond the top of the screen, remove it from the layout
				
				//the problem here is that by removing the first child, the next in line becomes first, and will instantly
				//be removed because it assumes the first items position and falls into this bit of code. Therefore, removing
				//the first item will subsequently remove all other items until there are no more views in the layout. To 
				//account for this we set an offset to preserve the location of the items minus the original view at position 0
				listTopOffset += child.getHeight();
				removeViewInLayout(child);
				childCount --;
				firstItemInAdapterPosition ++;
			}else{
				//we've reached a visible view, stop here
				reachedVisibleViews = true;
			}
		}
		
		if(childCount <= 1)
			return;
		
		//do the same but from the bottom (you could just loop through all views and check but this is more efficient
		lastItemInAdapterPosition = firstItemInAdapterPosition + childCount;
		
		int index = childCount - 1;
		reachedVisibleViews = false;
		while(index >= 0 && reachedVisibleViews == false){
			View child = getChildAt(index);
			
			//I've cheated a little here. Add the child's height to the height of the view before checking against TOP. 
			//This way we have one child "over" the bottom of the screen and so scrolling looks more fluid - I was seeing
			//some weird glitchy rendering without doing this, but if anyone can spot the problem I'd appreciate it.
			if(child.getTop() > getHeight() + child.getHeight()){
				//this child is beyond the top of the screen
				removeViewInLayout(child);
				childCount --;
				lastItemInAdapterPosition --;
			}else{
				//we've reached a visible view, stop here
				reachedVisibleViews = true;
			}
			
			index --;
		}
	}
	
	/**
	 * initializes the list. Fills from top to bottom.
	 */
	private void fillList(){
		int index = 0;
		boolean noMoreVisibleItems = false;
		
		int totalHeight = 0;
		while(index < mAdapter.getCount() && noMoreVisibleItems == false){
			View view = mAdapter.getView(index, null, this);
		
			totalHeight += addViewToLayout(false, view);
			if(totalHeight > getHeight())
				noMoreVisibleItems = true;
			
			index ++;
		}
	}
	
	/**
	 * updates the list. adds views to the layout if they have become visible 
	 */
	private void updateList(){
		int currentTop = getTopWithOffset();
		
		//add any views that have become visible at the top of the list
		while(firstItemInAdapterPosition > 0 && currentTop > 0){
			firstItemInAdapterPosition --;
			
			View view = mAdapter.getView(firstItemInAdapterPosition, null, this);
			
			int viewHeight = addViewToLayout(true, view);
			currentTop -= viewHeight;
			listTopOffset -= viewHeight;
		}
		
		View lastChild = getChildAt(getChildCount() - 1);
		currentTop = lastChild.getTop();
		
		//add any views that have become visible at the bottom of the list
		while(lastItemInAdapterPosition < mAdapter.getCount() && currentTop <= getHeight()){
			View view = mAdapter.getView(lastItemInAdapterPosition, null, this);
			
			//TODO: this is very jumpy - something wrong with the maths
			Log.d("bottom", String.format("bottom %d %d %d", currentTop, getHeight(), lastChild.getHeight()));
			
			int viewHeight = addViewToLayout(false, view);
			currentTop += viewHeight;
			
			lastItemInAdapterPosition ++;
		}
	}
	
	/**
	 * Adds a view to the layout
	 * @param addAbove
	 * @param view
	 * @return height of view
	 */
	private int addViewToLayout(boolean addAbove, View view){
		int parentWidth = getWidth();
		
		view.measure(MeasureSpec.EXACTLY | parentWidth, MeasureSpec.UNSPECIFIED);
		
		LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
		 
        view.setDrawingCacheEnabled(true);
        int location = addAbove ? 0 : -1;
		addViewInLayout(view, location, params, true);
		
		int height = view.getMeasuredHeight();
		
		return height;
	}
	
	/**
	 * Loops through each view in the layout and positions it accordingly
	 */
	private void positionItems(){
		int top = getTopWithOffset();
		
		for(int index = 0; index < getChildCount(); index ++){
			View view = getChildAt(index);
			
			final int width = view.getMeasuredWidth();
            final int height = view.getMeasuredHeight();
            final int left = (getWidth() - width) / 2;
            
            view.layout(left, top, left + width, top + height);
            
            if(index == getChildCount() - 1){
            	//check if we're overflowing at the bottom of the list
				if(view.getBottom() < getHeight()) {
					//we're overflowing from the bottom
					if(fling != null) {
						fling.setBottomOverflowAmount(getHeight() - view.getBottom());
					}
				}
            }
            
            top += height;
		}
	}
	
	/**
	 * @return the top plus any offset
	 */
	private int getTopWithOffset(){
		return mTop + listTopOffset;
	}
	
	Matrix mMatrix;
    Camera mCamera;
    Paint mPaint;
    
	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		View topChild = getChildAt(0);
		
		boolean thisIsFirstItem = false;
		int top = child.getTop();
		if(topChild.getTop() == top)
        	thisIsFirstItem  = true;
        
        if(topOverflowAmount != 0 && thisIsFirstItem){
        	final Bitmap bitmap = child.getDrawingCache();
    		
    		// get top left coordinates
            int left = child.getLeft();

            // get offset to center
            int centerX = child.getWidth() / 2;
        	
        	float height = child.getHeight();
        	
        	/*
        	 * calculate the angle to rotate by and the amount to adjust the plane 
        	 * along its z and y axis to account for the rotation
        	 */
        	float adjacent = topOverflowAmount;
        	float rotation = (float) Math.toDegrees(Math.acos(adjacent / height));
        	float opposite = (float) (height * Math.sin(Math.toRadians(rotation)));

        	//the amount to adjust the Y pos by to account for the rotation
        	float adjustmentY = (height - adjacent) / 2;
        	
        	if(rotation < 0 || Float.isNaN(rotation)){
        		//plane is vertical, don't rotate any further
        		rotation = 0;
    			opposite = 0;
    			//cling to the top of the item below it.
    			adjustmentY = top - height;
        	}
        		
        	//draw the folding plane
        	drawAndRotateChild(canvas, child, adjacent / 2, centerX, left, 0, bitmap, rotation, opposite, adjustmentY);
        }
        	
        //draw the standard item
        return super.drawChild(canvas, child, drawingTime);
	}
	
	/**
	 * draw the child 
	 * @param canvas
	 * @param child
	 * @param centerY
	 * @param centerX
	 * @param left
	 * @param top
	 * @param bitmap
	 * @param rotation
	 * @param z
	 * @param adjustmentY
	 */
	private void drawAndRotateChild(Canvas canvas, View child, float centerY, int centerX, int left, float top, Bitmap bitmap, float rotation, float z, float adjustmentY){
		// calculate scale and rotation
		canvas.save();
		
		// create the camera if we haven't before
        if (mCamera == null) {
            mCamera = new Camera();
        }

        // save the camera state
        mCamera.save();

        // translate and then rotate the camera
        mCamera.translate(0, 0, z);
        mCamera.rotateX(rotation);
        //re-translate z and apply an adjustment (z/2) to account for the rotation
        mCamera.translate(0, 0, -z + (z / 2));

        // create the matrix if we haven't before
        if (mMatrix == null) {
            mMatrix = new Matrix();
        }

        // get the matrix from the camera and then restore the camera
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        // translate and scale the matrix
        mMatrix.preTranslate(-centerX, -centerY);
        //apply an adjustment along the y axis to account for the rotation
        mMatrix.postTranslate(left + centerX, top + centerY + adjustmentY);

        // create and initialize the paint object
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setFilterBitmap(true);
        }

        // set the light
        mPaint.setAlpha(0xFF - (int)(2 * Math.abs(rotation)));

        // draw the bitmap
        canvas.drawBitmap(bitmap, mMatrix, mPaint);
	}
	
	
	
	/**
	 * @author garinevans
	 * Helper class for adding and measuring views
	 */
	protected class AddAndMeasureResult{
		
		private boolean viewAdded;
		private boolean heightExceeded;
		private int viewHeight;
		
		/**
		 * @param viewAdded
		 * @param child
		 * @param heightExceeded
		 */
		public AddAndMeasureResult(boolean viewAdded, int viewHeight, boolean heightExceeded){
			this.viewAdded = viewAdded;
			this.viewHeight = viewHeight;
			this.heightExceeded = heightExceeded;
		}
		
		/**
		 * @return true if the view was successfully added, false otherwise
		 */
		public boolean isViewAdded(){
			return viewAdded;
		}
		
		
		/**
		 * @return true if the height was exceeded, false otherwise
		 */
		public boolean isHeightExceeded(){
			return heightExceeded;
		}
		
		/**
		 * @return the height of the view that was added
		 */
		public int getViewHeight(){
			return viewHeight;
		}
		
	}
	
}
