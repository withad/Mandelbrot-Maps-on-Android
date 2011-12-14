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
   
   private enum ControlMode{
	   PAN,
	   ZOOM
   }
   
   private ControlMode controlMode;

   private MandelbrotFractalView fractalView;
   private MandelbrotJuliaLocation mjLocation;
   
   private int dragLastX;
   private int dragLastY;
   
   private int beforeDragX;
   private int beforeDragY;
   
   private boolean draggingFractal;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");
      
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

      fractalView = new MandelbrotFractalView(this);
      //fractalView.setLayoutParams(new LinearLayout.LayoutParams(500, 500));
      setContentView(fractalView);
      fractalView.requestFocus();
      
      mjLocation = new MandelbrotJuliaLocation();
      fractalView.loadLocation(mjLocation);
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
      }
      return false;
   }


public boolean onTouch(View v, MotionEvent evt) {
	switch (evt.getActionMasked())
	{
		case MotionEvent.ACTION_DOWN:			
			// Remember mouse position
			Log.d(TAG, "Remembering touch position");
			dragLastX = (int) evt.getX();
			dragLastY = (int) evt.getY();
			beforeDragX = (int) evt.getX();
			beforeDragY = (int) evt.getY();
			Log.d(TAG, "X: " + dragLastX + " Y: " + dragLastY);
			return true;
			
		case MotionEvent.ACTION_MOVE:
			if(!draggingFractal)
			{
				fractalView.startDragging();
				draggingFractal = true;
				Log.d(TAG, "Started dragging");
			}

			int dragDiffPixelsX = (int) (evt.getX() - dragLastX);
			int dragDiffPixelsY = (int) (evt.getY() - dragLastY);
	
			// Move the bitmap
			fractalView.dragFractal(dragDiffPixelsX, dragDiffPixelsY);
	
			// Update last mouse position
			dragLastX = (int) evt.getX();
			dragLastY = (int) evt.getY();
			
			return true;
			
		case MotionEvent.ACTION_UP:
			draggingFractal = false;
			fractalView.stopDragging();
			return true;
	}
	return false;
}


public boolean onScale(ScaleGestureDetector detector) {
	// TODO Auto-generated method stub
	return false;
}


public boolean onScaleBegin(ScaleGestureDetector detector) {
	// TODO Auto-generated method stub
	return false;
}


public void onScaleEnd(ScaleGestureDetector detector) {
	// TODO Auto-generated method stub
	
}
}
