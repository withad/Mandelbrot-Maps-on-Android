package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.ControlMode;
import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.RenderStyle;
import uk.ac.ed.inf.mandelbrotmaps.RenderThread.FractalSection;

import android.app.Activity;
import android.content.Context;
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
	
	public enum FractalType {
		MANDELBROT,
		JULIA
	}
	
	private FractalType fractalType = FractalType.MANDELBROT;
	
	
	private enum DisplayMode{
		MANDELBROT,
		ABOUT_TO_JULIA,
		JULIA
	}
	
	private DisplayMode displaymode = DisplayMode.MANDELBROT;
	
	private final String TAG = "MMaps";

	private AbstractFractalView fractalView;
	private AbstractFractalView littleFractalView;
	private MandelbrotJuliaLocation mjLocation;
	   
	private float dragLastX;
	private float dragLastY;
	   
	private ScaleGestureDetector gestureDetector;
	   
	private int dragID = -1;
	
	RenderStyle style;
   

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      Log.d(TAG, "onCreate");
      
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

      Bundle bundle = getIntent().getExtras();
      
      fractalType = (bundle.getInt("FRACTAL") == 0 ? FractalType.MANDELBROT : FractalType.JULIA);
      style = RenderStyle.valueOf(bundle.getString("RenderStyle"));
      
      if (fractalType == FractalType.MANDELBROT)
    	  fractalView = new MandelbrotFractalView(this, style);
      else if (fractalType == FractalType.JULIA)
      {
    	  fractalView = new JuliaFractalView(this, style);
      }
      
      setContentView(fractalView);
      fractalView.requestFocus();
      
      mjLocation = new MandelbrotJuliaLocation();
      fractalView.loadLocation(mjLocation);
      
      if (fractalType == FractalType.JULIA)
      {
    	  double juliaX = bundle.getDouble("JULIA_X");
          double juliaY = bundle.getDouble("JULIA_Y");
          
          ((JuliaFractalView)fractalView).setJuliaParameter(juliaX, juliaY);
      }
      
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
      Log.d(TAG, "onStop");
   }
   
   
   @Override
   protected void onDestroy(){
	   super.onDestroy();
	   fractalView.stopAllRendering();
	   fractalView.interruptThreads();
	   Log.d(TAG, "Running onDestroy().");
   }
   
   
   @Override
public void finish(){
	   super.finish();
	   Log.d(TAG, "Running finish.");
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
      case R.id.printLocation:
    	  double[] current_area = fractalView.getGraphArea();
    	  Log.d(TAG, "X: " + current_area[0] + " Y: " + current_area[1] + " Width: " + current_area[2]);
    	  return true;
      case R.id.settobookmark:
    	  fractalView.setToBookmark();
    	  return true;
      case R.id.juliamode:
    	  displaymode = DisplayMode.ABOUT_TO_JULIA;
    	  return true;
      case R.id.resetFractal:
    	  fractalView.reset();
    	  return true;
      case R.id.toggleCrude:
    	  fractalView.crudeRendering = !fractalView.crudeRendering;
    	  return true;
      case R.id.saveImage:
    	  fractalView.saveImage();
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
					
					double[] juliaParams = ((MandelbrotFractalView)fractalView).getJuliaParams(evt.getX(), evt.getY());
					bundle.putDouble("JULIA_X", juliaParams[0]);
					bundle.putDouble("JULIA_Y", juliaParams[1]);
					bundle.putString("RenderStyle", style.toString());
					
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
				if(currentlyDragging)
				{
					currentlyDragging = false;
					fractalView.stopDragging(false);
				}
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
