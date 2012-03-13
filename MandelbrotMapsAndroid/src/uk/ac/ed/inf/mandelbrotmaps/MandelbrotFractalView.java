package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

public class MandelbrotFractalView extends AbstractFractalView{

	private final String TAG = "MMaps";
	
	public float lastTouchX = 0;
	public float lastTouchY = 0;
	
	public double[] currentJuliaParams = new double[2];
	private float[] pinCoords = new float[2];
	
	Paint outerPinPaint;
	Paint innerPinPaint;
	Paint selectedPinPaint;
	Paint littlePinPaint;
	
	int outerPinAlpha = 150;
	int innerPinAlpha = 150;
	int selectedPinAlpha = 150;
	int littlePinAlpha = 180;
	
	private float smallPinRadius = 5.0f;
	private float largePinRadius = 20.0f;
	
	
	
	
	public MandelbrotFractalView(Context context, FractalViewSize size) {
		super(context, size);
		
		setColouringScheme(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("MANDELBROT_COLOURS", "MandelbrotDefault")
							, false);
		
		for(int i = 0; i < noOfThreads; i++) {
			renderThreadList.get(i).setName("Mandelbrot thread " + i);
		}
		
		// Set the "maximum iteration" calculation constants
		// Empirically determined values for Mandelbrot set.
		ITERATION_BASE = 1.24;
		ITERATION_CONSTANT_FACTOR = 54;
		
		// Set home area
		homeGraphArea = new MandelbrotJuliaLocation().getMandelbrotGraphArea();
		
		// How deep a zoom do we allow?
		MAXZOOM_LN_PIXEL = -31; // Beyond -31, "double"s break down(!).
		
		
		int pinColour = Color.parseColor(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PIN_COLOUR", "blue"));
		
		outerPinPaint = new Paint();
		outerPinPaint.setColor(pinColour);
		outerPinPaint.setAlpha(outerPinAlpha);
		//outerPinPaint.setStyle(Style.STROKE);
		
		innerPinPaint = new Paint();
		innerPinPaint.setColor(pinColour);
		innerPinPaint.setAlpha(innerPinAlpha);
		
		littlePinPaint = new Paint();
		littlePinPaint.setColor(pinColour);
		littlePinPaint.setAlpha(littlePinAlpha);
		littlePinPaint.setStyle(Style.STROKE);
		
		selectedPinPaint = new Paint();
		selectedPinPaint.setColor(pinColour);
		selectedPinPaint.setAlpha(selectedPinAlpha);
	}
		
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(parentActivity.showingLittle && drawPin) {
			if(controlmode != ControlMode.ZOOMING) pinCoords = getPinCoords();
			float[] mappedCoords = new float[2];
			matrix.mapPoints(mappedCoords, pinCoords);
			
			if(fractalViewSize == FractalViewSize.LARGE) {
				canvas.drawCircle(mappedCoords[0], mappedCoords[1], smallPinRadius, innerPinPaint);
				
				//Draw larger outer circle if pin is held down.
				if(!holdingPin)
					canvas.drawCircle(mappedCoords[0], mappedCoords[1], largePinRadius, outerPinPaint);
				else
					canvas.drawCircle(mappedCoords[0], mappedCoords[1], largePinRadius*2, selectedPinPaint);
			}
			else if (fractalViewSize == FractalViewSize.LITTLE) {
				canvas.drawCircle(mappedCoords[0], mappedCoords[1], smallPinRadius, littlePinPaint);
			}
		}
	}
	
	
	/* Runs when the view changes size. 
	 * Sets the size of the pin based on screen size. */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	   
		// Show the little view at the start, if allowed.
		if(fractalViewSize == FractalViewSize.LARGE) {
			DisplayMetrics currentDisplayMetrics = new DisplayMetrics();
			parentActivity.getWindowManager().getDefaultDisplay().getMetrics(currentDisplayMetrics);
			
			int dpi = currentDisplayMetrics.densityDpi;
			Log.d(TAG, "Current dpi is " + dpi + " dots per inch");
			largePinRadius = dpi/6; 
			smallPinRadius = dpi/30;
			Log.d(TAG, "Small circle radius is " + smallPinRadius);
		}
	}
	
	
	// Load a location
	void loadLocation(MandelbrotJuliaLocation mjLocation) {
		//setScaledIterationCount(mjLocation.getMandelbrotContrast());
		setGraphArea(mjLocation.getMandelbrotGraphArea(), true);
	}
	
	// Iterate a rectangle of pixels, in range (xPixelMin, yPixelMin) to (xPixelMax, yPixelMax)
	void computePixels(
		int pixelBlockSize,  // Pixel "blockiness"
		final boolean showRenderingProgress,  // Show progress as we go?
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
			int pixelBlockA, pixelBlockB;
		
			// c = (x0) + (y0)i
			double x0, y0;
		
			// z = (x) + (y)i
			double x, y;
		
			// newz = (newx) + (newy)i
			// ... NB: newz = (z^2 + c)
			double newx, newy;
			
			long initialMillis = System.currentTimeMillis();
			//Log.d(TAG, "Initial time: " + initialMillis);
			
			int pixelIncrement = pixelBlockSize * noOfThreads;
			int originalIncrement = pixelIncrement;
			
			int skippedCount = 0;
			
			int loopCount = 0;
			
			
			for (yIncrement = yPixelMin; yPixel < yPixelMax+(noOfThreads*pixelBlockSize) ; yIncrement += pixelIncrement) {			
				yPixel = yIncrement;
				
				pixelIncrement = (loopCount * originalIncrement);
				if(loopCount % 2 == 0)
					pixelIncrement*=-1;
				loopCount++;
				
				//If we've exceeded the bounds of the image (as can happen with many threads), exit the loop.
				if(((imgWidth * (yPixel+pixelBlockSize - 1)) + xPixelMax) > pixelSizes.length || 
						 yPixel < 0) {
					continue;
				}
				
				if (allowInterruption && (callingThread.abortSignalled())) {
					Log.d(TAG, "Render aborted.");
					return;
				}
				
				// Set y0 (im part of c)
				y0 = yMax - ( (double)yPixel * pixelSize );			
			
				for (xPixel=xPixelMin; xPixel<xPixelMax+1-pixelBlockSize; xPixel+=pixelBlockSize) {					
					//Check to see if this pixel is already iterated to the necessary block size
					if(fractalViewSize == FractalViewSize.LARGE && pixelSizes[(imgWidth*yPixel) + xPixel] <= pixelBlockSize) {
						skippedCount++;
						continue;
					}
					
					// Set x0 (real part of c)
					x0 = xMin + ( (double)xPixel * pixelSize);
				
					// Start at x0, y0
					x = x0;
					y = y0;
				
					boolean inside = true;
					
					//Run iterations over this point
					for (iterationNr=0; iterationNr<maxIterations; iterationNr++) {
						// z^2 + c
						newx = (x*x) - (y*y) + x0;
						newy = (2 * x * y) + y0;
					
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
				// Show thread's work in progress
				if ((showRenderingProgress) && (loopCount % linesToDrawAfter == 0)) 
					{
						postInvalidate();
					}
			}
			
			/*Log.d("ThreadEnding", "yIncrement of thread " + threadID + " is " + yIncrement + " and yPixel " + yPixel);*/
			
			
			//Log.d(TAG, "Ended on yIncrement " + yIncrement);
			postInvalidate();
			notifyCompleteRender(threadID, pixelBlockSize);
			//Log.d(TAG, "Reached end of computation loop. Skipped: " + skippedCount);
			//Log.d(TAG, callingThread.getName() + " complete. Time elapsed: " + (System.currentTimeMillis() - initialMillis));*/
		}
	
	
	public double[] getJuliaParams(float touchX, float touchY)
	{
		lastTouchX = touchX;
		lastTouchY = touchY;
		
		double[] mandelbrotGraphArea = graphArea;
		double pixelSize = getPixelSize();
	
		double[] juliaParams = new double[2];
		
		// Touch position, on the complex plane (translated from pixels)
		juliaParams[0] = mandelbrotGraphArea[0] + ( (double)touchX * pixelSize );
		juliaParams[1] = mandelbrotGraphArea[1] - ( (double)touchY * pixelSize );
		
		currentJuliaParams = juliaParams;
		
		return juliaParams;
	}
	
	
	public float[] getPinCoords() {		
		float[] pinCoords = new float[2];
		double pixelSize = getPixelSize();
		
		if (fractalViewSize == FractalViewSize.LITTLE) {
			currentJuliaParams = ((JuliaFractalView)parentActivity.fractalView).getJuliaParam();
		}

		pinCoords[0] = (float) ((currentJuliaParams[0] - graphArea[0]) / pixelSize);
		pinCoords[1] = (float) (-(currentJuliaParams[1] - graphArea[1]) / pixelSize);
		
		return pinCoords;
	}
	
	
	public void setPinColour(int newColour) {
		outerPinPaint.setColor(newColour);
		selectedPinPaint.setColor(newColour);
		innerPinPaint.setColor(newColour);
		littlePinPaint.setColor(newColour);
		
		// This somehow resets the alphas as well, so reset those.
		outerPinPaint.setAlpha(outerPinAlpha);
		innerPinPaint.setAlpha(innerPinAlpha);
		littlePinPaint.setAlpha(littlePinAlpha);		
		selectedPinPaint.setAlpha(selectedPinAlpha);
		
		invalidate();
	}
}
