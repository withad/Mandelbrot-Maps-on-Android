package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class FractalActivity extends Activity {
   private static final String TAG = "MMaps";

   private MandelbrotFractalView fractalView;
   private MandelbrotJuliaLocation mjLocation;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");

      fractalView = new MandelbrotFractalView(this);
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
      inflater.inflate(R.menu.colourmenu, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.Blue:
    	  //fractalView.paint.setColor(Color.BLUE);
    	  fractalView.invalidate();
    	  return true;
      case R.id.Green:
    	  //fractalView.paint.setColor(Color.GREEN);
    	  fractalView.invalidate();
    	  return true;
      case R.id.Red:
    	  //fractalView.paint.setColor(Color.RED);
    	  fractalView.invalidate();
    	  return true;
      }
      return false;
   }
}
