package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class FractalActivity extends Activity {
   private static final String TAG = "MMaps";

   private FractalView fractalView;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");

      fractalView = new FractalView(this);
      setContentView(fractalView);
      fractalView.requestFocus();
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
