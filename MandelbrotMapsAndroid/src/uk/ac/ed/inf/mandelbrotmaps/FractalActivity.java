package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class FractalActivity extends Activity {
   private static final String TAG = "MMaps";

   private MandelbrotFractalView fractalView;
   private MandelbrotJuliaLocation mjLocation;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");

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
    	  fractalView.dragCanvas(0, -100);
    	  return true;
      case R.id.PanDown:
    	  fractalView.dragCanvas(0, 100);
    	  return true;
      case R.id.PanLeft:
    	  fractalView.dragCanvas(100, 0);
    	  return true;
      case R.id.PanRight:
    	  fractalView.dragCanvas(-100, 0);
    	  return true;
      }
      return false;
   }
}
