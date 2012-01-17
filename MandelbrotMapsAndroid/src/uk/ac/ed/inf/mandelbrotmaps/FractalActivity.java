package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.ControlMode;

import android.app.Activity;
import android.content.Intent;
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

	private boolean currentlyDragging = false;
	
	private enum DisplayMode{
		MANDELBROT,
		ABOUT_TO_JULIA,
		JULIA
	}
	
	private DisplayMode displaymode = DisplayMode.MANDELBROT;
	
	private static final String TAG = "MMaps";
	
//	private MandelbrotFractalView fractalView;
//	private JuliaFractalView fractalView;
	private AbstractFractalView fractalView;
	private MandelbrotJuliaLocation mjLocation;
	   
	private float dragLastX;
	private float dragLastY;
	   
	private ScaleGestureDetector gestureDetector;
	   
	private int dragID = -1;
   

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");
      
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

      Bundle bundle = getIntent().getExtras();
      int fractaltype = bundle.getInt("FRACTAL");
      
      if (fractaltype == 0)
    	  fractalView = new MandelbrotFractalView(this);
      else
    	  fractalView = new JuliaFractalView(this);
      setContentView(fractalView);
      fractalView.requestFocus();
      
      mjLocation = new MandelbrotJuliaLocation();
      fractalView.loadLocation(mjLocation);
      
      gestureDetector = new ScaleGestureDetector(this, this);
   }

   
   @Override
   protected void onResume() {
      super.onResume();
      displaymode = DisplayMode.MANDELBROT;      
      Log.d(TAG, "onResume");
   }

   
   @Override
   protected void onPause() {
      super.onPause();
      Log.d(TAG, "onPause");
   }
   
   
   @Override
   protected void onStop() {
      super.onStop();
      //fractalView.fractalBitmap = null;
      //fractalView.movingBitmap = null;
      //fractalView.pixelSizes = null;
      Log.d(TAG, "onStop");
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
    	  float scaleOut = 0.5f;
    	  fractalView.zoomImage((float)(fractalView.getWidth()/2), (float)(fractalView.getHeight()/2), scaleOut);
    	  fractalView.stopZooming();
    	  fractalView.zoomChange((int)(fractalView.getWidth()/2), (int)(fractalView.getHeight()/2), 1/scaleOut);
    	  return true;
      case R.id.ZoomIn:
    	  float scale = 2.0f;
    	  fractalView.zoomImage((float)(fractalView.getWidth()/2), (float)(fractalView.getHeight()/2), scale);
    	  fractalView.stopZooming();
    	  fractalView.zoomChange((int)(fractalView.getWidth()/2), (int)(fractalView.getHeight()/2), 1/scale);
    	  return true;
      case R.id.juliamode:
    	  displaymode = DisplayMode.ABOUT_TO_JULIA;
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
				if (displaymode == DisplayMode.ABOUT_TO_JULIA)
				{
					Intent intent = new Intent(this, FractalActivity.class);
					Bundle bundle = new Bundle();
					bundle.putInt("FRACTAL", 1);
					intent.putExtras(bundle);
			   		startActivity(intent);
				}
				else
				{
					dragLastX = (int) evt.getX();
					dragLastY = (int) evt.getY();
					dragID = evt.getPointerId(0);	
					
					fractalView.startDragging();
					currentlyDragging = true;	
				}
				return true;
				
			case MotionEvent.ACTION_MOVE:						
				if(currentlyDragging)//!gestureDetector.isInProgress())
				{
					int pointerIndex = evt.findPointerIndex(dragID);
					
					float dragDiffPixelsX = evt.getX(pointerIndex) - dragLastX;
					float dragDiffPixelsY = evt.getY(pointerIndex) - dragLastY;
			
					// Move the canvas
					if (dragDiffPixelsX != 0.0f && dragDiffPixelsY != 0.0f)
						fractalView.dragFractal(dragDiffPixelsX, dragDiffPixelsY);
			
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
				currentlyDragging = false;
				fractalView.stopDragging(false);
				break;
		}
		return true;
	}
	
	
   public boolean onScaleBegin(ScaleGestureDetector detector) {
	   fractalView.stopDragging(true);
	   fractalView.startZooming(detector.getFocusX(), detector.getFocusY());
	   	 
	   currentlyDragging = false;
	   return true;
	}
	
	
   public boolean onScale(ScaleGestureDetector detector) {
		fractalView.zoomImage(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
		return true;
	}
	
	
   public void onScaleEnd(ScaleGestureDetector detector) {
	   fractalView.stopZooming();
	   currentlyDragging = true;
	   fractalView.startDragging();
	}
}
