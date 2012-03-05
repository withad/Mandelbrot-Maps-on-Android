package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.FractalViewSize;
import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.RenderMode;
import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.RenderStyle;
import android.content.Context;
import android.util.Log;

public class JuliaFractalView extends AbstractFractalView{

	private String TAG = "MMaps";
	
	// Point paramaterising this Julia set
	private double juliaX = 0;
	private double juliaY = 0;
	
	
	public JuliaFractalView(Context context, RenderStyle style, FractalViewSize size) {
		super(context, style, size);

		for(int i = 0; i < noOfThreads; i++) {
			renderThreadList.get(i).setName("Julia thread " + i);
		}
		
		// Set the "maximum iteration" calculation constants
		// Empirically determined values for Julia sets.
		ITERATION_BASE = 1.58;
		ITERATION_CONSTANT_FACTOR = 6.46;
		
		// Set home area
		homeGraphArea = new MandelbrotJuliaLocation().getJuliaGraphArea();
		
		// How deep a zoom do we allow?
		MAXZOOM_LN_PIXEL = -20; // Beyond -21, "double"s break down(!).
	}
		
		
	public void setJuliaParameter(double newJuliaX, double newJuliaY) {
		//stopAllRendering();
		juliaX = newJuliaX;
		juliaY = newJuliaY;
		scheduleNewRenders();
		
		Log.d(TAG, "Setting new Julia param...");
	}
	
	public double[] getJuliaParam() {
		double[] juliaParam = new double[2];
		juliaParam[0] = juliaX;
		juliaParam[1] = juliaY;
		return juliaParam;
	}
	
	// Load a location
	void loadLocation(MandelbrotJuliaLocation mjLocation) {
		setScaledIterationCount(mjLocation.getJuliaContrast());
		double[] juliaParam = mjLocation.getJuliaParam();
		setGraphArea(mjLocation.getJuliaGraphArea(), true);
		setJuliaParameter(juliaParam[0], juliaParam[1]);
	}
	
	// Iterate a rectangle of pixels, in range (xPixelMin, yPixelMin) to (xPixelMax, yPixelMax)
	void computePixels(
		int pixelBlockSize,  // Pixel "blockiness"
		final boolean showRenderingProgress,  // Call newPixels() on outputMIS as we go?
		final int xPixelMin,
		final int xPixelMax,
		final int yPixelMin,
		final int yPixelMax,
		final double xMin,
		final double yMax,
		final double pixelSize,
		final boolean allowInterruption,  // Shall we abort if renderThread signals an abort?
		RenderMode renderMode,
		final int threadID,
		final int noOfThreads) {	
			RenderThread callingThread = renderThreadList.get(threadID);
			
			int maxIterations = getMaxIterations();
			int imgWidth = xPixelMax - xPixelMin;
			
			int xPixel = 0, yPixel = 0, yIncrement = 0, iterationNr = 0;
			double colourCode;
			int colourCodeR, colourCodeG, colourCodeB, colourCodeHex;
			int pixelBlockA, pixelBlockB;
		
			double x, y;
			double newx, newy;
			
			long initialMillis = System.currentTimeMillis();
			Log.d(TAG, "Initial time: " + initialMillis);
			
			int pixelIncrement = pixelBlockSize * noOfThreads;
			int skippedCount = 0;
			
			for (yIncrement = yPixelMin; yIncrement < yPixelMax+1-pixelBlockSize; yIncrement+= pixelIncrement) {			
				yPixel = yIncrement;
				
				// Detect rendering abortion.			
				if (allowInterruption && (callingThread.abortSignalled())) {
					Log.d(TAG, "Render aborted.");
					return;
				}
			
				for (xPixel=xPixelMin; xPixel<xPixelMax+1-pixelBlockSize; xPixel+=pixelBlockSize) {
					//Check to see if this pixel is already iterated to the necessary block size
					if(pixelSizes[(imgWidth*yPixel) + xPixel] <= pixelBlockSize) {
						skippedCount++;
						continue;
					}
					
					// Initial coordinates
					x = xMin + ( (double)xPixel * pixelSize);
					y = yMax - ( (double)yPixel * pixelSize);
					
					double myJuliaX = juliaX;
					double myJuliaY = juliaY;
				
					iterationNr = equationIteration(x, y, myJuliaX, myJuliaY, maxIterations);
					
					// Percentage (0.0 -- 1.0)
					colourCode = (double)iterationNr / (double)maxIterations;
					
					// Red
					colourCodeR = Math.min((int)(255 * 2*colourCode), 255);
					
					// Green
					colourCodeG = (int)(255*colourCode);
					
					// Blue
					colourCodeB = (int)(
						127.5 - 127.5*Math.cos(
							3 * Math.PI * colourCode
						)
					);
					
					//Compute colour from the three components
					colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);
					
					//Note that the pixel being calculated has been calculated in full (upper right of a block)
					pixelSizes[(imgWidth*yPixel) + (xPixel)] = DEFAULT_PIXEL_SIZE;
					
					// Save colour info for this pixel. int, interpreted: 0xAARRGGBB
					int p = 0;
					for (pixelBlockA=0; pixelBlockA<pixelBlockSize; pixelBlockA++) {
						for (pixelBlockB=0; pixelBlockB<pixelBlockSize; pixelBlockB++) {
							if(p != 0) {
								pixelSizes[imgWidth*(yPixel+pixelBlockB) + (xPixel+pixelBlockA)] = pixelBlockSize;
							}
							p++;
							if(fractalPixels == null) return;
							fractalPixels[imgWidth*(yPixel+pixelBlockB) + (xPixel+pixelBlockA)] = colourCodeHex;
						}
					}
				}
				// Show thread's work in progress
				if ((showRenderingProgress) && (yPixel % LINES_TO_DRAW_AFTER == 0)
				) 
					{
						postInvalidate();
					}
			}
			
			postInvalidate();
			notifyCompleteRender(threadID, pixelBlockSize);
			Log.d(TAG, "Reached end of computation loop. Skipped: " + skippedCount);
			Log.d(TAG, callingThread.getName() + " complete. Time elapsed: " + (System.currentTimeMillis() - initialMillis));
		}


	@Override
	int equationIteration(double x, double y, double myJuliaX, double myJuliaY,	int maxIterations) {
		int iterationNr;
		double newx, newy;
		
		// We don't want the Julia parameter to change under our feet...
		for (iterationNr=0; iterationNr<maxIterations; iterationNr++) {
			// z^2 + c
			newx = (x*x) - (y*y) + myJuliaX;
			newy = (2 * x * y) + myJuliaY;
		
			x = newx;
			y = newy;
		
			// Well known result: if distance is >2, escapes to infinity...
			if ( (x*x + y*y) > 4) { 
				break;
			}
		}
		
		return iterationNr;
	}
}
