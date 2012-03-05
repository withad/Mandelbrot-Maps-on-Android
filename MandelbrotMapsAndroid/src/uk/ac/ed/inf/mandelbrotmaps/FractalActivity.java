package uk.ac.ed.inf.mandelbrotmaps;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.FractalViewSize;
import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.RenderStyle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView.SavedState;
import android.widget.Toast;

public class FractalActivity extends Activity implements OnTouchListener, OnScaleGestureListener {

	private boolean currentlyDragging = false;
	
	public enum FractalType {
		MANDELBROT,
		JULIA
	}
	
	private FractalType fractalType = FractalType.MANDELBROT;
	
	
	private enum DisplayMode{
		MANDELBROT,
		ABOUT_TO_JULIA,
		JULIA
	}
	
	private DisplayMode displaymode = DisplayMode.MANDELBROT;
	
	private final String TAG = "MMaps";

	private AbstractFractalView fractalView;
	private AbstractFractalView littleFractalView;
	private MandelbrotJuliaLocation mjLocation;
	
	View borderView;
	   
	private float dragLastX;
	private float dragLastY;
	   
	private ScaleGestureDetector gestureDetector;
	   
	private int dragID = -1;
	
	RenderStyle style = RenderStyle.DUAL_THREAD;
	
	private int SHARE_IMAGE_REQUEST = 0;
   
	private File imagefile;

	private boolean includeLittle = true;
	FractalViewSize size;
	
	RelativeLayout relativeLayout;
	
	private boolean showingLittle = false;
	boolean littleFractalSelected = false;
	
	double[] littleMandelbrotLocation;

	Boolean cancelledSave;
	
	private ProgressBar progressBar;
	boolean showingSpinner = false;
	boolean allowSpinner = false;
	
	Boolean renderComplete = false;
	ProgressDialog savingDialog;
	
	
	
/*-----------------------------------------------------------------------------------*/
/*Android lifecycle handling*/
/*-----------------------------------------------------------------------------------*/
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);      
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);      
      
      Bundle bundle = getIntent().getExtras();
      
      double juliaX = 0;
      double juliaY = 0;
      
      relativeLayout = new RelativeLayout(this);
      
      //Extract features from bundle, if there is one
      try {     
	      fractalType = FractalType.valueOf(bundle.getString("FractalType"));
	      style = RenderStyle.valueOf(bundle.getString("RenderStyle"));
	      includeLittle = bundle.getBoolean("SideBySide");
	      littleMandelbrotLocation = bundle.getDoubleArray("LittleMandelbrotLocation");
      } 
      catch (NullPointerException npe) {}
      
      if (fractalType == FractalType.MANDELBROT) {
    	  fractalView = new MandelbrotFractalView(this, style, FractalViewSize.LARGE);
      }
      else if (fractalType == FractalType.JULIA) {
    	  fractalView = new JuliaFractalView(this, style, FractalViewSize.LARGE);
    	  juliaX = bundle.getDouble("JULIA_X");
          juliaY = bundle.getDouble("JULIA_Y");
      }
      
      LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
      relativeLayout.addView(fractalView, lp);
      setContentView(relativeLayout);
      
      mjLocation = new MandelbrotJuliaLocation();
      fractalView.loadLocation(mjLocation);
      if(fractalType == FractalType.JULIA)
    	  ((JuliaFractalView)fractalView).setJuliaParameter(juliaX, juliaY);
      
      gestureDetector = new ScaleGestureDetector(this, this);
   }
   
   
   public void addLittleView() {   
	   //Check to see if view has already or should never be included
	   if (showingLittle || !includeLittle) return;
	   
	   //Show a little Julia next to a Mandelbrot and vice versa
	   if(fractalType == FractalType.MANDELBROT) {
		   littleFractalView = new JuliaFractalView(this, style, FractalViewSize.LITTLE);
	   }
	   else {
		   littleFractalView = new MandelbrotFractalView(this, style, FractalViewSize.LITTLE);
	   }
	   
	   //Set size of border, little view proportional to screen size
	   int width = fractalView.getWidth();
	   int height = fractalView.getHeight();
	   int borderwidth = Math.max(1, (int)(width/300.0));
	   
	   double ratio = (double)width/(double)height;
	   width /= 7;
	   height = (int)(width/ratio);   
	   
	   //Add border view (behind little view, slightly larger)
	   borderView = new View(this);
	   borderView.setBackgroundColor(Color.GRAY);
	   LayoutParams borderLayout = new LayoutParams(width + 2*borderwidth, height + 2*borderwidth);
	   relativeLayout.addView(borderView, borderLayout);

	   //Add little fractal view
	   LayoutParams lp2 = new LayoutParams(width, height);
	   lp2.setMargins(borderwidth, borderwidth, borderwidth, borderwidth);
	   relativeLayout.addView(littleFractalView, lp2);
	   
	   if(fractalType == FractalType.MANDELBROT) {
		   littleFractalView.loadLocation(mjLocation); 
		   double[] jParams = ((MandelbrotFractalView)fractalView).getJuliaParams(fractalView.getWidth()/2, fractalView.getHeight()/2);
		   ((JuliaFractalView)littleFractalView).setJuliaParameter(jParams[0], jParams[1]);
	   }
	   else {
		   mjLocation.setMandelbrotGraphArea(littleMandelbrotLocation);
		   littleFractalView.loadLocation(mjLocation);
	   }
	   
	   
      
	   setContentView(relativeLayout);
	   
	   showingLittle = true;
   }
   
   public void removeLittleView() {
	   if(!showingLittle) return;
	   
	   relativeLayout.removeView(borderView);
	   relativeLayout.removeView(littleFractalView);
	   
	   showingLittle = false;
   }
   
   public void showProgressSpinner() {
	    if(showingSpinner || !allowSpinner) return;
	    
		LayoutParams progressBarParams = new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		progressBarParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		progressBar = new ProgressBar(getApplicationContext());
		relativeLayout.addView(progressBar, progressBarParams);
		showingSpinner = true;
   }
   
   public void hideProgressSpinner() {
	   if(!showingSpinner || !allowSpinner) return;
	   Log.d(TAG, "Remove spinner");
	   
	   runOnUiThread(new Runnable() {
		
		public void run() {
			relativeLayout.removeView(progressBar);
			
		}
	});
	   showingSpinner = false;
   }
   
   @Override
   protected void onResume() {
      super.onResume();
      displaymode = DisplayMode.MANDELBROT;      
      Log.d(TAG, "onResume");
   }
   
   @Override
   protected void onDestroy(){
	   super.onDestroy();
	   fractalView.stopAllRendering();
	   fractalView.interruptThreads();
	   littleFractalView.stopAllRendering();
	   littleFractalView.interruptThreads();
	   Log.d(TAG, "Running onDestroy().");
   }
   
   @Override
   protected void onPause() {
	   super.onPause();
	   
	   if(savingDialog != null) 
		   savingDialog.dismiss();
   }
   
   
   
/*-----------------------------------------------------------------------------------*/
/*Menu creation/handling*/
/*-----------------------------------------------------------------------------------*/
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.navigationmenu, menu);
      
      return true;
   }
   
   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
	   if (fractalType == FractalType.JULIA) {
	    	  MenuItem showLittle = menu.findItem(R.id.toggleLittle);
	    	  showLittle.setTitle("Show Mandelbrot");
	      }
	   
	   return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.settobookmark:
    	  fractalView.setToBookmark();
    	  return true;
      case R.id.toggleLittle:
    	  if(includeLittle) {
	    	  if(showingLittle) {
	    		  removeLittleView();
	    	  }
	    	  else {
	    		  addLittleView();
	    	  }
    	  }
    	  else
    		  displaymode = DisplayMode.ABOUT_TO_JULIA;
    	  return true;
      case R.id.resetFractal:
    	  fractalView.reset();
    	  return true;
      case R.id.toggleCrude:
    	  fractalView.crudeRendering = !fractalView.crudeRendering;
    	  return true;
      case R.id.saveImage:
    	  saveImage();
    	  return true;
      case R.id.shareImage:
    	  shareImage();
    	  return true;
      }
      return false;
   }


/*-----------------------------------------------------------------------------------*/
/*Image saving/sharing*/
/*-----------------------------------------------------------------------------------*/
   //Wait for render to finish, then save the fractal image
   private void saveImage() {
	cancelledSave = false;
	
	if(fractalView.isRendering()) {
		savingDialog = new ProgressDialog(this);
		savingDialog.setMessage("Waiting for render to finish...");
		savingDialog.setCancelable(true);
		savingDialog.setIndeterminate(true);
		savingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				FractalActivity.this.cancelledSave = true;
			}
		});
		savingDialog.show();

		//Launch a thread to wait for completion
		new Thread(new Runnable() {  
			public void run() {  
				if(fractalView.isRendering()) {
					while (!cancelledSave && fractalView.isRendering()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {}
					}
					
					if(!cancelledSave) {
						savingDialog.dismiss();
						imagefile = fractalView.saveImage();
						final String toastText = "Saved fractal as " + imagefile.getAbsolutePath();
						showToastOnUIThread(toastText, Toast.LENGTH_LONG);
					}
				}		
				return;  
			}
		}).start(); 
	} 
	else {
		imagefile = fractalView.saveImage();
		final String toastText = "Saved fractal as " + imagefile.getAbsolutePath();
		showToastOnUIThread(toastText, Toast.LENGTH_LONG);
	}
   }
  
   //Wait for the render to finish, then share the fractal image
   private void shareImage() {
	   cancelledSave = false;
	   
	   if(fractalView.isRendering()) {
			savingDialog = new ProgressDialog(this);
			savingDialog.setMessage("Waiting for render to finish...");
			savingDialog.setCancelable(true);
			savingDialog.setIndeterminate(true);
			savingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					FractalActivity.this.cancelledSave = true;
				}
			});
			savingDialog.show();
	
			//Launch a thread to wait for completion
			new Thread(new Runnable() {  
				public void run() {  
					if(fractalView.isRendering()) {
						while (!cancelledSave && fractalView.isRendering()) {
							try {
								Thread.sleep(100);
								Log.d(TAG, "Waiting to save...");
							} catch (InterruptedException e) {}
						}
						
						if(!cancelledSave) {
							savingDialog.dismiss();
							imagefile = fractalView.saveImage();
							Intent imageIntent = new Intent(Intent.ACTION_SEND);
							imageIntent.setType("image/jpg");
							imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));
							
							startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
						}
					}		
					return;  
				}
			}).start(); 
		} 
		else {
			imagefile = fractalView.saveImage();
			Intent imageIntent = new Intent(Intent.ACTION_SEND);
			imageIntent.setType("image/jpg");
			imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));
			
			startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
		}
   }
   
   //Get result of launched activity (only time used is after sharing, so delete temp. image)
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data)
   {
	   if (requestCode == 0) {
			   Log.d(TAG, "Deleting temporary jpg " + imagefile.getAbsolutePath());
			   imagefile.delete();
	   }
   }

   

/*-----------------------------------------------------------------------------------*/
/*Touch controls*/
/*-----------------------------------------------------------------------------------*/
   public boolean onTouch(View v, MotionEvent evt) {
		gestureDetector.onTouchEvent(evt);
		
		switch (evt.getAction() & MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_DOWN:
				if (showingLittle && evt.getX() <= borderView.getWidth() && evt.getY() <= borderView.getHeight()) {
					borderView.setBackgroundColor(Color.DKGRAY);
					littleFractalSelected = true;
				}
				else if (displaymode == DisplayMode.ABOUT_TO_JULIA) {
					launchJulia(evt.getX(), evt.getY());
				} 
				else if (showingLittle && fractalType == FractalType.MANDELBROT)	{
					double[] juliaParams = ((MandelbrotFractalView)fractalView).getJuliaParams(evt.getX(), evt.getY());
					((JuliaFractalView)littleFractalView).setJuliaParameter(juliaParams[0], juliaParams[1]);
				}
				else {
					startDragging(evt);	
				}
				
				break;
				
				
			case MotionEvent.ACTION_MOVE:						
				if(currentlyDragging) {
					dragFractal(evt);
				}
				else if (showingLittle && !littleFractalSelected && fractalType == FractalType.MANDELBROT)	{
					double[] juliaParams = ((MandelbrotFractalView)fractalView).getJuliaParams(evt.getX(), evt.getY());
					((JuliaFractalView)littleFractalView).setJuliaParameter(juliaParams[0], juliaParams[1]);
				}
				
				break;
				
				
			case MotionEvent.ACTION_POINTER_UP:
				if(evt.getPointerCount() == 1)
					break;
				else {
					try {
						chooseNewActivePointer(evt);
					} 
					catch (IllegalArgumentException iae) {} 
					
				}
				
				break;
		       
				
			case MotionEvent.ACTION_UP:
				if(currentlyDragging)
					stopDragging();
				else if (littleFractalSelected) {
					borderView.setBackgroundColor(Color.GRAY);
					littleFractalSelected = false;
					if (evt.getX() <= borderView.getWidth() && evt.getY() <= borderView.getHeight())
						if (fractalType == FractalType.MANDELBROT)
							launchJulia(((JuliaFractalView)littleFractalView).getJuliaParam());
						else if (fractalType == FractalType.JULIA)
							finish();
				}					
				
				break;
		}
		return true;
	}


	private void dragFractal(MotionEvent evt) {
		try {
		   	int pointerIndex = evt.findPointerIndex(dragID);
			
			float dragDiffPixelsX = evt.getX(pointerIndex) - dragLastX;
			float dragDiffPixelsY = evt.getY(pointerIndex) - dragLastY;
			
			// Move the canvas
			if (dragDiffPixelsX != 0.0f && dragDiffPixelsY != 0.0f)
				fractalView.dragFractal(dragDiffPixelsX, dragDiffPixelsY);
			
			// Update last mouse position
			dragLastX = evt.getX(pointerIndex);
			dragLastY = evt.getY(pointerIndex);
		}
		catch (Exception iae) {}
	}
	
	
	private void startDragging(MotionEvent evt) {
		   dragLastX = (int) evt.getX();
		   dragLastY = (int) evt.getY();
		   dragID = evt.getPointerId(0);	
		
		   fractalView.startDragging();
		   currentlyDragging = true;
	   }
	
	
	private void chooseNewActivePointer(MotionEvent evt) {
		// Extract the index of the pointer that came up
		final int pointerIndex = (evt.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = evt.getPointerId(pointerIndex);
		
		//evt.getX/Y() can apparently throw these exceptions, in some versions of Android (2.2, at least)
		//(https://android-review.googlesource.com/#/c/21318/)
		try {		
			dragLastX = (int) evt.getX(dragID);
			dragLastY = (int) evt.getY(dragID);
			   
			if (pointerId == dragID) {
				Log.d(TAG, "Choosing new active pointer");
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				dragLastX = (int) evt.getX(newPointerIndex);
				dragLastY = (int) evt.getY(newPointerIndex);
				dragID = evt.getPointerId(newPointerIndex);
			}
		} 
		catch (ArrayIndexOutOfBoundsException aie) {}
		
	}
	
	
	private void stopDragging() {
		   	currentlyDragging = false;
			fractalView.stopDragging(false);	
	}
	
	
	private void launchJulia(double[] juliaParams)
   {
	   	Intent intent = new Intent(this, FractalActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("FractalType", FractalType.JULIA.toString());
		bundle.putBoolean("SideBySide", includeLittle);
		
		Log.d(TAG, "Mandelbrot Graph Area at launch: " + (mjLocation.getMandelbrotGraphArea())[0]);
		bundle.putDoubleArray("LittleMandelbrotLocation", fractalView.graphArea);
		
		bundle.putDouble("JULIA_X", juliaParams[0]);
		bundle.putDouble("JULIA_Y", juliaParams[1]);
		bundle.putString("RenderStyle", style.toString());
		
		intent.putExtras(bundle);
		startActivity(intent);
   }
   
   private void launchJulia(float x, float y) {
		double[] juliaParams = ((MandelbrotFractalView)fractalView).getJuliaParams(x, y);
		launchJulia(juliaParams);
}


   public boolean onScaleBegin(ScaleGestureDetector detector) {
	   fractalView.stopDragging(true);
	   fractalView.startZooming(detector.getFocusX(), detector.getFocusY());
	   	 
	   currentlyDragging = false;
	   return true;
	}

   public boolean onScale(ScaleGestureDetector detector) {
		fractalView.zoomImage(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
		return true;
	}
	
   public void onScaleEnd(ScaleGestureDetector detector) {
	   fractalView.stopZooming();
	   currentlyDragging = true;
	   fractalView.startDragging();
	}
   
/*-----------------------------------------------------------------------------------*/
/*Utilities*/
/*-----------------------------------------------------------------------------------*/
   //A single method for running toasts on the UI thread, rather than 
   //creating new Runnables each time.
   public void showToastOnUIThread(final String toastText, final int length) {
	    runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), toastText, length).show();
			}
		});
   }
}
