package uk.ac.ed.inf.mandelbrotmaps;

class Rendering {
	private int pixelBlockSize;
	
	public Rendering(int newPixelBlockSize) {
		pixelBlockSize = newPixelBlockSize;
	}
	
	public int getPixelBlockSize() {
		return pixelBlockSize;
	}
}