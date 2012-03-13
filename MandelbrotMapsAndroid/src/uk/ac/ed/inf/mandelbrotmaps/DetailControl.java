package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class DetailControl extends Activity implements OnClickListener, OnSeekBarChangeListener {	
	
	Button applyButton;
	Button defaultsButton;
	Button cancelButton;
	
	int originalMandelbrot = 0;
	int originalJulia = 0;
	
	int returnMandelbrot = 0;
	int returnJulia = 0;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detailcontrol);
		
		applyButton = (Button)findViewById(R.id.detail_apply_button);
		applyButton.setOnClickListener(this);
		
		defaultsButton = (Button) findViewById(R.id.default_detail_button);
		defaultsButton.setOnClickListener(this);
		
		cancelButton = (Button) findViewById(R.id.detail_cancel_button);
		cancelButton.setOnClickListener(this);
	}
	

	public void onClick(View view) {
		int button = view.getId();		
		
		if(button == R.id.detail_cancel_button) {
			finish();
		}
		else if(button == R.id.default_detail_button) {
			
		}
		else if(button == R.id.detail_apply_button) {
			//Set shared prefs and return value (to indicate if shared prefs have changed)
			boolean changed = (originalMandelbrot == returnMandelbrot) && (originalJulia == returnJulia);
			
			finish();
		}
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}
 