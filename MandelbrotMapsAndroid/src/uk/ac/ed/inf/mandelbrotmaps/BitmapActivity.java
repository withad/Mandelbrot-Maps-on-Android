package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

public class BitmapActivity extends Activity implements OnTouchListener, OnScaleGestureListener {
   private static final String TAG = "MMaps";
   private static final int INVALID_POINTER_ID = -1;

   private BitmapDraggingView bitmapView;
   
   private int dragLastX;
   private int dragLastY;
   
   private ScaleGestureDetector gestureDetector;
   
   private int dragID = INVALID_POINTER_ID;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      
      bitmapView = new BitmapDraggingView(this);
      setContentView(bitmapView);
      bitmapView.requestFocus();
      
      gestureDetector = new ScaleGestureDetector(this, this);
   }

   
   @Override
   protected void onResume() {
      super.onResume();
      Log.d(TAG, "onResume");
   }

   
   @Override
   protected void onPause() {
      super.onPause();
      Log.d(TAG, "onPause");
   }
   
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.navigationmenu, menu);
      return true;
   }


public boolean onTouch(View v, MotionEvent evt) {
	gestureDetector.onTouchEvent(evt);
	
	switch (evt.getAction() & MotionEvent.ACTION_MASK)
	{
		case MotionEvent.ACTION_DOWN:
			dragLastX = (int) evt.getX();
			dragLastY = (int) evt.getY();
			
			dragID = evt.getPointerId(0);	
			Log.d(TAG, "Initial dragID: " + dragID);
			return true;
			
		case MotionEvent.ACTION_MOVE:
			if(!gestureDetector.isInProgress() && dragID != INVALID_POINTER_ID)
			{
				int pointerIndex = evt.findPointerIndex(dragID);
				
				int dragDiffPixelsX = (int) (evt.getX(pointerIndex) - dragLastX);
				int dragDiffPixelsY = (int) (evt.getY(pointerIndex) - dragLastY);
		
				// Move the canvas
				bitmapView.dragCanvas(dragDiffPixelsX, dragDiffPixelsY);
		
				// Update last mouse position
				dragLastX = (int) evt.getX(pointerIndex);
				dragLastY = (int) evt.getY(pointerIndex);
				return true;
			}
			
		case MotionEvent.ACTION_POINTER_UP:
			// Extract the index of the pointer that came up
	        final int pointerIndex = (evt.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	        final int pointerId = evt.getPointerId(pointerIndex);
	        
	        if (pointerId == dragID) {
	            Log.d(TAG, "Choosing new active pointer");
	            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
	            dragLastX = (int) evt.getX(newPointerIndex);
				dragLastY = (int) evt.getY(newPointerIndex);
	            dragID = evt.getPointerId(newPointerIndex);
	        }
	        break;
	}
	return true;
}


public boolean onScale(ScaleGestureDetector detector) {
	if(gestureDetector.getScaleFactor() == 0)
		return false;
	bitmapView.midX = detector.getFocusX();
	bitmapView.midY = detector.getFocusY();
	
	bitmapView.scaleFactor = gestureDetector.getScaleFactor();
	bitmapView.invalidate();
	return true;
}


public boolean onScaleBegin(ScaleGestureDetector detector) {
	return true;
}


public void onScaleEnd(ScaleGestureDetector detector) {
	// TODO Auto-generated method stub
	
}
}
