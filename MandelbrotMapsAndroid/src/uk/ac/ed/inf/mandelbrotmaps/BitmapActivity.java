package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class BitmapActivity extends Activity implements OnTouchListener {
   private static final String TAG = "MMaps";

   private BitmapDraggingView bitmapView;
   private MandelbrotJuliaLocation mjLocation;
   
   private int dragLastX;
   private int dragLastY;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      
      bitmapView = new BitmapDraggingView(this);
      setContentView(bitmapView);
      bitmapView.requestFocus();
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
    	  bitmapView.zoomChange((int)(bitmapView.getWidth()/2), (int)(bitmapView.getHeight()/2), 1);
    	  return true;
      case R.id.ZoomIn:
    	  bitmapView.zoomChange((int)(bitmapView.getWidth()/2), (int)(bitmapView.getHeight()/2), -1);
    	  return true;
      case R.id.PanUp:
    	  bitmapView.dragCanvas(0, -100);
    	  return true;
      case R.id.PanDown:
    	  bitmapView.dragCanvas(0, 100);
    	  return true;
      case R.id.PanLeft:
    	  bitmapView.dragCanvas(100, 0);
    	  return true;
      case R.id.PanRight:
    	  bitmapView.dragCanvas(-100, 0);
    	  return true;
      }
      return false;
   }


public boolean onTouch(View v, MotionEvent evt) {
	Log.d(TAG, "Event: " + evt.getActionMasked());
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
			Log.d(TAG, "Dragging detected");
			Log.d(TAG, "X: " + dragLastX + " Y: " + dragLastY);
			// If in real time mode, enable dragging.
			// How has the mouse moved? Vars should each be one of: {-1, 0, 1}
			int dragDiffPixelsX = (int) (evt.getX() - dragLastX);
			int dragDiffPixelsY = (int) (evt.getY() - dragLastY);
	
			// Move the canvas
			bitmapView.dragCanvas(dragDiffPixelsX, dragDiffPixelsY);
	
			// Update last mouse position
			dragLastX = (int) evt.getX();
			dragLastY = (int) evt.getY();
			
			Log.d(TAG, "X: " + evt.getX() + " Y: " + evt.getY());
			return true;
			
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "Up detected");
			Log.d(TAG, "X: " + evt.getX() + " Y: " + evt.getY());
			return true;
	}
	return false;
}
}
