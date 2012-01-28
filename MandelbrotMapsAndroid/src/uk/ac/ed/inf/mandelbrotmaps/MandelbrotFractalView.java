package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.RenderThread.FractalSection;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class MandelbrotFractalView extends AbstractFractalView{

	private static final String TAG = "MMaps";
	
	
	public MandelbrotFractalView(Context context) {
		super(context);

		// Set the "maximum iteration" calculation constants
		// Empirically determined values for Mandelbrot set.
		ITERATION_BASE = 1.24;
		ITERATION_CONSTANT_FACTOR = 54;
		
		// Set home area
		homeGraphArea = new MandelbrotJuliaLocation().getMandelbrotGraphArea();
		
		// How deep a zoom do we allow?
		MAXZOOM_LN_PIXEL = -31; // Beyond -31, "double"s break down(!).
	}
		
		
	// Load a location
	void loadLocation(MandelbrotJuliaLocation mjLocation) {
		setScaledIterationCount(mjLocation.getMandelbrotContrast());
		setGraphArea(mjLocation.getMandelbrotGraphArea(), true);
	}
	
	// Iterate a rectangle of pixels, in range (xPixelMin, yPixelMin) to (xPixelMax, yPixelMax)
	void computePixels(
		int[] outputPixelArray,  // Where pixels are output
		int[] currentPixelSizes,
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
		FractalSection section
	) {				
		int maxIterations = getMaxIterations();
		int imgWidth = xPixelMax - xPixelMin;
		
		// Efficiency: For very high-demanding pictures, increase pixel block.
		if (
			(pixelBlockSize!=1) && (maxIterations>10000)
		) pixelBlockSize = Math.min(
			getWidth() / 17,
			pixelBlockSize * (maxIterations/5000)
		);
		
		int xPixel = 0, yPixel = 0, yIncrement = 0, iterationNr = 0;
		double colourCode;
		int colourCodeR, colourCodeG, colourCodeB, colourCodeHex;
		int pixelBlockA, pixelBlockB;
	
		// c = (x0) + (y0)i
		double x0, y0;
	
		// z = (x) + (y)i
		double x, y;
	
		// newz = (newx) + (newy)i
		// ... NB: newz = (z^2 + c)
		double newx, newy;
		
		long initialMillis = System.currentTimeMillis();
		Log.d(TAG, "Initial time: " + initialMillis);
	
		for (yIncrement = yPixelMin; yIncrement < yPixelMax+1-pixelBlockSize; yIncrement+=pixelBlockSize) {			
			//Work backwards on upper half
			if (section == FractalSection.UPPER)
				yPixel = yPixelMax - yIncrement - 1;
			else 
				yPixel = yIncrement;
			
			if (
				allowInterruption &&
				upperRenderThread.abortSignalled()
			) 
				{
					Log.d("MFV", "Returning based on interruption test");
					return;
				}
			
			// Set y0 (im part of c)
			y0 = yMax - ( (double)yPixel * pixelSize );			
		
			for (xPixel=xPixelMin; xPixel<xPixelMax+1-pixelBlockSize; xPixel+=pixelBlockSize) {
				//Check to see if this pixel is already iterated to the necessary block size
				/*int size = pixelSizes[(imgWidth*yPixel) + xPixel];
				if(renderMode == RenderMode.JUST_DRAGGED && 
						size <= pixelBlockSize)
				{
					continue;
				}*/
				
				// Set x0 (real part of c)
				x0 = xMin + ( (double)xPixel * pixelSize);
			
				// Start at x0, y0
				x = x0;
				y = y0;
			
				for (iterationNr=0; iterationNr<maxIterations; iterationNr++) {
					// z^2 + c
					newx = (x*x) - (y*y) + x0;
					newy = (2 * x * y) + y0;
				
					x = newx;
					y = newy;
				
					// Well known result: if distance is >2, escapes to infinity...
					if ( (x*x + y*y) > 4) break;
				}
				
				// Percentage (0.0 -- 1.0)
				colourCode = (double)iterationNr / (double)maxIterations;
				
				// Red
				colourCodeR = Math.min((int)(255 * 6*colourCode), 255);
				
				// Green
				colourCodeG = (int)(255*colourCode);
				
				// Blue
				colourCodeB = (int)(
					127.5 - 127.5*Math.cos(
						7 * Math.PI * colourCode
					)
				);
				
				// Save colour info for this pixel. int, interpreted: 0xAARRGGBB
				colourCodeHex = (0xFF<<24) + (colourCodeR<<16) + (colourCodeG<<8) + (colourCodeB);
				for (pixelBlockA=0; pixelBlockA<pixelBlockSize; pixelBlockA++) {
					for (pixelBlockB=0; pixelBlockB<pixelBlockSize; pixelBlockB++) {
						if(outputPixelArray == null) return;
						outputPixelArray[imgWidth*(yPixel+pixelBlockB) + (xPixel+pixelBlockA)] = colourCodeHex;
						//currentPixelSizes[imgWidth*(yPixel+pixelBlockB) + (xPixel+pixelBlockA)] = pixelBlockSize;
					}
				}
			}
			// Show thread's work in progress
			if ((showRenderingProgress) && (yPixel % 30 == 0)) 
				{
					//Log.d(TAG, "Should be creating new Bitmap... " + yPixel);
					postInvalidate();
				}
		}
		
		postInvalidate();
		Log.d("MFV", "Reached end of computation loop");
		Log.d(TAG, "Time elapsed: " + (System.currentTimeMillis() - initialMillis));
	}
	
	
	public double[] getJuliaParams(float touchX, float touchY)
	{
		double[] mandelbrotGraphArea = getGraphArea();
		double pixelSize = getPixelSize();
	
		double[] juliaParams = new double[2];
		
		// Mouse position, on the complex plane (translated from pixels)
		juliaParams[0] = mandelbrotGraphArea[0] + ( (double)touchX * pixelSize );
		juliaParams[1] = mandelbrotGraphArea[1] - ( (double)touchY * pixelSize );
		
		return juliaParams;
	}

	

	
	/* Mouse events
	private void changeMode(int modeID) {
		mandelbrotMode = modeID;
	}
	//Mouse click
	public void mouseClicked(MouseEvent e) {
		// Real time mode? Change modes on click.
		if (mandelbrotMode == MODE_REALTIMEJULIA) {
			changeMode(MODE_FREEZE);
			setCursor(null);
		} else if (mandelbrotMode == MODE_FREEZE) {
			changeMode(MODE_REALTIMEJULIA);
			// Generate Julia set immediately.
			mouseMoved(e);
		}
	}	
	public void mouseMoved(MouseEvent e) {
		// Draw Julia set - if in real-time Julia mode, and real-time enabled
		if (
			(mandelbrotMode == MODE_REALTIMEJULIA)
		) {
			double[] mandelbrotGraphArea = getGraphArea();
			double pixelSize = getPixelSize();
		
			// Mouse position, on the complex plane (translated from pixels)
			juliaX = mandelbrotGraphArea[0] + ( (double)e.getX() * pixelSize );
			juliaY = mandelbrotGraphArea[1] - ( (double)e.getY() * pixelSize );
		}
	}
	
	// Need to override for MouseListener; unused at present.
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	*/
	
}
