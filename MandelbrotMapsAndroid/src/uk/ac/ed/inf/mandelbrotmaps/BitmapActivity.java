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

   private BitmapDraggingView bitmapView;
   private MandelbrotJuliaLocation mjLocation;
   
   private int dragLastX;
   private int dragLastY;
   
   private ScaleGestureDetector gestureDetector;

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
	Log.d(TAG, "Event: " + evt.getActionMasked());
	gestureDetector.onTouchEvent(evt);
	switch (evt.getActionMasked())
	{
		case MotionEvent.ACTION_DOWN:
			// Remember mouse position
			Log.d(TAG, "Remembering touch position");
			dragLastX = (int) evt.getX();
			dragLastY = (int) evt.getY();
			Log.d(TAG, "X: " + dragLastX + " Y: " + dragLastY);
			return true;
			
		case MotionEvent.ACTION_MOVE:
			if(!gestureDetector.isInProgress())
			{
				Log.d(TAG, "Dragging detected");
				Log.d(TAG, "X: " + dragLastX + " Y: " + dragLastY);
	
				int dragDiffPixelsX = (int) (evt.getX() - dragLastX);
				int dragDiffPixelsY = (int) (evt.getY() - dragLastY);
		
				// Move the canvas
				bitmapView.dragCanvas(dragDiffPixelsX, dragDiffPixelsY);
		
				// Update last mouse position
				dragLastX = (int) evt.getX();
				dragLastY = (int) evt.getY();
				
				Log.d(TAG, "X: " + evt.getX() + " Y: " + evt.getY());
				return true;
			}
			
			
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "Up detected");
			Log.d(TAG, "X: " + evt.getX() + " Y: " + evt.getY());
			return true;
	}
	return false;
}


public boolean onScale(ScaleGestureDetector detector) {
	Log.d(TAG, "This working?");
	
	bitmapView.midX = detector.getFocusX();
	bitmapView.midY = detector.getFocusY();
	
	bitmapView.scaleFactor = gestureDetector.getScaleFactor();
	bitmapView.invalidate();
	return true;
}


public boolean onScaleBegin(ScaleGestureDetector detector) {
	Log.d(TAG, "On scale begin working?");
	return true;
}


public void onScaleEnd(ScaleGestureDetector detector) {
	// TODO Auto-generated method stub
	
}
}
