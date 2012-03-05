package uk.ac.ed.inf.mandelbrotmaps;

import android.util.Log;

public class SavingTask extends Thread {
	String TAG = "MMaps";
	
	AbstractFractalView fractalView;
	Boolean cancelledSave;
	
	public SavingTask (AbstractFractalView _fractalView, Boolean _cancelledSave) {
		fractalView = _fractalView;
		cancelledSave = _cancelledSave;
	}
	
	public void run() {
			if(fractalView.isRendering()) {
				while (!cancelledSave && fractalView.isRendering()) {
					try {
						Thread.sleep(100);
						Log.d(TAG, "Waiting to save...");
					} catch (InterruptedException e) {}
				}
			}	
			synchronized (this) {
				this.notify();
			}
			return;  
	}
}
