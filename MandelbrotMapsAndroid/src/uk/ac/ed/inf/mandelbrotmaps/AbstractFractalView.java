package uk.ac.ed.inf.mandelbrotmaps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.ed.inf.mandelbrotmaps.FractalActivity.FractalType;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


abstract class AbstractFractalView extends View {
	private final String TAG = "MMaps";
	
	// How many different, discrete zoom and contrast levels?
	public final int ZOOM_SLIDER_SCALING = 300;
	public final int CONTRAST_SLIDER_SCALING = 200;
   
	// Default pixel block sizes for crude, detailed renders
	final int CRUDE_PIXEL_BLOCK = 3;
	final int DEFAULT_PIXEL_SIZE = 1;
	
	
	// How many iterations, at the very fewest, will we do?
	int MIN_ITERATIONS = 10;
	
	// Constants for calculating maxIterations()
	double ITERATION_BASE;
	double ITERATION_CONSTANT_FACTOR;
	
	// Scaling factor for maxIterations() calculations
	double iterationScaling = 0.3;
	double ITERATIONSCALING_DEFAULT = 0.3;
	double ITERATIONSCALING_MIN = 0.01; 
	double ITERATIONSCALING_MAX = 100;
	
	
	// How often to redraw fractal when rendering. Set to 1/12th screen size in onSizeChanged()
	public int linesToDrawAfter = 20;
	
	// Tracks current control (zooming, dragging, or none)
	public static enum ControlMode {
		ZOOMING,
		DRAGGING,
		STATIC
	}
	public ControlMode controlmode = ControlMode.STATIC;
	
	// The size of the fractal view - whether it's the main one or little and in the corner
	public static enum FractalViewSize{
		LARGE,
		LITTLE,
		HALF
	}
	FractalViewSize fractalViewSize;
	
	// Lists for threads, their queues, and their status
	int noOfThreads = 1;
	ArrayList<LinkedBlockingQueue<Rendering>> renderQueueList = new ArrayList<LinkedBlockingQueue<Rendering>>();
	ArrayList<RenderThread> renderThreadList = new ArrayList<RenderThread>();
	ArrayList<Boolean> rendersComplete = new ArrayList<Boolean>();
	
	// What zoom range do we allow? Expressed as ln(pixelSize).
	double MINZOOM_LN_PIXEL = -3;
	double MAXZOOM_LN_PIXEL;
	
	// Graph area on the complex plane? Stored as double[] {x_min, y_max, width}
	double[] graphArea;
	double[] homeGraphArea;
	
	// Fractal image data
	int[] fractalPixels;
	int[] pixelSizes;
	Bitmap fractalBitmap;
	
	// Image position on screen (changes while dragging)
	public float bitmapX = 0;
	public float bitmapY = 0;
	
	// Dragging state
	public float totalDragX = 0;
	public float totalDragY = 0;
	public boolean holdingPin = false;
	
	// Scaling state
	private float totalScaleFactor = 1.0f;
	public float scaleFactor = 1.0f;
	public float midX = 0.0f;
	public float midY = 0.0f;
	
	boolean zoomingFractal = false;
	boolean hasZoomed = false;
	boolean hasPassedMaxDepth = false;
	boolean isAtMaxDepth = false;
	
	// Tracks scaling/ dragging position
	public Matrix matrix;	
	
	// Handle to activity holding the view
	public FractalActivity parentActivity;
	
	// Used to track length of a render
	private long renderStartTime;
	
	// Track number of times bitmap is recreated onDraw (debug info)
	int bitmapCreations = 0;
	
	boolean drawPin = true;
	
	boolean allowCrudeRendering = true;
	
	
	
/*-----------------------------------------------------------------------------------*/
/*Constructor*/
/*-----------------------------------------------------------------------------------*/
	/* Constructor for the view, assigns the parent activity and size and
	 * launches the render threads. */
	public AbstractFractalView(Context context, FractalViewSize size) {
		super(context);
		setFocusable(true);
		setFocusableInTouchMode(true);
      	setId(0); 
      	setBackgroundColor(Color.BLACK);
      
      	parentActivity = (FractalActivity)context;
      	setOnTouchListener(parentActivity);
      	
      	fractalViewSize = size;
      	
      	//Up the iteration count a bit for the little view (decent value, seems to work)
      	if (fractalViewSize == FractalViewSize.LITTLE) {
      		iterationScaling *= 1.5;
      		ITERATIONSCALING_DEFAULT *= 1.5;
      	}
      
      	// Initialise the matrix (not nearly as sinister as it sounds)
      	matrix = new Matrix();
      	matrix.reset();
      	
      	// Create the render threads
      	noOfThreads = Runtime.getRuntime().availableProcessors();
      	Log.d(TAG, "Using " + noOfThreads + " cores");
      	
      	for (int i = 0; i < noOfThreads; i++) {
      		rendersComplete.add(false);
      		renderQueueList.add(new LinkedBlockingQueue<Rendering>());	
      		renderThreadList.add(new RenderThread(this, i, noOfThreads));
      		renderThreadList.get(i).start();
      	}
   }

	
	
/*-----------------------------------------------------------------------------------*/
/*Android life-cycle handling*/   
/*-----------------------------------------------------------------------------------*/
	/* Runs when the view changes size. 
	 * Used to set the little fractal view once large fractal view size has first been determined. 
	 * Also sets linesToDrawAfter to 1/12th of the screen size. */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	   
		// Show the little view at the start, if allowed.
		if(fractalViewSize == FractalViewSize.LARGE && parentActivity.showLittleAtStart) {
			parentActivity.addLittleView();
		}
		
		// Set linesToDrawAfter to a reasonable portion of size (1/12th works nicely).
		linesToDrawAfter = getHeight()/12;
		Log.d(TAG, "Drawing every " + linesToDrawAfter + " lines.");
	}
   
   
   
/*-----------------------------------------------------------------------------------*/
/* Fractal drawing */
/*-----------------------------------------------------------------------------------*/
	/* Draw the fractal Bitmap to the screen, updating it if not dragging or zooming */
	@Override
	protected void onDraw(Canvas canvas) {	   
		// (Re)create pixel grid, if not initialised - or if wrong size.
		if ((fractalPixels == null) || (fractalPixels.length != getWidth()*getHeight())) {
			fractalPixels = new int[getWidth() * getHeight()];
			pixelSizes = new int[getWidth() * getHeight()];
			clearPixelSizes();
			//scheduleNewRenders();
			setGraphArea(graphArea, true);
		}
	
		//Translation
		if (controlmode == ControlMode.DRAGGING) {
			matrix.postTranslate(bitmapX, bitmapY);
			bitmapX = 0;
			bitmapY = 0;
		}
		
		//Scaling
		matrix.postScale(scaleFactor, scaleFactor, midX, midY);
		scaleFactor = 1.0f;
		midX = 0.0f;
		midY = 0.0f;
		
		//Create new image only if not dragging, zooming, or moving the Julia pin
		if(controlmode == ControlMode.STATIC && !holdingPin) {
			bitmapCreations++;
			fractalBitmap = Bitmap.createBitmap(fractalPixels, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
		}
		
		//Draw fractal image on screen
		canvas.drawBitmap(fractalBitmap, matrix, new Paint());
	}
	
	
 	/* Computes pixels of the fractal Bitmap, puts them in array (run by render thread) */
	public void computeAllPixels(final int pixelBlockSize, final int threadID) {
		// Nothing to do - stop if called before layout has been sanely set...
		if (getWidth() <= 0 || graphArea == null)
			return;
		
		int yStart = (getHeight()/2) + (threadID * pixelBlockSize);
		int yEnd = getHeight() - (noOfThreads - (threadID + 1));
		boolean showRenderProgress = (threadID == 0);
		
		if(fractalViewSize == FractalViewSize.LARGE)
			Log.d("ThreadEnding", "Thread " + threadID + " ending at " + yEnd + "/" + getHeight());
			
		if (pixelSizes == null)
			pixelSizes = new int[getWidth() * getHeight()];
		
		// Don't bother showing render progress on little views
		if(fractalViewSize == FractalViewSize.LITTLE) showRenderProgress = false;
		
		computePixels(
			pixelBlockSize,
			showRenderProgress,
			0, 
			getWidth(),
			yStart,
			yEnd,
			graphArea[0],
			graphArea[1],
			getPixelSize(),
			true,
			threadID,
			noOfThreads
		);
		
		postInvalidate();
	}
	

	
/*-----------------------------------------------------------------------------------*/
/* Render scheduling/tracking */
/*-----------------------------------------------------------------------------------*/
	/* Adds renders to queues for each thread */
	void scheduleNewRenders() {		
		// Abort current and future renders
		stopAllRendering();
		
		// New render won't have passed maximum depth, reset check
		hasPassedMaxDepth = false;
		
		// Try showing the progress spinner (won't show if, as normal, it's disallowed)
		if(fractalViewSize == FractalViewSize.LARGE)
			parentActivity.showProgressSpinner();
		
		// Reset all the tracking
		for(int i = 0; i < noOfThreads; i++) {
			rendersComplete.set(i, false);
		}
		
		renderStartTime = System.currentTimeMillis();
		
		
		//Schedule a crude rendering if needed (not the small view, not a small zoom)
		if(allowCrudeRendering && fractalViewSize != FractalViewSize.LITTLE && (totalScaleFactor < 0.6f|| totalScaleFactor == 1.0f || totalScaleFactor > 3.5f)) {
			scheduleRendering(CRUDE_PIXEL_BLOCK);
		}
		totalScaleFactor = 1.0f; // Needs reset once checked, so that next render doesn't account for it.
		
		// Schedule a high-quality rendering
		scheduleRendering(DEFAULT_PIXEL_SIZE);
	}
	
	/* Add a rendering of a particular pixel size (crude or detailed) to the queues */
	void scheduleRendering(int pixelBlockSize) {
		for (int i = 0; i < noOfThreads; i++) {
			renderThreadList.get(i).allowRendering();
			renderQueueList.get(i).add(new Rendering(pixelBlockSize));
		}
	}
	
	/* Stop all rendering, including planned and current */
	void stopAllRendering() {
		for (int i = 0; i < noOfThreads; i++) {
			renderQueueList.get(i).clear();
			renderThreadList.get(i).abortRendering();
		}
	}
	
	
	/* Check if any threads are still rendering (returns true if so) */
	public boolean isRendering() {
		boolean allComplete = true;
		
		for (Boolean b : rendersComplete) {
			allComplete = allComplete && b;
		}
		
		return !allComplete;
	}
	
	/* Mark a thread as having completed a render (called in computePixels())
	 * Does nothing if it's a crude render that finished. */
	public void notifyCompleteRender(int threadID, int pixelBlockSize) {
		// If detailed render has finished, note that thread has completed.
		if(pixelBlockSize == DEFAULT_PIXEL_SIZE) {
			rendersComplete.set(threadID, true);
		}
		
		// If all threads are done and you're the main view, show time.
		if (!(isRendering()) && fractalViewSize == FractalViewSize.LARGE) {
			Log.d(TAG, "Renders completed.");
			
			//Show time in seconds
			double time = (double)((System.currentTimeMillis() - renderStartTime))/1000;
			String renderCompleteMessage = "Rendering time: " + new DecimalFormat("#.##").format(time) + " second" + (time == 1d ? "." : "s.");
			Log.d(TAG, renderCompleteMessage);
			parentActivity.showToastOnUIThread(renderCompleteMessage, Toast.LENGTH_SHORT);	
			
			parentActivity.hideProgressSpinner();
		}
	}
	
	
	
/*-----------------------------------------------------------------------------------*/
/* Movement */
/*-----------------------------------------------------------------------------------*/
	/* Set new location after movement */
	public void moveFractal(int dragDiffPixelsX, int dragDiffPixelsY) {
		Log.d(TAG, "moveFractal()");
		
		// What does each pixel correspond to, on the complex plane?
		double pixelSize = getPixelSize();
		
		// Adjust the Graph Area
		double[] newGraphArea = graphArea;
		newGraphArea[0] -= (dragDiffPixelsX * pixelSize);
		newGraphArea[1] -= -(dragDiffPixelsY * pixelSize);
		setGraphArea(newGraphArea, false);
	}
	
	
	/* Start a dragging motion - clear all the variables, stop any rendering */
	public void startDragging() {			
		controlmode = ControlMode.DRAGGING;
		
		//Stop current rendering (to not render areas that are offscreen afterwards)
		stopAllRendering();
		
		//Clear translation variables
		bitmapX = 0;
		bitmapY = 0;
		totalDragX = 0;
		totalDragY = 0;
		
		hasZoomed = false;
	}
	
	/* Update the position of the image on screen as dragging happens */
	public void dragFractal(float dragDiffPixelsX, float dragDiffPixelsY) {		
		bitmapX = dragDiffPixelsX;
		bitmapY = dragDiffPixelsY;
		
		totalDragX += dragDiffPixelsX;
		totalDragY += dragDiffPixelsY;
		
		invalidate();
	}
	
	/* End of a dragging motion. Move the fractal position to match the image. */
	public void stopDragging(boolean stoppedOnZoom)	{		
		controlmode = ControlMode.STATIC;
		
		// If no zooming's occured, keep the remaining pixels
		if(!hasZoomed && !stoppedOnZoom) {
			shiftPixels((int)totalDragX, (int)totalDragY);
		}
		
		//Set the new location for the fractals
		moveFractal((int)totalDragX, (int)totalDragY);
		
		if(!stoppedOnZoom) setGraphArea(graphArea, true);//scheduleNewRenders();
		
		// Reset all the variables (possibly paranoid)
		if(!hasZoomed && !stoppedOnZoom) 
			matrix.reset();

		hasZoomed = false;
		
		invalidate();
	}

	
	/* Shift values in pixel array to keep pixels that have already been calculated */
	public void shiftPixels(int shiftX, int shiftY) {
		Log.d(TAG, "Shifting pixels");
		
		int height = getHeight();
		int width = getWidth();
		int[] newPixels = new int[height * width];
		int[] newSizes = new int[height * width];
		for (int i = 0; i < newSizes.length; i++) newSizes[i] = 1000;
		
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
			System.arraycopy(pixelSizes, (origY * width) + origStartCol, 
					 newSizes, (destY * width) + destStartCol,
					 colNum);
		}
		
		//Set values
		fractalPixels = newPixels;
		pixelSizes = newSizes;
	}
	
	
	
/*-----------------------------------------------------------------------------------*/
/* Zooming */	
/*-----------------------------------------------------------------------------------*/
	/* Adjust zoom, centred on pixel (xPixel, yPixel) */
	public void zoomChange(int xPixel, int yPixel, float scale) {
		stopAllRendering();
		
		double pixelSize = getPixelSize();
		
		double[] oldGraphArea = graphArea;
		double[] newGraphArea = new double[3];
		
		double zoomPercentChange = (double)scale; //= (double)(100 + (zoomAmount)) / 100;
		
		// What is the zoom centre?
		double zoomCentreX = oldGraphArea[0] + ( (double)xPixel * pixelSize );
		double zoomCentreY = oldGraphArea[1] - ( (double)yPixel * pixelSize );
		
		// Since we're zooming in on a point (the "zoom centre"),
		// let's now shrink each of the distances from the zoom centre
		// to the edges of the picture by a constant percentage.
		double newMinX = zoomCentreX - (zoomPercentChange * (zoomCentreX-oldGraphArea[0]));
		double newMaxY = zoomCentreY - (zoomPercentChange * (zoomCentreY-oldGraphArea[1]));
		
		double oldMaxX = oldGraphArea[0] + oldGraphArea[2];
		double newMaxX = zoomCentreX - (zoomPercentChange * (zoomCentreX-oldMaxX));
		
		double leftWidthDiff = newMinX - oldGraphArea[0];
		double rightWidthDiff = oldMaxX - newMaxX;
		
		newGraphArea[0] = newMinX;
		newGraphArea[1] = newMaxY;
		newGraphArea[2] = oldGraphArea[2] - leftWidthDiff - rightWidthDiff;
		
		Log.d(TAG, "Just zoomed - zoom level is " + getZoomLevel());
		
		setGraphArea(newGraphArea, false);
	}
	
	
	/* Start a zooming gesture */
	public void startZooming(float initialMidX, float initialMidY)
	{
		controlmode = ControlMode.ZOOMING;
		hasZoomed = true;
		clearPixelSizes();
	}
	
	/* Updates zoom level during a scale gesture, but doesn't re-render */
	public void zoomImage(float focusX, float focusY, float newScaleFactor) {
		midX = focusX;
		midY = focusY;
		scaleFactor = newScaleFactor;
		totalScaleFactor *= newScaleFactor;
		
		// Change zoom, but don't re-render
		zoomChange((int)focusX, (int)focusY, 1/newScaleFactor);
		
		invalidate();
	}
	
	/* End a zooming gesture (crop bitmap to screen, re-render) */
	public void stopZooming()
	{		
		clearPixelSizes();
		
		Log.d(TAG, "Total scale factor = " + totalScaleFactor);
		
		controlmode = ControlMode.DRAGGING;
		
		stopAllRendering();
		
		drawPin = false;
		setDrawingCacheEnabled(true);
		fractalBitmap = Bitmap.createBitmap(getDrawingCache());
		fractalBitmap.getPixels(fractalPixels, 0, getWidth(), 0, 0, getWidth(), getHeight());
		setDrawingCacheEnabled(false);
		drawPin = true;
		
		bitmapX = 0;
		bitmapY = 0;
		totalDragX = 0;
		totalDragY = 0;
		matrix.reset();
	}
	
	
	/* Returns zoom level, in range 0..ZOOM_SLIDER_SCALING	(logarithmic scale) */
	public int getZoomLevel() {
		double pixelSize = getPixelSize();
		
		// If the pixel size = 0, something's wrong (happens at Julia launch). 
		if (pixelSize == 0.0d)
			return 1;
		
		double lnPixelSize = Math.log(pixelSize);
		double zoomLevel = (double)ZOOM_SLIDER_SCALING * (lnPixelSize-MINZOOM_LN_PIXEL) / (MAXZOOM_LN_PIXEL-MINZOOM_LN_PIXEL);
		return (int)zoomLevel;
	}
	
	/* Checks if this zoom level if sane (within the chosen limits) */
	boolean saneZoomLevel() {
		int zoomLevel = getZoomLevel();
		Log.d(TAG, "Zoom level - " + zoomLevel);
		
		if ((zoomLevel >= 1) &&	(zoomLevel <= ZOOM_SLIDER_SCALING)) {
			return true;
		}
		else {
			return false;
		}
	}
	

	
/*-----------------------------------------------------------------------------------*/
/* Iteration variables */
/*-----------------------------------------------------------------------------------*/
	/* Get the iteration scaling factor.
	 * Log scale, with values ITERATIONSCALING_MIN .. ITERATIONSCALING_MAX
	 * represented by values in range 0..CONTRAST_SLIDER_SCALING */
	public int getScaledIterationCount() {
		return (int)(
			CONTRAST_SLIDER_SCALING *
			( Math.log(iterationScaling) - Math.log(ITERATIONSCALING_MIN) ) /
			( Math.log(ITERATIONSCALING_MAX) - Math.log(ITERATIONSCALING_MIN) )
		);
	}
	
	/* Set the iteration scaling factor.
	 * Log scale, with values ITERATIONSCALING_MIN .. ITERATIONSCALING_MAX
	 * represented by values in range 0..CONTRAST_SLIDER_SCALING */
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
			//scheduleNewRenders();
			setGraphArea(graphArea, true);
		}
	}
	
	/* How many iterations to perform?
	 * Empirically determined to be generally exponentially rising, as a function of x = |ln(pixelSize)|
	 * ie, maxIterations ~ a(b^x)
	 * a, b determined empirically for Mandelbrot/Julia curves
	 * The contrast slider (not implemented yet, was in the original web applet)
	 *  allows adjustment of the magnitude of a, with a log scale. */
	int getMaxIterations() {
		// How many iterations to perform?
		double absLnPixelSize = Math.abs(Math.log(getPixelSize()));
		double dblIterations = iterationScaling * ITERATION_CONSTANT_FACTOR * Math.pow(ITERATION_BASE, absLnPixelSize);
		
		int iterationsToPerform = (int)dblIterations;
		
		return Math.max(iterationsToPerform, MIN_ITERATIONS);
	}
	
	
	
/*-----------------------------------------------------------------------------------*/
/* Graph area */
/*-----------------------------------------------------------------------------------*/
	/* Set a new graph area, if valid */
	void setGraphArea(double[] newGraphArea, boolean newRender) {
		// If a graph area already exists, be picky. 
		//Don't bother checking for validity on the little view - zoom never changes
		if (graphArea != null && fractalViewSize == FractalViewSize.LARGE) {
			double[] initialGraphArea = graphArea;
			graphArea = newGraphArea;
			
			// Check for sane zoom level.
			if (saneZoomLevel()) {
				if(newRender) {
					if (hasPassedMaxDepth) {
						parentActivity.showToastOnUIThread("Maximum zoom depth reached.", Toast.LENGTH_SHORT);
					}
					Log.d(TAG, "New graph area xmin: " + graphArea[0]);
					scheduleNewRenders();
				}
			}
			else {
				// Zoom level is out of bounds, just roll back.
				hasPassedMaxDepth = true;
				graphArea = initialGraphArea;
			}
		}
		else {
			// There is no predefined graphArea; we'll have to accept whatever newGraphArea is.
			graphArea = newGraphArea;
			if(newRender) scheduleNewRenders();
		}
	}
	
	/* Compute length of 1 pixel on the complex plane */
	double getPixelSize() {
		// Nothing to do - cannot compute a sane pixel size
		if (getWidth() == 0) return 0.0;
		if (graphArea == null) return 0.0;
		
		// Return the pixel size
		return (graphArea[2] / (double)getWidth());
	}
	
	
	/* Reset the fractal to the home graph area */
	public void canvasHome() {
		stopAllRendering();
		clearPixelSizes();
		
		// Default max iterations scaling
		//iterationScaling = ITERATIONSCALING_DEFAULT;
		
		Log.d(TAG, "Reset hit.");
		
		// Default graph area		
		if (parentActivity.fractalType == FractalType.MANDELBROT) {
			setGraphArea(new MandelbrotJuliaLocation().defaultMandelbrotGraphArea, true);
		}
		else
			setGraphArea(new MandelbrotJuliaLocation().defaultJuliaGraphArea, true);
	}

	
	
/*-----------------------------------------------------------------------------------*/
/* File saving */
/*-----------------------------------------------------------------------------------*/
	/* Saves the current fractal image as an image file 
	 * (Call in FractalActivity should ensure that image is done rendering) */
	public File saveImage() {
		//Check if external storage is available
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			String filename = getNewFileName();
			File imagefile = new File(path, "FractalImage" + filename + ".png");
			
			//Check if it exists, try extending the names a few times if it does
			int nameTries = 0;
			while(imagefile.exists()) {
				nameTries++;
				filename += "a";
				imagefile = new File(path, "FractalImage" + filename + ".png");
				if(nameTries > 1)
					return null;
			}
			
			try {
				//Check pictures directory already exists
				path.mkdir();
				
				//Open file output stream
				FileOutputStream output = new FileOutputStream(imagefile);
				
				/*Recreate the bitmap - all the render thread completion guarantees is that the arrays
				are full. onDraw() may not have run before saving.*/
				fractalBitmap = Bitmap.createBitmap(fractalPixels, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
				fractalBitmap.compress(Bitmap.CompressFormat.PNG, 90, output);

				output.close();				
				Log.d(TAG, "Wrote image out to " + imagefile.getAbsolutePath());
			}
			catch (IOException ioe)
			{
				//File writing failed
				Log.d(TAG, "Could not write image file to " + imagefile.getAbsolutePath());
				parentActivity.showToastOnUIThread("Unable to write file.", Toast.LENGTH_LONG);
			}
			
			return imagefile;
		}
		else
		{
			parentActivity.showToastOnUIThread("Unable to write file.", Toast.LENGTH_LONG);
			return null;
		}
	}
	
	/* Generates a new filename (currently uses time down to the second) */
	private String getNewFileName() {
		String datetime = "";
		
		Calendar currentTime = Calendar.getInstance();
		datetime += currentTime.get(Calendar.YEAR) + "-";
		datetime += (currentTime.get(Calendar.MONTH) + 1) + "-";	//Add 1 because months start from 0, apparently.
		datetime += currentTime.get(Calendar.DAY_OF_MONTH) + "-";
		datetime += currentTime.get(Calendar.HOUR_OF_DAY) + "-";
		datetime += currentTime.get(Calendar.MINUTE) + "-";
		datetime += currentTime.get(Calendar.SECOND);
		
		return datetime;
	}
	
	
	
/*-----------------------------------------------------------------------------------*/
/* Thread handling  */
/*-----------------------------------------------------------------------------------*/
	/* Interrupt (and end) the rendering threads, called when activity closing */
	public void interruptThreads(){
		for (RenderThread thread : renderThreadList) {
			thread.interrupt();
		}
	}
	
	
	/* Retrieve and remove the next rendering from a queue (used by render threads) */
	public Rendering getNextRendering(int threadID) throws InterruptedException {
		return renderQueueList.get(threadID).take();
	}
	
	
	
/*-----------------------------------------------------------------------------------*/
/* Utilities (miscellaneous useful functions)  */
/*-----------------------------------------------------------------------------------*/
	/* Clear the sizes array of its current values, so anything new is smaller
	 * (Fills it with 1000s) */
	private void clearPixelSizes() {
		Log.d(TAG, "Clearing pixel sizes");
		//pixelSizes = new int[getWidth() * getHeight()];
		
		for (int i = 0; i < pixelSizes.length; i++)
		{
			pixelSizes[i] = 1000;
		}
	   }
	
	/* Stop any rendering and return to "home" position */
	public void reset(){
		stopAllRendering();
		
		bitmapCreations = 0;

		matrix.reset();
		fractalPixels = new int[getWidth() * getHeight()];
		clearPixelSizes();
		canvasHome();
		
		//postInvalidate();
	}
	
	/* Sets to a predetermined spot that takes a while to render (just used for debugging) */
	public void setToBookmark()
	{	
		stopAllRendering();
		
		clearPixelSizes();
		
		double[] bookmark = new double[3];
		
		bookmark[0] = -1.631509065569354;
		bookmark[1] = 0.0008548063308817164;
		bookmark[2] = 0.0027763525271276013;
		
		Log.d(TAG, "Jumping to bookmark");
		
		setGraphArea(bookmark, true);
	}
	
	
	
/*-----------------------------------------------------------------------------------*/
/* Abstract methods */
/*-----------------------------------------------------------------------------------*/
	abstract void loadLocation(MandelbrotJuliaLocation mjLocation);
	abstract void computePixels(
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
			final int noOfThreads
		);
}


