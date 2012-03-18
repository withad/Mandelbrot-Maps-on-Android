package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.colouring.ColouringScheme;
import uk.ac.ed.inf.mandelbrotmaps.colouring.DefaultColouringScheme;
import uk.ac.ed.inf.mandelbrotmaps.colouring.JuliaDefaultColouringScheme;
import uk.ac.ed.inf.mandelbrotmaps.colouring.RGBWalkColouringScheme;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

public class JuliaFractalView extends AbstractFractalView{

	private String TAG = "MMaps";
	
	// Point paramaterising this Julia set
	private double juliaX = 0;
	private double juliaY = 0;
	
	
	public JuliaFractalView(Context context, FractalViewSize size) {
		super(context, size);
		
		setColouringScheme(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("JULIA_COLOURS", "JuliaDefault")
				, false);

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
		setGraphArea(graphArea, true);
	}
	
	public double[] getJuliaParam() {
		double[] juliaParam = new double[2];
		juliaParam[0] = juliaX;
		juliaParam[1] = juliaY;
		return juliaParam;
	}
	
	// Load a location
	void loadLocation(MandelbrotJuliaLocation mjLocation) {
		//setScaledIterationCount(mjLocation.getJuliaContrast());
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
		final int threadID,
		final int noOfThreads) {	
			RenderThread callingThread = renderThreadList.get(threadID);
			
			int maxIterations = getMaxIterations();
			int imgWidth = xPixelMax - xPixelMin;
			
			int xPixel = 0, yPixel = 0, yIncrement = 0, iterationNr = 0;
			double colourCode;
			int colourCodeR, colourCodeG, colourCodeB, colourCodeHex;
			int pixelBlockA = 0, pixelBlockB = 0;
		
			double x, y;
			double newx, newy;
			
			long initialMillis = System.currentTimeMillis();
			//Log.d(TAG, "Initial time: " + initialMillis);
			
			int pixelIncrement = pixelBlockSize * noOfThreads;
			int originalIncrement = pixelIncrement;
			
			int skippedCount = 0;
			int loopCount = 0;
			
			for (yIncrement = yPixelMin; yPixel < yPixelMax+(noOfThreads*pixelBlockSize); yIncrement+= pixelIncrement) {	
				yPixel = yIncrement;
				
				pixelIncrement = (loopCount * originalIncrement);
				if(loopCount % 2 == 0)
					pixelIncrement*=-1;
				loopCount++;
				
				if(((imgWidth * (yPixel+pixelBlockSize - 1)) + xPixelMax) > pixelSizes.length ||
						 yPixel < 0) {
					continue;
				}
				
				// Detect rendering abortion.			
				if (allowInterruption && (callingThread.abortSignalled())) {
					return;
				}
			
				for (xPixel=xPixelMin; xPixel<xPixelMax+1-pixelBlockSize; xPixel+=pixelBlockSize) {
					//Check to see if this pixel is already iterated to the necessary block size
					try {
						if(fractalViewSize == FractalViewSize.LARGE && pixelSizes[(imgWidth*yPixel) + xPixel] <= pixelBlockSize) {
							skippedCount++;
							continue;
						}
					}
					catch (ArrayIndexOutOfBoundsException ae) {
						break;
					}
					
					// Initial coordinates
					x = xMin + ( (double)xPixel * pixelSize);
					y = yMax - ( (double)yPixel * pixelSize);
				
					boolean inside = true;
					
					for (iterationNr=0; iterationNr<maxIterations; iterationNr++) {
						// z^2 + c
						newx = (x*x) - (y*y) + juliaX;
						newy = (2 * x * y) + juliaY;
					
						x = newx;
						y = newy;
					
						// Well known result: if distance is >2, escapes to infinity...
						if ( (x*x + y*y) > 4) { 
							inside = false;
							break;
						}
					}
					
					if(inside)
						colourCodeHex = colourer.colourInsidePoint();
					else
						colourCodeHex = colourer.colourOutsidePoint(iterationNr, maxIterations);
					
					
					//Note that the pixel being calculated has been calculated in full (upper right of a block)
					if(fractalViewSize == FractalViewSize.LARGE)
						pixelSizes[(imgWidth*yPixel) + (xPixel)] = DEFAULT_PIXEL_SIZE;
					
					// Save colour info for this pixel. int, interpreted: 0xAARRGGBB
					int p = 0;
					try {
						for (pixelBlockA=0; pixelBlockA<pixelBlockSize; pixelBlockA++) {
							for (pixelBlockB=0; pixelBlockB<pixelBlockSize; pixelBlockB++) {
								if(fractalViewSize == FractalViewSize.LARGE) {
									if(p != 0) {
										pixelSizes[imgWidth*(yPixel+pixelBlockB) + (xPixel+pixelBlockA)] = pixelBlockSize;
									}
									p++;
								}
								if(fractalPixels == null) return;
								fractalPixels[imgWidth*(yPixel+pixelBlockB) + (xPixel+pixelBlockA)] = colourCodeHex;
							}
						}
					} 
					catch (ArrayIndexOutOfBoundsException ae) {
						break;
					}
				}
				// Show thread's work in progress
				if ((showRenderingProgress) && (yPixel % linesToDrawAfter == 0)
				) 
					{
						postInvalidate();
					}
			}
			
			postInvalidate();
			notifyCompleteRender(threadID, pixelBlockSize);
			/*Log.d(TAG, "Reached end of computation loop. Skipped: " + skippedCount);
			Log.d(TAG, callingThread.getName() + " complete. Time elapsed: " + (System.currentTimeMillis() - initialMillis));*/
		}
}
