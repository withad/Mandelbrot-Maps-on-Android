package uk.ac.ed.inf.mandelbrotmaps;

import android.util.Log;

class RenderThread extends Thread {
	public enum FractalSection {
		ALL,
		UPPER,
		LOWER
	}
	
	private final FractalSection fractalSection;
	
	private AbstractFractalView mjCanvas;
	private volatile boolean abortThisRendering = false;
	public boolean isRunning = false;
	
	public RenderThread(AbstractFractalView mjCanvasHandle, FractalSection section) {
		fractalSection = section;
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
				Rendering newRendering = mjCanvas.getNextRendering(fractalSection);
				mjCanvas.computeAllPixels(newRendering.getPixelBlockSize(), fractalSection);
				abortThisRendering = false;
			} catch (InterruptedException e) {
				return;
			}
		}
	}
}