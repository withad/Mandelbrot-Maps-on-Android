/***
 * Excerpted from "Hello, Android! 3e",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/eband3 for more book information.
***/
package org.example.sudoku;

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

public class Launcher extends Activity implements OnClickListener {
   private static final String TAG = "Sudoku";
   
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
      
      View uiMockupButton = findViewById(R.id.UI_mockup_button);
      uiMockupButton.setOnClickListener(this);
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
         openNewGameDialog();
         break;
      case R.id.UI_mockup_button:
    	  Log.d(TAG, "Launching UI mockup");
    	  startActivity(new Intent(this, UIMockup.class));
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

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.settings:
         startActivity(new Intent(this, Prefs.class));
         return true;
      // More items go here (if any) ...
      }
      return false;
   }

   /** Ask the user what difficulty level they want */
   private void openNewGameDialog() {
      new AlertDialog.Builder(this)
           .setTitle(R.string.draw_type_title)
           .setItems(R.array.render_type,
            new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialoginterface,
                     int i) {
                  startGame(i);
               }
            })
           .show();
   }

   /** Start a new game with the given difficulty level */
   private void startGame(int i) {
	   	if (i != 0) return;
	   	Log.d(TAG, "clicked on " + i);
   		Intent intent = new Intent(this, Fractal.class);
   		//intent.putExtra(Game.KEY_DIFFICULTY, i);
   		startActivity(intent);
   }
}