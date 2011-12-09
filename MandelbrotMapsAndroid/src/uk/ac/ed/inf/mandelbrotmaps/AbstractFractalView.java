package uk.ac.ed.inf.mandelbrotmaps;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

abstract class AbstractFractalView extends View {
   
   private static final String TAG = "FractalView";

   private int WIDTH;
   private int HEIGHT;
   private int STRIDE;
   
   // How many different, discrete zoom and contrast levels?
   public static final int ZOOM_SLIDER_SCALING = 300;
   public static final int CONTRAST_SLIDER_SCALING = 200;
   
   // Default "crude rendering" pixel block size?
   int INITIAL_PIXEL_BLOCK = 3;
   
   //Default pixel size
   int DEFAULT_PIXEL_SIZE = 1;
   
   // How much of a zoom, on each increment?
   public static final int zoomPercent = 20;
   
   // Rendering queue (modified from a LinkedBlockingDeque in the original version)
   LinkedBlockingQueue<CanvasRendering> renderingQueue = new LinkedBlockingQueue<CanvasRendering>();	
   CanvasRenderThread renderThread = new CanvasRenderThread(this);
   
   //Handle on parent activity
   FractalActivity parentActivity;	
	
	// What zoom range do we allow? Expressed as ln(pixelSize).
	double MINZOOM_LN_PIXEL = -3;
	double MAXZOOM_LN_PIXEL;
	
	// How many iterations, at the very fewest, will we do?
	int MIN_ITERATIONS = 10;
	
	// Constants for calculating maxIterations()
	double ITERATION_BASE;
	double ITERATION_CONSTANT_FACTOR;
	
	// Scaling factor for maxIterations() calculations
	double iterationScaling = 0.3;
	double ITERATIONSCALING_MIN = 0.01;
	double ITERATIONSCALING_MAX = 100;
	double ITERATIONSCALING_DEFAULT = 1;
	
	// Mouse dragging state.
	int dragLastX = 0;
	int dragLastY = 0;
	
	// Graph Area on the Complex Plane? new double[] {x_min, y_max, width}
	double[] graphArea;
	double[] homeGraphArea;
	
	// Fractal image data
	int[] fractalPixels;
	Bitmap fractalBitmap;
	Bitmap movingBitmap;
	
	// Where to draw the image onscreen
	public int bitmapX = 0;
	public int bitmapY = 0;	
	
	boolean pauseRendering;
	boolean draggingFractal = false;
	
   
   public AbstractFractalView(Context context) {
      super(context);
      setFocusable(true);
      setFocusableInTouchMode(true);
      setBackgroundColor(Color.BLACK);
      setId(0); 
      
      parentActivity = (FractalActivity)context;
      setOnTouchListener((FractalActivity)context);
      
      renderThread.start();
   }

   /*Android life-cycle handling*/   
   @Override
   protected Parcelable onSaveInstanceState() { 
	  super.onSaveInstanceState();
      Log.d(TAG, "onSaveInstanceState");
      Bundle bundle = new Bundle();
      return bundle;
   }
    
   @Override
   protected void onRestoreInstanceState(Parcelable state) { 
      Log.d(TAG, "onRestoreInstanceState");
      super.onRestoreInstanceState(state);
   }
     
   @Override
   protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);
      WIDTH = getWidth();
      HEIGHT = getHeight();
      STRIDE = WIDTH;
   }
   

   /* Graphics Stuff */
   @Override
   protected void onDraw(Canvas canvas) {	   
	// (Re)create pixel grid, if not initialised - or if wrong size.
	if (
		(fractalPixels == null) ||
		(fractalPixels.length != getWidth()*getHeight())
	) {
		fractalPixels = new int[getWidth() * getHeight()];
		updateDisplay();
	}
	
	if(fractalBitmap != null && !draggingFractal)
	{
		fractalBitmap = Bitmap.createBitmap(fractalPixels, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
		canvas.drawBitmap(fractalBitmap, 0, 0, new Paint());
	}
	else if (draggingFractal)
	{
		canvas.drawBitmap(fractalBitmap, bitmapX, bitmapY, new Paint());
	}
   }
	
   
   // Called when we want to recompute everything
	void updateDisplay() {		
		// Abort future rendering queue. If in real-time mode, interrupt current rendering too
		stopAllRendering();
		
		// If in real-time mode, schedule a crude rendering
		if (needCrudeRendering()) scheduleRendering(INITIAL_PIXEL_BLOCK);
		
		// Schedule a high-quality rendering
		scheduleRendering(DEFAULT_PIXEL_SIZE);
	}
   
	
	/* Utilities */	
	// Do we need a crude rendering? (Based on number of iterations)
	boolean needCrudeRendering() {
		return getMaxIterations() > 30;
	}
	
	public double[] getGraphArea() {
		return graphArea;
	}
	
	public void stopPlannedRendering() {
		renderingQueue.clear();
	}
	
	void stopAllRendering() {
		stopPlannedRendering();
		renderThread.abortRendering();
	}
	
	void scheduleRendering(int pixelBlockSize) {
		renderingQueue.add( new CanvasRendering(pixelBlockSize) );
	}
	
	public CanvasRendering getNextRendering() throws InterruptedException {
		return renderingQueue.take();
	}
	
	
	void setGraphArea(double[] newGraphArea) {
		// We have a predefined graphArea, so we can be picky with newGraphArea.
		if (graphArea != null) {
			double[] initialGraphArea = graphArea;
			graphArea = newGraphArea;
			
			// Zoom level is sane - let's allow this!
			if (saneZoomLevel()) {
				updateDisplay();
			// Zoom level is out of bounds; let's just roll back.
			} else {
				graphArea = initialGraphArea;
			}
		// There is no predefined graphArea; we'll have to accept whatever newGraphArea is.
		} else {
			graphArea = newGraphArea;
			updateDisplay();
		}
	}
	
	// On the complex plane, what is the current length of 1 pixel?
	// Pixels are square, so 1D length completely categorises.
	double getPixelSize() {
		// Nothing to do - cannot compute a sane pixel size
		if (getWidth() == 0) return 0.0;
		if (graphArea == null) return 0.0;
		// Return the pixel size
		return (graphArea[2] / (double)getWidth());
	}
	
	// Restore default canvas
	public void canvasHome() {
		// Default max iterations scaling
		iterationScaling = ITERATIONSCALING_DEFAULT;
		
		// Default graph area
		double[] newGraphArea = new double[homeGraphArea.length];
		for (int i=0; i<homeGraphArea.length; i++) newGraphArea[i] = homeGraphArea[i];
		setGraphArea(newGraphArea);
	}
		
	// Adjust zoom, centred on pixel (xPixel, yPixel)
	public void zoomChange(int xPixel, int yPixel, int zoomAmount) {
		double pixelSize = getPixelSize();
		
		double[] oldGraphArea = getGraphArea();
		double[] newGraphArea = new double[3];
		
		double zoomPercentChange = (double)(100 + (zoomPercent*zoomAmount)) / 100;
		
		// What is the zoom centre?
		double mousedOverX = oldGraphArea[0] + ( (double)xPixel * pixelSize );
		double mousedOverY = oldGraphArea[1] - ( (double)yPixel * pixelSize );
		
		// Since we're zooming in on a point (the "zoom centre"),
		// let's now shrink each of the distances from the zoom centre
		// to the edges of the picture by a constant percentage.
		double newMinX = mousedOverX - (zoomPercentChange * (mousedOverX-oldGraphArea[0]));
		double newMaxY = mousedOverY - (zoomPercentChange * (mousedOverY-oldGraphArea[1]));
		
		double oldMaxX = oldGraphArea[0] + oldGraphArea[2];
		double newMaxX = mousedOverX - (zoomPercentChange * (mousedOverX-oldMaxX));
		
		double leftWidthDiff = newMinX - oldGraphArea[0];
		double rightWidthDiff = oldMaxX - newMaxX;
		
		newGraphArea[0] = newMinX;
		newGraphArea[1] = newMaxY;
		newGraphArea[2] = oldGraphArea[2] - leftWidthDiff - rightWidthDiff;
		
		setGraphArea(newGraphArea);
	}

	// Returns zoom level, in range 0..ZOOM_SLIDER_SCALING	(logarithmic scale)
	public int getZoomLevel() {
		double lnPixelSize = Math.log(getPixelSize());
		double zoomLevel = (double)ZOOM_SLIDER_SCALING * (lnPixelSize-MINZOOM_LN_PIXEL) / (MAXZOOM_LN_PIXEL-MINZOOM_LN_PIXEL);
		return (int)zoomLevel;
	}
	
	boolean saneZoomLevel() {
		int zoomLevel = getZoomLevel();
		if (
			(zoomLevel >= 1) &&
			(zoomLevel <= ZOOM_SLIDER_SCALING)
		) return true;
		return false;
	}
	
	// Sets zoom, given number in range 0..1000 (logarithmic scale)
	public void setZoomLevel(int zoomLevel) {
		double lnPixelSize = MINZOOM_LN_PIXEL + (zoomLevel * (MAXZOOM_LN_PIXEL-MINZOOM_LN_PIXEL) / (double)ZOOM_SLIDER_SCALING);
		double newPixelSize = Math.exp(lnPixelSize);
		setPixelSize(newPixelSize);
	}
	
	// Given a desired new pixel size, sets - keeping current image centre
	void setPixelSize(double newPixelSize) {
		double[] oldGraphArea = getGraphArea();
		double[] newGraphArea = new double[3];
		
		double centerX = oldGraphArea[0] + (getPixelSize() * getWidth() * 0.5);
		double centerY = oldGraphArea[1] - (getPixelSize() * getWidth() * 0.5);
		
		newGraphArea[0] = centerX - (getWidth() * newPixelSize * 0.5);
		newGraphArea[1] = centerY + (getWidth() * newPixelSize * 0.5);
		newGraphArea[2] = newPixelSize * getWidth();
		
		setGraphArea(newGraphArea);
	}
	
	
	/* Movement */
	public void moveFractal(int dragDiffPixelsX, int dragDiffPixelsY) {
		// What does each pixel correspond to, on the complex plane?
		double pixelSize = getPixelSize();
		
		// Adjust the Graph Area
		double[] newGraphArea = getGraphArea();
		newGraphArea[0] -= (dragDiffPixelsX * pixelSize);
		newGraphArea[1] -= -(dragDiffPixelsY * pixelSize);
		setGraphArea(newGraphArea);
	}
	
	public void startDragging()
	{
		movingBitmap = Bitmap.createBitmap(fractalBitmap);
		bitmapX = 0;
		bitmapY = 0;
		draggingFractal = true;
	}
	
	public void dragFractal(int dragDiffPixelsX, int dragDiffPixelsY) {		
		// Adjust the Graph Area
		bitmapX += dragDiffPixelsX;
		bitmapY += dragDiffPixelsY;
		
		invalidate();
	}
	
	public void stopDragging()
	{
		draggingFractal = false;
		moveFractal(bitmapX, bitmapY);
		invalidate();
	}
	
	public void shiftPixels(int shiftX, int shiftY)
	{
		int height = getHeight();
		int width = getWidth();
		int[] newPixels = new int[height * width];
		
		//Choose rows to copy from
		int rowNum = height - Math.abs(shiftY);
		int origStartRow = (shiftY < 0 ? Math.abs(shiftY) : 0);
		
		//Choose columns to copy from
		int colNum = width - Math.abs(shiftX);
		int origStartCol = (shiftX < 0 ? Math.abs(shiftX) : 0);
		
		//Choose columns to copy to
		int destStartCol = (shiftX < 0 ? 0 : shiftX);
		
		//Copy useful parts into new array
		for (int origY = origStartRow; origY < origStartRow + rowNum; origY++)
		{
			int destY = origY + shiftY;
			System.arraycopy(fractalPixels, (origY * width) + origStartCol, 
							 newPixels, (destY * width) + destStartCol,
							 colNum);
		}
		
		fractalPixels = newPixels;
		
		invalidate();
	}
	
	
	/* Get the iteration scaling factor.
	// Log scale, with values ITERATIONSCALING_MIN .. ITERATIONSCALING_MAX
	// represented by values in range 0..CONTRAST_SLIDER_SCALING*/
	public int getScaledIterationCount() {
		return (int)(
			CONTRAST_SLIDER_SCALING *
			( Math.log(iterationScaling) - Math.log(ITERATIONSCALING_MIN) ) /
			( Math.log(ITERATIONSCALING_MAX) - Math.log(ITERATIONSCALING_MIN) )
		);
	}
	
	/* Set the iteration scaling factor.
	// Log scale, with values ITERATIONSCALING_MIN .. ITERATIONSCALING_MAX
	// represented by values in range 0..CONTRAST_SLIDER_SCALING */
	public void setScaledIterationCount(int scaledIterationCount) {
		if (
			(scaledIterationCount >= 0) &&
			(scaledIterationCount <= CONTRAST_SLIDER_SCALING)
		) {
			iterationScaling = Math.exp(
				Math.log(ITERATIONSCALING_MIN) + (
				(scaledIterationCount * (Math.log(ITERATIONSCALING_MAX) - Math.log(ITERATIONSCALING_MIN))) /
				CONTRAST_SLIDER_SCALING)
			);
			Log.d(TAG, "iterationScaling = " + iterationScaling);
			Log.d(TAG, "scaledIterationCount = " + scaledIterationCount);
			updateDisplay();
		}
	}
	
	/* How many iterations to perform?
	// Empirically determined to be generally exponentially rising, as a function of x = |ln(pixelSize)|
	// ie, maxIterations ~ a(b^x)
	// a, b determined empirically for Mandelbrot/Julia curves
	// The contrast slider allows adjustment of the magnitude of a, with a log scale. */
	int getMaxIterations() {
		// How many iterations to perform?
		double absLnPixelSize = Math.abs(Math.log(getPixelSize()));
		double dblIterations = iterationScaling * ITERATION_CONSTANT_FACTOR * Math.pow(ITERATION_BASE, absLnPixelSize);
		int iterationsToPerform = (int)dblIterations;
		Log.d(TAG, "iterationsToPerform = " + iterationsToPerform);
		return Math.max(iterationsToPerform, MIN_ITERATIONS);
	}
	
	
	// Compute entire pixel grid
	public void computeAllPixels(final int pixelBlockSize) {
		// Nothing to do - stop if called before layout has been sanely set...
		if (getWidth() <= 0) return;
		if (graphArea == null) return;
		
		computePixels(
			fractalPixels,
			pixelBlockSize,
			true,
			0,
			getWidth(),
			0,
			getHeight(),
			graphArea[0],
			graphArea[1],
			getPixelSize(),
			true,
			10000
		);
		
		fractalBitmap = Bitmap.createBitmap(fractalPixels, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
		
		Log.d(TAG, "Checking pixels");
		
		postInvalidate();
	}
	
	
	// Abstract methods
	abstract void loadLocation(MandelbrotJuliaLocation mjLocation);
	abstract void computePixels(
			int[] outputPixelArray,  // Where pixels are output
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
			final int millisBeforeSlowRenderBehaviour  // How many millis before show rendering progress, and (if allowInterruption) before listening for this.
		);
}


