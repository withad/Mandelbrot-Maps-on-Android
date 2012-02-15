package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.RenderStyle;
import uk.ac.ed.inf.mandelbrotmaps.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

public class LauncherActivity extends Activity implements OnClickListener {
   private static final String TAG = "MMaps";
   
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      // Set up click listeners for all the buttons
      View fractalButton = findViewById(R.id.fractal_button);
      fractalButton.setOnClickListener(this);
      
      View aboutButton = findViewById(R.id.about_button);
      aboutButton.setOnClickListener(this);
      
      View exitButton = findViewById(R.id.exit_button);
      exitButton.setOnClickListener(this);
      
      View uiMockupButton = findViewById(R.id.bitmap_test_button);
      uiMockupButton.setOnClickListener(this);
      
      View sideBySideButton = findViewById(R.id.side_by_side_button);
      sideBySideButton.setOnClickListener(this);
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   protected void onPause() {
      super.onPause();
   }

   public void onClick(View v) {
      switch (v.getId()) {
      case R.id.about_button:
         Intent i = new Intent(this, About.class);
         startActivity(i);
         break;
      case R.id.fractal_button:
         startFractal(RenderStyle.SINGLE_THREAD, false);
         break;
      case R.id.bitmap_test_button:
    	  //startActivity(new Intent(this, BitmapActivity.class));
    	  startFractal(RenderStyle.DUAL_THREAD, false);
    	  break;
      case R.id.side_by_side_button:
    	  Log.d(TAG, "Side by side button pressed");
    	  startFractal(RenderStyle.DUAL_THREAD, true);
    	  break;
      case R.id.exit_button:
         finish();
         break;
      }
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu, menu);
      return true;
   }

   /** Start a new game with the given difficulty level */
   private void startFractal(RenderStyle style, boolean sideBySide) {
   		Intent intent = new Intent(this.getApplicationContext(), FractalActivity.class);
   		Bundle bundle = new Bundle();
   		bundle.putBoolean("SideBySide", sideBySide);
   		bundle.putInt("FRACTAL", 0);
   		bundle.putString("RenderStyle", style.toString());
   		intent.putExtras(bundle);
   		startActivity(intent);
   }
}