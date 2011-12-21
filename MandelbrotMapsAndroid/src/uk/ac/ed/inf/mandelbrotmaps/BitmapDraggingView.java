package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
	
	Bitmap image;
	float prevScaleFactor = 1.0f;
	int prevX = 0;
	int prevY = 0;
	
	public float scaleFactor = 1.0f;
	public float midX = 0.0f;
	public float midY = 0.0f;
	
	Matrix matrix;
	
   
   public BitmapDraggingView(Context context) {
      super(context);
      setFocusable(true);
      setFocusableInTouchMode(true);
      setBackgroundColor(Color.BLUE);
      setId(0); 
      
      image = BitmapFactory.decodeResource(getResources(), R.drawable.image);
      image = Bitmap.createScaledBitmap(image, 1280, 800, true);
      
      matrix = new Matrix();
      matrix.reset();

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
	if(image != null)
	{		
		if(bitmapX != prevX)
		{
			matrix.postTranslate(bitmapX, 0);
			prevX = bitmapX;
		}
		
		if(bitmapY != prevY)
		{
			matrix.postTranslate(0, bitmapY);
			prevY = bitmapY;
		}
		
		if(scaleFactor != prevScaleFactor)
		{
			matrix.postScale(scaleFactor, scaleFactor, midX, midY);
			prevScaleFactor = scaleFactor;
		}
				
		Bitmap newImage = Bitmap.createBitmap(image, 0, 0, getWidth(), getHeight(), matrix, true);
		
		canvas.drawBitmap(image, matrix, new Paint());
		//canvas.drawBitmap(newImage, new Matrix, new Paint());
	}
		
   }


	
	/* Utilities */	
	// Shift the canvas x pixels right; y pixels up
	public void dragCanvas(int dragDiffPixelsX, int dragDiffPixelsY) {
		// Adjust the Graph Area
		bitmapX = dragDiffPixelsX;
		bitmapY = dragDiffPixelsY;
		
		invalidate();
	}
}


