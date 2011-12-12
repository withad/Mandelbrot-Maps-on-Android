package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

class BitmapDraggingView extends View {
   
   private static final String TAG = "FractalView";

   private int WIDTH;
   private int HEIGHT;
   private int STRIDE;
   
   // How many different, discrete zoom and contrast levels?
   public static final int ZOOM_SLIDER_SCALING = 300;
   public static final int CONTRAST_SLIDER_SCALING = 200;
   
   // Default "crude rendering" pixel block size?
   int INITIAL_PIXEL_BLOCK = 2;
   
   // How much of a zoom, on each increment?
   public static final int zoomPercent = 20;
   
   // Rendering queue	
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
	double iterationScaling = 1;
	double ITERATIONSCALING_MIN = 0.01;
	double ITERATIONSCALING_MAX = 100;
	double ITERATIONSCALING_DEFAULT = 1;
	
	// Mouse dragging state.
	int dragLastX = 0;
	int dragLastY = 0;
	
	public int bitmapX = 0;
	public int bitmapY = 0;
	
	// Graph Area on the Complex Plane? new double[] {x_min, y_max, width}
	double[] graphArea;
	double[] homeGraphArea;
	
	// Pixel colours
	int[] pixelIterations;
	Bitmap bitmapPixels;
	
   
   public BitmapDraggingView(Context context) {
      super(context);
      setFocusable(true);
      setFocusableInTouchMode(true);
      setBackgroundColor(Color.BLUE);
      setId(0); 
      
      setOnTouchListener((BitmapActivity)context);
   }

   
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
		(pixelIterations == null)
	) {
		pixelIterations = new int[200*200];
		fillBitmap();
	}
	if(bitmapPixels != null)
	{
		Matrix matrix = new Matrix();
		matrix.reset();
		matrix.setScale(0.5f, 0.5f);
		canvas.setMatrix(matrix);
		canvas.drawBitmap(bitmapPixels, bitmapX, bitmapY, new Paint());
	}
		
   }
	
   private void fillBitmap() {
	   for (int i = 0; i < 200*200; i++)
	   {
		   pixelIterations[i] = Color.GREEN;
	   }
	   
	   bitmapPixels = Bitmap.createBitmap(pixelIterations, 0, 200, 200, 200, Bitmap.Config.RGB_565);	
}


// Called when we want to recompute everything
	void updateDisplay() {		
	}
   
	
	/* Utilities */
	// Get a handle on Parent
	public void setParentHandle(FractalActivity parentHandle) {
		parentActivity = parentHandle;
	}
	
	public double[] getGraphArea() {
		return graphArea;
	}
	
	// Do we need a crude rendering?
	boolean needCrudeRendering() {
		return false;
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
	
	// Shift the canvas x pixels right; y pixels up
	public void dragCanvas(int dragDiffPixelsX, int dragDiffPixelsY) {
		// What does each pixel correspond to, on the complex plane?
		double pixelSize = getPixelSize();
		
		// Adjust the Graph Area
		bitmapX += dragDiffPixelsX;
		bitmapY += dragDiffPixelsY;
		
		invalidate();
	}
	
	// Get the iteration scaling factor.
	// Log scale, with values ITERATIONSCALING_MIN .. ITERATIONSCALING_MAX
	// represented by values in range 0..CONTRAST_SLIDER_SCALING
	public int getScaledIterationCount() {
		return (int)(
			CONTRAST_SLIDER_SCALING *
			( Math.log(iterationScaling) - Math.log(ITERATIONSCALING_MIN) ) /
			( Math.log(ITERATIONSCALING_MAX) - Math.log(ITERATIONSCALING_MIN) )
		);
	}
	
	// Set the iteration scaling factor.
	// Log scale, with values ITERATIONSCALING_MIN .. ITERATIONSCALING_MAX
	// represented by values in range 0..CONTRAST_SLIDER_SCALING
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
			updateDisplay();
		}
	}
}


