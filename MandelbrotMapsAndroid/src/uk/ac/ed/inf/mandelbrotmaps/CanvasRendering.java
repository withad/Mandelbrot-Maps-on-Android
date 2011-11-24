package uk.ac.ed.inf.mandelbrotmaps;

class CanvasRendering {
	private int pixelBlockSize;
	
	public CanvasRendering(int newPixelBlockSize) {
		pixelBlockSize = newPixelBlockSize;
	}
	
	public int getPixelBlockSize() {
		return pixelBlockSize;
	}
}