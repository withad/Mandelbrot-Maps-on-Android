package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

public class FractalActivity extends Activity implements OnTouchListener, OnScaleGestureListener {
   private static final String TAG = "MMaps";
   private static final int INVALID_POINTER_ID = -1;

   private MandelbrotFractalView fractalView;
   private MandelbrotJuliaLocation mjLocation;
   
   private float dragLastX;
   private float dragLastY;
   
   private ScaleGestureDetector gestureDetector;
   
   private boolean draggingFractal = false;
   
   private int dragID = INVALID_POINTER_ID;
   

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");
      
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

      fractalView = new MandelbrotFractalView(this);
      setContentView(fractalView);
      fractalView.requestFocus();
      
      mjLocation = new MandelbrotJuliaLocation();
      fractalView.loadLocation(mjLocation);
      
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
      finish();
      Log.d(TAG, "onPause");
   }
   
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.navigationmenu, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.ZoomOut:
    	  fractalView.zoomChange((int)(fractalView.getWidth()/2), (int)(fractalView.getHeight()/2), 1);
    	  return true;
      case R.id.ZoomIn:
    	  fractalView.zoomChange((int)(fractalView.getWidth()/2), (int)(fractalView.getHeight()/2), -1);
    	  return true;
      case R.id.PanUp:
    	  fractalView.shiftPixels(0, -100);
    	  fractalView.moveFractal(0, -100);
    	  return true;
      case R.id.PanDown:
    	  fractalView.shiftPixels(0, 100);
    	  fractalView.moveFractal(0, 100);
    	  return true;
      case R.id.PanLeft:
    	  fractalView.moveFractal(100, 0);
    	  return true;
      case R.id.PanRight:
    	  fractalView.moveFractal(-100, 0);
    	  return true;
      case R.id.resetFractal:
    	  fractalView.reset();
    	  return true;
      }
      return false;
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
			if(!draggingFractal)
			{
				fractalView.startDragging();
				draggingFractal = true;
			}
			
			
			if(!gestureDetector.isInProgress() && dragID != INVALID_POINTER_ID)
			{
				int pointerIndex = evt.findPointerIndex(dragID);
				
				float dragDiffPixelsX = evt.getX(pointerIndex) - dragLastX;
				float dragDiffPixelsY = evt.getY(pointerIndex) - dragLastY;
		
				// Move the canvas
				if (dragDiffPixelsX != 0.0f && dragDiffPixelsY != 0.0f)
					fractalView.dragFractal(dragDiffPixelsX, dragDiffPixelsY);
				
				Log.d(TAG, "Diff pixels X: " + dragDiffPixelsX);
		
				// Update last mouse position
				dragLastX = evt.getX(pointerIndex);
				dragLastY = evt.getY(pointerIndex);
				return true;
			}
			return false;
			
		case MotionEvent.ACTION_POINTER_UP:
			Log.d(TAG, "Pointer count: " + evt.getPointerCount());
			if(evt.getPointerCount() == 1) break;
			
			// Extract the index of the pointer that came up
	        final int pointerIndex = (evt.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	        final int pointerId = evt.getPointerId(pointerIndex);
	        
	        fractalView.stopZooming();
	        
	        dragLastX = (int) evt.getX(dragID);
			dragLastY = (int) evt.getY(dragID);
	        
	        if (pointerId == dragID) {
	            Log.d(TAG, "Choosing new active pointer");
	            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
	            dragLastX = (int) evt.getX(newPointerIndex);
				dragLastY = (int) evt.getY(newPointerIndex);
	            dragID = evt.getPointerId(newPointerIndex);
	        }
	        
	        break;
	        
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "ACTION_UP");
			//if(!gestureDetector.isInProgress())
			{
				draggingFractal = false;
				fractalView.stopDragging();
			}
			break;
	}
	return true;
}


public boolean onScaleBegin(ScaleGestureDetector detector) {
	Log.d(TAG, "Start of zoom");
	fractalView.startZooming();
	return true;
}


public boolean onScale(ScaleGestureDetector detector) {
	//DEBUG CODE
	//fractalView.pauseRendering = true;
	if(gestureDetector.getScaleFactor() == 0 || gestureDetector.getScaleFactor() == 1)
		return false;
	//DEBUG CODE
	
	fractalView.zoomImage(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
	
	return true;
}


public void onScaleEnd(ScaleGestureDetector detector) {
	// TODO Auto-generated method stub
	
}
}
