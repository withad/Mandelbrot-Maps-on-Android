package uk.ac.ed.inf.mandelbrotmaps;

import org.example.sudoku.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class UIMockup extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uimockup);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.uimenu, menu);
		
		return true;
	}
	
	
	@Override
	   public boolean onOptionsItemSelected(MenuItem item) {
	      switch (item.getItemId()) {
		    case R.id.About:
		       startActivity(new Intent(this, About.class));
		       return true;
	        case R.id.Settings:
	           startActivity(new Intent(this, Prefs.class));
	           return true;
	      }
	      return false;
	   }

}
