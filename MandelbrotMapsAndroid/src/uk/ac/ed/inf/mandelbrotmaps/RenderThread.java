package uk.ac.ed.inf.mandelbrotmaps;

import android.util.Log;

class RenderThread extends Thread {
	public enum FractalHalf {
		UPPER,
		LOWER
	}
	
	private AbstractFractalView mjCanvas;
	private volatile boolean abortThisRendering = false;
	private final boolean isPrimary;
	
	public RenderThread(AbstractFractalView mjCanvasHandle, boolean primary) {
		isPrimary = primary;
		mjCanvas = mjCanvasHandle;
		setPriority(Thread.MIN_PRIORITY);
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
				Rendering newRendering = mjCanvas.getNextRendering();
				mjCanvas.computeAllPixels(newRendering.getPixelBlockSize(), FractalHalf.UPPER);
				abortThisRendering = false;
			} catch (InterruptedException e) {
				abortThisRendering = false;
				Log.d("Testing", "Caught exception");
			}
		}
	}
}