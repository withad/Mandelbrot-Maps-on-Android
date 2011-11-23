/***
 * Excerpted from "Hello, Android! 3e",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/eband3 for more book information.
***/

package org.example.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

public class GraphicsView extends View {
   
   private static final String TAG = "Sudoku";
   public Paint paint = new Paint();
   private Canvas canvas;

   private final Fractal game;
   
   public GraphicsView(Context context) {
      
      super(context);
      this.game = (Fractal) context;
      setFocusable(true);
      setFocusableInTouchMode(true);
      paint.setColor(Color.GREEN);
      
      // ...
      setId(0); 
   }

   @Override
   protected Parcelable onSaveInstanceState() { 
      Parcelable p = super.onSaveInstanceState();
      Log.d(TAG, "onSaveInstanceState");
      Bundle bundle = new Bundle();
//      bundle.putInt(SELX, selX);
//      bundle.putInt(SELY, selY);
//      bundle.putParcelable(VIEW_STATE, p);
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
   }
   

   @Override
   protected void onDraw(Canvas canvas) {
	   this.canvas = canvas;      
	   drawFullColour();
   }

public void drawFullColour() {
	//paint.setColor(Color.BLUE);
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

   /*
   @Override
   public boolean onTouchEvent(MotionEvent event) {
      if (event.getAction() != MotionEvent.ACTION_DOWN)
         return super.onTouchEvent(event);

      select((int) (event.getX() / width),
            (int) (event.getY() / height));
      game.showKeypadOrError(selX, selY);
      Log.d(TAG, "onTouchEvent: x " + selX + ", y " + selY);
      return true;
   }

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
      Log.d(TAG, "onKeyDown: keycode=" + keyCode + ", event="
            + event);
      switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_UP:
         select(selX, selY - 1);
         break;
      case KeyEvent.KEYCODE_DPAD_DOWN:
         select(selX, selY + 1);
         break;
      case KeyEvent.KEYCODE_DPAD_LEFT:
         select(selX - 1, selY);
         break;
      case KeyEvent.KEYCODE_DPAD_RIGHT:
         select(selX + 1, selY);
         break;
      case KeyEvent.KEYCODE_0:
      case KeyEvent.KEYCODE_SPACE: setSelectedTile(0); break;
      case KeyEvent.KEYCODE_1:     setSelectedTile(1); break;
      case KeyEvent.KEYCODE_2:     setSelectedTile(2); break;
      case KeyEvent.KEYCODE_3:     setSelectedTile(3); break;
      case KeyEvent.KEYCODE_4:     setSelectedTile(4); break;
      case KeyEvent.KEYCODE_5:     setSelectedTile(5); break;
      case KeyEvent.KEYCODE_6:     setSelectedTile(6); break;
      case KeyEvent.KEYCODE_7:     setSelectedTile(7); break;
      case KeyEvent.KEYCODE_8:     setSelectedTile(8); break;
      case KeyEvent.KEYCODE_9:     setSelectedTile(9); break;
      case KeyEvent.KEYCODE_ENTER:
      case KeyEvent.KEYCODE_DPAD_CENTER:
         game.showKeypadOrError(selX, selY);
         break;
      default:
         return super.onKeyDown(keyCode, event);
      }
      return true;
   }

   public void setSelectedTile(int tile) {
      if (game.setTileIfValid(selX, selY, tile)) {
         invalidate();// may change hints
      } else {
         // Number is not valid for this tile
         Log.d(TAG, "setSelectedTile: invalid: " + tile);
         startAnimation(AnimationUtils.loadAnimation(game,
               R.anim.shake));
      }
   }

   private void select(int x, int y) {
      invalidate(selRect);
      selX = Math.min(Math.max(x, 0), 8);
      selY = Math.min(Math.max(y, 0), 8);
      getRect(selX, selY, selRect);
      invalidate(selRect);
   }

   private void getRect(int x, int y, Rect rect) {
      rect.set((int) (x * width), (int) (y * height), (int) (x
            * width + width), (int) (y * height + height));
   }
   */
   
   // ...
}

