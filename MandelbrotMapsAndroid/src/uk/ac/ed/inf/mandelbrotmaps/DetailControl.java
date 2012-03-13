package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.os.Bundle;
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
		
		mandelbrotBar = (SeekBar) findViewById(R.id.mandelbrot_seekbar);
		mandelbrotBar.setOnSeekBarChangeListener(this);
		
		juliaBar = (SeekBar) findViewById(R.id.julia_seekbar);
		juliaBar.setOnSeekBarChangeListener(this);
		
		mandelbrotText = (TextView)findViewById(R.id.mandelbrotText);		
		juliaText = (TextView)findViewById(R.id.juliaText);
	}
	

	public void onClick(View view) {
		int button = view.getId();		
		
		if(button == R.id.detail_cancel_button) {
			finish();
		}
		else if(button == R.id.default_detail_button) {
			juliaBar.setProgress(30);
			mandelbrotBar.setProgress(30);
		}
		else if(button == R.id.detail_apply_button) {
			//Set shared prefs and return value (to indicate if shared prefs have changed)
			boolean changed = (originalMandelbrot == mandelbrotBar.getProgress()) 
								&& (originalJulia == juliaBar.getProgress());

			finish();
		}
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
 