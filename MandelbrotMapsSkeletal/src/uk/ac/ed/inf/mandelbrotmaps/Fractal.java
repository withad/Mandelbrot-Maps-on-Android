/***
 * Excerpted from "Hello, Android! 3e",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/eband3 for more book information.
***/
package uk.ac.ed.inf.mandelbrotmaps;

import org.example.sudoku.R;

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

public class Fractal extends Activity {
   private static final String TAG = "MMaps";

   private GraphicsView fractalView;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");

      fractalView = new GraphicsView(this);
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
    	  fractalView.paint.setColor(Color.BLUE);
    	  fractalView.invalidate();
    	  return true;
      case R.id.Green:
    	  fractalView.paint.setColor(Color.GREEN);
    	  fractalView.invalidate();
    	  return true;
      case R.id.Red:
    	  fractalView.paint.setColor(Color.RED);
    	  fractalView.invalidate();
    	  return true;
      }
      return false;
   }
}
