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
   int INITIAL_PIXEL_BLOCK = 2;
   
   // How much of a zoom, on each increment?
   public static final int zoomPercent = 20;
   
   // Rendering queue (modified from a LinkedBlockingDeque
   LinkedBlockingQueue<CanvasRendering> renderingQueue = new LinkedBlockingQueue<CanvasRendering>();	
   CanvasRenderThread renderThread = new CanvasRenderThread(this);
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
	
	// Graph Area on the Complex Plane? new double[] {x_min, y_max, width}
	double[] graphArea;
	double[] homeGraphArea;
	
	// Pixel colours
	int[] pixelIterations;
	Bitmap bitmapPixels;
	
   
   public AbstractFractalView(Context context) {
      super(context);
      setFocusable(true);
      setFocusableInTouchMode(true);
      setBackgroundColor(Color.BLUE);
      setId(0); 
      
      renderThread.start();
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
		(pixelIterations == null) ||
		(pixelIterations.length != getWidth()*getHeight())
	) {
		pixelIterations = new int[getWidth() * getHeight()];
		//bitmapPixels = new MemoryImageSource(getDimensions().width, getDimensions().height, pixelIterations, 0, getDimensions().width);
		//Bitmap.createBitmap(pixelIterations, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
		updateDisplay();
	}
	
	if(bitmapPixels != null)
	{
		//bitmapPixels = Bitmap.createBitmap(pixelIterations, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
		canvas.drawBitmap(bitmapPixels, 0,0, new Paint());
	}
	else Log.d(TAG, "No bitmap");
   }
	
   // Called when we want to recompute everything
	void updateDisplay() {		
		// Abort future rendering queue. If in real-time mode, interrupt current rendering too
		stopAllRendering();
		
		// If in real-time mode, schedule a crude rendering
		if (needCrudeRendering()) 
			scheduleRendering(3);
		
		// Schedule a high-quality rendering
		scheduleRendering(1);
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
		return getMaxIterations() > 50;
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
	
	// Shift the canvas x pixels right; y pixels up
	public void dragCanvas(int dragDiffPixelsX, int dragDiffPixelsY) {
		// What does each pixel correspond to, on the complex plane?
		double pixelSize = getPixelSize();
		
		// Adjust the Graph Area
		double[] newGraphArea = getGraphArea();
		newGraphArea[0] -= (dragDiffPixelsX * pixelSize);
		newGraphArea[1] -= (dragDiffPixelsY * pixelSize);
		setGraphArea(newGraphArea);
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
			Log.d(TAG, "iterationScaling = " + iterationScaling);
			Log.d(TAG, "scaledIterationCount = " + scaledIterationCount);
			updateDisplay();
		}
	}
	
	// How many iterations to perform?
	// Empirically determined to be generally exponentially rising, as a function of x = |ln(pixelSize)|
	// ie, maxIterations ~ a(b^x)
	// a, b determined empirically for Mandelbrot/Julia curves
	// The contrast slider allows adjustment of the magnitude of a, with a log scale.
	int getMaxIterations() {
		// How many iterations to perform?
		double absLnPixelSize = Math.abs(Math.log(getPixelSize()));
		double dblIterations = iterationScaling * ITERATION_CONSTANT_FACTOR * Math.pow(ITERATION_BASE, absLnPixelSize);
		int iterationsToPerform = (int)dblIterations;
		Log.d(TAG, "iterationsToPerform = " + iterationsToPerform);
		return Math.max(iterationsToPerform/2, MIN_ITERATIONS);
	}
	
	// Compute entire pixel grid
	// "pixelBlockSize" is how many real pixels each of our pixels should span.
	// eg, "3" will result in each of our "pixels" being 3x3 = 9 real pixels.
	public void computeAllPixels(final int pixelBlockSize) {
		// Nothing to do - stop if called before layout has been sanely set...
		if (getWidth() <= 0) return;
		if (graphArea == null) return;
		
		computePixels(
			pixelIterations,
			pixelBlockSize,
			bitmapPixels,
			true,
			0,
			getWidth(),
			0,
			getHeight(),
			graphArea[0],
			graphArea[1],
			getPixelSize(),
			false,
			10000
		);
		
		bitmapPixels = Bitmap.createBitmap(pixelIterations, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
		
		Log.d(TAG, "Checking pixels");
		
		postInvalidate();
	}
	
	/* File saving, ignore for now
	// Save canvas area as a newly-generated image file
	public void saveFile(File fileToSave, ProgressMonitor progressMonitor, final int imgWidth, final int imgHeight) {
		try {
			double imgAspectRatio = (double)imgWidth / (double)imgHeight;
			double canvasAspectRatio = (double)getSize().width / (double)getSize().height;
		
			// Make image as wide/tall as possible without cropping, then center it.
			double xMin, yMax, pixelSize;
			if (imgAspectRatio < canvasAspectRatio) {
				xMin = graphArea[0];
				pixelSize = graphArea[2] / (double)imgWidth;
				yMax = graphArea[1] + 0.5 * pixelSize * (double)(imgHeight - getSize().height);
			} else {
				double origPixelSize = getPixelSize();
				yMax = graphArea[1];
				pixelSize = origPixelSize * ((double)getSize().height / (double)imgHeight);
				xMin = graphArea[0] - 0.5 * ( (pixelSize*(double)imgWidth) - (graphArea[2]) );
			}
		
			BufferedImage bufferedImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
			int[] filePixels = new int[imgWidth * imgHeight];
			computePixels(
				filePixels,
				1,
				null,
				false,
				0,
				imgWidth,
				0,
				imgHeight,
				xMin,
				yMax,
				pixelSize,
				false,
				0
			);
			MemoryImageSource misFile = new MemoryImageSource(
				imgWidth,
				imgHeight,
				filePixels,
				0,
				imgWidth
			);
			bufferedImage.createGraphics().drawImage( createImage(misFile), 0, 0, null );
		
			// Now is a good garbage collection time...
			// We don't want to hit the Java applet upper bound on memory usage.
			filePixels = null;
			misFile = null;
			System.gc();
		
			// Write out to the file
			ImageIO.write(bufferedImage, "png", fileToSave);
		} catch(OutOfMemoryError e) {
			JOptionPane.showMessageDialog(
				this,
				String.format("Sorry, there is insufficient free memory available (%,dMB).%nYou may try saving the image at a smaller size, or saving only one image at a time.", (Runtime.getRuntime().freeMemory() / 1048576)),
				"Out of memory",
				JOptionPane.ERROR_MESSAGE
			);
			fileToSave.delete();
		} catch(IOException e) {
			JOptionPane.showMessageDialog(
				this,
				String.format("Could not write to file '%s' (%s).", fileToSave, e.getMessage()),
				"Unable to write file",
				JOptionPane.ERROR_MESSAGE
			);
			fileToSave.delete();
		}
	}*/
	
	
	/*	Mouse handling stuff
	// Mouse wheel: Adjust zoom level - if in real-time mode
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoomChange(e.getX(), e.getY(), e.getWheelRotation());
	}
	
	// "Click and Drag" in Java is mousePressed, (mouseDragged)*, mouseReleased. Each mouseDragged is 1px move (horizontal | vertical | diagonal)
	public void mousePressed(MouseEvent e) {	
		// Remember mouse position
		dragLastX = e.getX();
		dragLastY = e.getY();
	}
	
	// Mouse drag: Pans the canvas
	public void mouseDragged(MouseEvent e) {
		// If in real time mode, enable dragging.
		// How has the mouse moved? Vars should each be one of: {-1, 0, 1}
		int dragDiffPixelsX = e.getX() - dragLastX;
		int dragDiffPixelsY = -(e.getY() - dragLastY);

		// Move the canvas
		dragCanvas(dragDiffPixelsX, dragDiffPixelsY);

		// Update last mouse position
		dragLastX = e.getX();
		dragLastY = e.getY();
	}
	*/
	
	
	// Abstract methods
	abstract void loadLocation(MandelbrotJuliaLocation mjLocation);
	abstract void computePixels(
			int[] outputPixelArray,  // Where pixels are output
			int pixelBlockSize,  // Pixel "blockiness"
			final Bitmap outputMIS,  // Memory image source to get newPixels() on
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


