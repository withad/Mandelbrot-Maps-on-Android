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

public class GraphicsView extends View {
   
   private static final String TAG = "Sudoku";
   public Paint paint = new Paint();
   private Canvas canvas;

   private int WIDTH = 50;
   private int HEIGHT = 50;
   private int STRIDE = 64;
   
   int[] colors;
   
   private final Fractal game;
   
   public GraphicsView(Context context) {
      
      super(context);
      this.game = (Fractal) context;
      setFocusable(true);
      setFocusableInTouchMode(true);
      paint.setColor(Color.GREEN);
      
      //colors = makeArray();
      
      setId(0); 
   }

   
   @Override
   protected Parcelable onSaveInstanceState() { 
      Parcelable p = super.onSaveInstanceState();
      Log.d(TAG, "onSaveInstanceState");
      Bundle bundle = new Bundle();
      return bundle;
   }
   
   
   @Override
   protected void onRestoreInstanceState(Parcelable state) { 
      Log.d(TAG, "onRestoreInstanceState");
      Bundle bundle = (Bundle) state;
      //select(bundle.getInt(SELX), bundle.getInt(SELY));
      //super.onRestoreInstanceState(bundle.getParcelable(VIEW_STATE));
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
	   //drawFullColour();
	   
	   Bitmap bitmap = Bitmap.createBitmap(colors, 0, STRIDE, WIDTH, HEIGHT, Bitmap.Config.RGB_565);

	   canvas.drawBitmap(bitmap, 0,0, paint); 
   }

   
   public int[] makeArray()
   {
	   int[] colors = new int[getHeight() * getWidth()];
	   
	   for (int y = 0; y < HEIGHT; y++) {
           for (int x = 0; x < WIDTH; x++) {
               int col = Color.BLUE;
               colors[y * STRIDE + x] = col;//(a << 24) | (r << 16) | (g << 8) | b;
           }
       }
	   
	   return colors;
   }
   
   
   public void drawFullColour() {
		int ImageWidth = getWidth();
	    int ImageHeight = getHeight();
	    
	    for (int x = 0; x < ImageWidth; x++)
	    {
	  	  for (int y = 0; y < ImageHeight; y++)
	  	  {
	  		  canvas.drawPoint((float)x, (float)y, paint);
	  	  }
	    }
	}
}

