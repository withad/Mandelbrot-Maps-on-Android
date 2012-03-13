package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class DetailControl extends Activity implements OnClickListener, OnSeekBarChangeListener {	
	
	private final String TAG = "MMaps";
	
	Button applyButton;
	Button defaultsButton;
	Button cancelButton;
	
	SeekBar mandelbrotBar;
	SeekBar juliaBar;
	
	TextView mandelbrotText;
	TextView juliaText;
	
	int originalMandelbrot = 0;
	int originalJulia = 0;
	
	int returnMandelbrot = 0;
	int returnJulia = 0;
	
	boolean changed = false;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detailcontrol);
		
		// Get references to UI elements
		applyButton = (Button)findViewById(R.id.detail_apply_button);
		applyButton.setOnClickListener(this);
		defaultsButton = (Button) findViewById(R.id.default_detail_button);
		defaultsButton.setOnClickListener(this);
		cancelButton = (Button) findViewById(R.id.detail_cancel_button);
		cancelButton.setOnClickListener(this);
		
		// Assign TextViews before their values are set when the SeekBars change
		mandelbrotText = (TextView)findViewById(R.id.mandelbrotText);		
		juliaText = (TextView)findViewById(R.id.juliaText);
		
		// Get references to SeekBars, set their value from the prefs
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		mandelbrotBar = (SeekBar) findViewById(R.id.mandelbrot_seekbar);
		mandelbrotBar.setOnSeekBarChangeListener(this);
		mandelbrotBar.setProgress((int)prefs.getFloat(FractalActivity.mandelbrotDetailKey, (float) AbstractFractalView.DEFAULT_DETAIL_LEVEL));
		
		juliaBar = (SeekBar) findViewById(R.id.julia_seekbar);
		juliaBar.setOnSeekBarChangeListener(this);
		juliaBar.setProgress((int)prefs.getFloat(FractalActivity.juliaDetailKey, (float) AbstractFractalView.DEFAULT_DETAIL_LEVEL));
	}
	

	public void onClick(View view) {
		int button = view.getId();		
		
		if(button == R.id.detail_cancel_button) {
			finish();
		}
		else if(button == R.id.default_detail_button) {
			juliaBar.setProgress((int)AbstractFractalView.DEFAULT_DETAIL_LEVEL);
			mandelbrotBar.setProgress((int)AbstractFractalView.DEFAULT_DETAIL_LEVEL);
		}
		else if(button == R.id.detail_apply_button) {
			//Set shared prefs and return value (to indicate if shared prefs have changed)
			SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			prefsEditor.putFloat(FractalActivity.mandelbrotDetailKey, (float)mandelbrotBar.getProgress());
			prefsEditor.putFloat(FractalActivity.juliaDetailKey, (float)juliaBar.getProgress());
			
			prefsEditor.commit();
			
			changed = true;
			
			finish();
		}
	}
	
	@Override
	public void finish() {
		Intent result = new Intent();
		result.putExtra(FractalActivity.DETAIL_CHANGED_KEY, changed);
	   
		setResult(Activity.RESULT_OK, result);
		
		super.finish();
	}

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		
		if(seekBar.getId() == R.id.mandelbrot_seekbar) {
			mandelbrotText.setText(Integer.toString(seekBar.getProgress()));
		}	
		else if(seekBar.getId() == R.id.julia_seekbar) {
			juliaText.setText(Integer.toString(seekBar.getProgress()));
		}
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

}
 