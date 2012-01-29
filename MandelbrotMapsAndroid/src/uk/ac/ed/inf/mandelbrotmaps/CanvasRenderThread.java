package uk.ac.ed.inf.mandelbrotmaps;

import android.util.Log;

class CanvasRenderThread extends Thread {
	private AbstractFractalView mjCanvas;
	private volatile boolean abortThisRendering = false;
	
	public CanvasRenderThread(AbstractFractalView mjCanvasHandle) {
		mjCanvas = mjCanvasHandle;
		setPriority(Thread.MAX_PRIORITY);
	}
	
	public void abortRendering() {
		abortThisRendering = true;
	}
	
	public void allowRendering() {
		abortThisRendering = false;
	}
	
	public boolean abortSignalled() {
		return abortThisRendering;
	}
	
	public void run() {
		while(true) {
			try {
				CanvasRendering newRendering = mjCanvas.getNextRendering();
				mjCanvas.computeAllPixels(newRendering.getPixelBlockSize());
				abortThisRendering = false;
			} catch (InterruptedException e) {
				abortThisRendering = false;
				Log.d("Testing", "Caught exception");
			}
		}
	}
}