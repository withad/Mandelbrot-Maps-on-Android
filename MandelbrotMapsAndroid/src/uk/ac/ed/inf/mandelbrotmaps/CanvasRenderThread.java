package uk.ac.ed.inf.mandelbrotmaps;

class CanvasRenderThread extends Thread {
	private AbstractFractalView mjCanvas;
	private volatile boolean abortThisRendering = false;
	
	public CanvasRenderThread(AbstractFractalView mjCanvasHandle) {
		mjCanvas = mjCanvasHandle;
		setPriority(Thread.MIN_PRIORITY);
	}
	
	public void abortRendering() {
		abortThisRendering = true;
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
			} catch (InterruptedException e) {}
		}
	}
}