package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

public class FractalView extends View {
   
   private static final String TAG = "FractalView";
   private Canvas canvas;

   private int WIDTH;
   private int HEIGHT;
   private int STRIDE;
   
   int[] colors;
   
   public FractalView(Context context) {
      super(context);
      setFocusable(true);
      setFocusableInTouchMode(true);
      setBackgroundColor(Color.BLUE);
      setId(0); 
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
      colors = makeArray();
   }
   

   @Override
   protected void onDraw(Canvas canvas) {
	   this.canvas = canvas;      
	   
	   Bitmap bitmap = Bitmap.createBitmap(colors, 0, getWidth(), getWidth(), getHeight(), Bitmap.Config.RGB_565);
	   canvas.drawBitmap(bitmap, 0,0, new Paint()); 
   }

   
   public int[] makeArray()
   {
	   colors = new int[getHeight() * getWidth()];	   
	   computePixels(1, 0, getWidth(), 0, getHeight(), -10, 10, 0.06, false, 300);	   
	   return colors;
   }

// Iterate a rectangle of pixels, in range (xPixelMin, yPixelMin) to (xPixelMax, yPixelMax)
	void computePixels(
		int pixelBlockSize,  // Pixel "blockiness"
		final int xPixelMin,
		final int xPixelMax,
		final int yPixelMin,
		final int yPixelMax,
		final double xMin,
		final double yMax,
		final double pixelSize,
		final boolean allowInterruption,  // Shall we abort if renderThread signals an abort?
		final int millisBeforeSlowRenderBehaviour  // How many millis before show rendering progress, and (if allowInterruption) before listening for this.
	) {
		int maxIterations = 10;
		int imgWidth = xPixelMax - xPixelMin;
		
		int xPixel, yPixel, iterationNr;
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
		
		long timeBegin = System.currentTimeMillis();
	
		for (yPixel=yPixelMin; yPixel<yPixelMax+1-pixelBlockSize; yPixel+=pixelBlockSize) {			
			// Set y0 (im part of c)
			y0 = yMax - ( (double)yPixel * pixelSize );
		
			for (xPixel=xPixelMin; xPixel<xPixelMax+1-pixelBlockSize; xPixel+=pixelBlockSize) {
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
						colors[imgWidth*(yPixel+pixelBlockB) + (xPixel+pixelBlockA)] = colourCodeHex;
					}
				}
			}
		}
	}
}

