package uk.ac.ed.inf.mandelbrotmaps;

import java.io.File;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.FractalViewSize;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class FractalActivity extends Activity implements OnTouchListener, OnScaleGestureListener, 
														OnSharedPreferenceChangeListener, OnLongClickListener{
	private final String TAG = "MMaps";
	
	// Constants
	private final int SHARE_IMAGE_REQUEST = 0;
	private final int RETURN_FROM_JULIA = 1;
	private final int RETURN_FROM_DETAIL_CHANGE = 2;
	
	// Shared pref keys
	public static final String mandelbrotDetailKey = "MANDELBROT_DETAIL";
	public static final String juliaDetailKey = "JULIA_DETAIL";
	public static final String DETAIL_CHANGED_KEY = "DETAIL_CHANGED";
	private final String PREVIOUS_MAIN_GRAPH_AREA = "prevMainGraphArea";
	private final String PREVIOUS_LITTLE_GRAPH_AREA = "prevLittleGraphArea";
	private final String PREVIOUS_JULIA_PARAMS = "prevJuliaParams";
	private final String PREVIOUS_SHOWING_LITTLE = "prevShowingLittle";
	private final String FIRST_TIME_KEY = "FirstTime";

	
	// Type of fractal displayed in the main fractal view
	public static enum FractalType {
		MANDELBROT,
		JULIA
	}
	public FractalType fractalType = FractalType.MANDELBROT;

	// Layout variables
	public AbstractFractalView fractalView;
	private AbstractFractalView littleFractalView;
	private View borderView;
	private RelativeLayout relativeLayout;
	
	// Fractal locations
	private MandelbrotJuliaLocation mjLocation;
	private double[] littleMandelbrotLocation;
	   
	// Dragging/scaling control variables
	private float dragLastX;
	private float dragLastY;
	private int dragID = -1;
	private boolean currentlyDragging = false;
	
	private ScaleGestureDetector gestureDetector;
	
	// File saving variables
	private ProgressDialog savingDialog;
	private File imagefile;
	private Boolean cancelledSave = false;	
	
	// Little fractal view tracking
	public boolean showLittleAtStart = false;
	public boolean showingLittle = false;
	private boolean littleFractalSelected = false;	
	
	// Loading spinner (currently all disabled due to slowdown)
	private ProgressBar progressBar;
	private boolean showingSpinner = false;
	private boolean allowSpinner = false;
	
	
	
	
/*-----------------------------------------------------------------------------------*/
/*Android lifecycle handling*/
/*-----------------------------------------------------------------------------------*/
	/* Sets up the activity, mostly creates the main fractal view.
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);  
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);      
		
		// If first time launch, show the tutorial/intro
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(prefs.getBoolean(FIRST_TIME_KEY, true)) showIntro();
	  
	  	Bundle bundle = getIntent().getExtras();
	  
	  	mjLocation = new MandelbrotJuliaLocation();
	  	double[] juliaParams = mjLocation.defaultJuliaParams;
	  	double[] juliaGraphArea = mjLocation.defaultJuliaGraphArea;
  
	  	relativeLayout = new RelativeLayout(this);
      
	  	//Extract features from bundle, if there is one
		try {     
			fractalType = FractalType.valueOf(bundle.getString("FractalType"));
			littleMandelbrotLocation = bundle.getDoubleArray("LittleMandelbrotLocation");
			showLittleAtStart = bundle.getBoolean("ShowLittleAtStart");
		}
		catch (NullPointerException npe) {}
		
		if (fractalType == FractalType.MANDELBROT) {
			fractalView = new MandelbrotFractalView(this, FractalViewSize.LARGE);
		}
		else if (fractalType == FractalType.JULIA) {
			fractalView = new JuliaFractalView(this, FractalViewSize.LARGE);
			juliaParams = bundle.getDoubleArray("JuliaParams");
			juliaGraphArea = bundle.getDoubleArray("JuliaGraphArea");
		}
		else {
			fractalView = new CubicMandelbrotFractalView(this, FractalViewSize.LARGE);
		}
		
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		relativeLayout.addView(fractalView, lp);
		setContentView(relativeLayout);
		
		mjLocation = new MandelbrotJuliaLocation(juliaGraphArea, juliaParams);
		fractalView.loadLocation(mjLocation);
		
		gestureDetector = new ScaleGestureDetector(this, this);
	}

/* When destroyed, stop rendering and kill all the threads,
	* so references aren't kept.
    */
   @Override
   protected void onDestroy() {
	   super.onDestroy();
	   fractalView.stopAllRendering();
	   fractalView.interruptThreads();
	   if (littleFractalView != null) {
		   littleFractalView.stopAllRendering();
		   littleFractalView.interruptThreads();
	   }
	   
	   
   }
   
   /* When paused, do the following, dismiss the saving dialog. Might be buggy if mid-save?
    * (non-Javadoc)
    * @see android.app.Activity#onPause()
    */
   @Override
   protected void onPause() {
	   super.onPause();
	   if(savingDialog != null) 
		   savingDialog.dismiss();
   }
   
   
   @Override
   protected void onResume() {
	   super.onResume();
	   
	   SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	   prefs.registerOnSharedPreferenceChangeListener(this);
   }
   
   
   @Override
   protected void onSaveInstanceState(Bundle outState) {
	   super.onSaveInstanceState(outState);
	   
	   outState.putDoubleArray(PREVIOUS_MAIN_GRAPH_AREA, fractalView.graphArea);
	   
	   if(showingLittle) {
		   outState.putDoubleArray(PREVIOUS_LITTLE_GRAPH_AREA, littleFractalView.graphArea);
	   }
	   
	   if(fractalType == FractalType.MANDELBROT) {
		   outState.putDoubleArray(PREVIOUS_JULIA_PARAMS, ((MandelbrotFractalView)fractalView).currentJuliaParams);
	   }
	   else {
		   outState.putDoubleArray(PREVIOUS_JULIA_PARAMS, ((JuliaFractalView)fractalView).getJuliaParam());
	   }
	   
	   outState.putBoolean(PREVIOUS_SHOWING_LITTLE, showingLittle);
   }
   
   
   @Override
   protected void onRestoreInstanceState(Bundle savedInstanceState) {
	   super.onRestoreInstanceState(savedInstanceState);
	   
	   double[] mainGraphArea = savedInstanceState.getDoubleArray(PREVIOUS_MAIN_GRAPH_AREA);
	   double[] littleGraphArea = savedInstanceState.getDoubleArray(PREVIOUS_LITTLE_GRAPH_AREA);
	   double[] juliaParams = savedInstanceState.getDoubleArray(PREVIOUS_JULIA_PARAMS);
	   
	   MandelbrotJuliaLocation restoredLoc;
	   
	   if(fractalType == FractalType.MANDELBROT) {
		   restoredLoc = new MandelbrotJuliaLocation(mainGraphArea, littleGraphArea, juliaParams);
		   ((MandelbrotFractalView)fractalView).currentJuliaParams = juliaParams;
	   }
	   else {
		   restoredLoc = new MandelbrotJuliaLocation(littleGraphArea, mainGraphArea, juliaParams);
	   }
	   
	   restoredLoc.setMandelbrotGraphArea(mainGraphArea);
	   fractalView.loadLocation(restoredLoc);
	   
	   showLittleAtStart = savedInstanceState.getBoolean(PREVIOUS_SHOWING_LITTLE);
   }
   
   
   /* Set the activity result when finishing, if needed
    * (non-Javadoc)
    * @see android.app.Activity#finish()
    */
   @Override
   public void finish() {
	   if(fractalType == FractalType.JULIA) {
		   double[] juliaParams = ((JuliaFractalView)fractalView).getJuliaParam();
		   double[] currentGraphArea = fractalView.graphArea;
		   
		   Intent result = new Intent();
		   result.putExtra("JuliaParams", juliaParams);
		   result.putExtra("JuliaGraphArea", currentGraphArea);
		   
		   setResult(Activity.RESULT_OK, result);
	   }
	   
	   super.finish();
   }
   
   
   //Get result of launched activity (only time used is after sharing, so delete temp. image)
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	   
	   switch (requestCode) {
	   case SHARE_IMAGE_REQUEST:
		   // Delete the temporary image
		   imagefile.delete();
		   break;
		   
	   case RETURN_FROM_JULIA:
		   if(showingLittle) {
			   double[] juliaGraphArea = data.getDoubleArrayExtra("JuliaGraphArea");
			   double[] juliaParams = data.getDoubleArrayExtra("JuliaParams");
			   littleFractalView.loadLocation(new MandelbrotJuliaLocation(juliaGraphArea, juliaParams));
		   }
		   break;
		   
	   case RETURN_FROM_DETAIL_CHANGE:
		   boolean changed = data.getBooleanExtra(DETAIL_CHANGED_KEY, false);
		   if(changed) {
			   fractalView.reloadCurrentLocation();
			   if(showingLittle)
				   littleFractalView.reloadCurrentLocation();
		   }
		   break;
	   }
  	}
   

   
/*-----------------------------------------------------------------------------------*/
/*Dynamic UI creation*/
/*-----------------------------------------------------------------------------------*/
   /* Adds the little fractal view and its border, if not showing
    * Also determines its height, width based on large fractal view's size
    */
   public void addLittleView(boolean centre) {   
	   //Check to see if view has already or should never be included
	   if (showingLittle) {
		   relativeLayout.bringChildToFront(littleFractalView);
		   return;
	   }
		   
	   
	   //Show a little Julia next to a Mandelbrot and vice versa
	   if(fractalType == FractalType.MANDELBROT) {
		   littleFractalView = new JuliaFractalView(this, FractalViewSize.LITTLE);
	   }
	   else {
		   littleFractalView = new MandelbrotFractalView(this, FractalViewSize.LITTLE);
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
		   
		   double[] jParams;		   
		   if(!centre) {
			   jParams = ((MandelbrotFractalView)fractalView).currentJuliaParams;
		   }
		   else {
			   jParams = ((MandelbrotFractalView)fractalView).getJuliaParams(fractalView.getWidth()/2, fractalView.getHeight()/2);
		   }
		   
		   ((JuliaFractalView)littleFractalView).setJuliaParameter(jParams[0], jParams[1]);
	   }
	   else {
		   mjLocation.setMandelbrotGraphArea(littleMandelbrotLocation);
		   littleFractalView.loadLocation(mjLocation);
	   }
      
	   setContentView(relativeLayout);
	   
	   showingLittle = true;
   }
   
   /* Hides the little fractal view, if showing */
   public void removeLittleView() {
	   if(!showingLittle) return;
	   
	   relativeLayout.removeView(borderView);
	   relativeLayout.removeView(littleFractalView);
	   
	   littleFractalView.interruptThreads();
	   
	   showingLittle = false;
   }
   
   
   /* Shows the progress spinner. Never used because it causes slowdown,
    * leaving it in so I can demonstrate it with benchmarks.
    * Might adapt it to do a progress bar that updates less often. 
    */
   public void showProgressSpinner() {
	    if(showingSpinner || !allowSpinner) return;
	    
		LayoutParams progressBarParams = new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		progressBarParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		progressBar = new ProgressBar(getApplicationContext());
		relativeLayout.addView(progressBar, progressBarParams);
		showingSpinner = true;
   }
   
   /* As above, except for hiding.
    */
   public void hideProgressSpinner() {
	   if(!showingSpinner || !allowSpinner) return;
	   
	   runOnUiThread(new Runnable() {
		
		public void run() {
			relativeLayout.removeView(progressBar);
		}
	});
	   showingSpinner = false;
   }
   
   
   
/*-----------------------------------------------------------------------------------*/
/*Menu creation/handling*/
/*-----------------------------------------------------------------------------------*/
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.mainmenu, menu);
      
      return true;
   }
   
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		String verb;
		String fractal;
		
		if(!showingLittle)
			verb = "Add";
		else 
			verb = "Remove";
		
		if (fractalType == FractalType.MANDELBROT)
			fractal = "Julia";
		else
			fractal = "Mandelbrot";
		
		MenuItem showLittle = menu.findItem(R.id.toggleLittle);
		showLittle.setTitle(verb+" "+fractal);
		
		return true;
	}

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.testlocation:
    	  fractalView.setToTestLocation();
    	  return true;
    	  
      case R.id.toggleLittle:
    	  if(showingLittle) {
    		  removeLittleView();
    	  }
    	  else {
    		  addLittleView(true);
    	  }
    	  return true;
    	  
      case R.id.resetFractal:
    	  fractalView.reset();
    	  return true;
    	  
      case R.id.saveImage:
    	  saveImage();
    	  return true;
    	  
      case R.id.shareImage:
    	  shareImage();
    	  return true;
    	  
      case R.id.preferences:
    	  startActivity(new Intent(this, Prefs.class));
    	  return true;
    	  
      case R.id.details:
    	  startActivityForResult(new Intent(this, DetailControl.class), RETURN_FROM_DETAIL_CHANGE);
    	  return true;
    	  
      case R.id.help:
    	  showHelpDialog();
    	  return true;
    	  
      case R.id.printbookmark:
    	  setBookmark();
    	  return true;
    	  
      case R.id.loadbookmark:
    	  loadBookmark();
    	  return true;
      }
      return false;
   }

   

/*-----------------------------------------------------------------------------------*/
/*Image saving/sharing*/
/*-----------------------------------------------------------------------------------*/
   /* TODO: Tidy up this code. Possibly switch to using Handlers and postDelayed.
   */
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
						String toastText;
						if(imagefile == null) toastText = "Unable to save fractal - filename already in use.";
						else toastText = "Saved fractal as " + imagefile.getAbsolutePath();
						showToastOnUIThread(toastText, Toast.LENGTH_LONG);
					}
				}		
				return;  
			}
		}).start(); 
	} 
	else {
		imagefile = fractalView.saveImage();
		String toastText;
		if(imagefile == null) toastText = "Unable to save fractal - filename already in use.";
		else toastText = "Saved fractal as " + imagefile.getAbsolutePath();
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
							} catch (InterruptedException e) {}
						}			
			
						if(!cancelledSave) {
							savingDialog.dismiss();
							imagefile = fractalView.saveImage();
							if(imagefile != null) {
								Intent imageIntent = new Intent(Intent.ACTION_SEND);
								imageIntent.setType("image/jpg");
								imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));
						
							startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
							}
							else {
								showToastOnUIThread("Unable to share image - couldn't save temporary file", Toast.LENGTH_LONG);
							}
						}
					}	
					return;  
				}
			}).start(); 
		} 
		else {
			imagefile = fractalView.saveImage();
			
			if(imagefile != null) {
				Intent imageIntent = new Intent(Intent.ACTION_SEND);
				imageIntent.setType("image/png");
				imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));
				
				startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
			}
			else {
				showToastOnUIThread("Unable to share image - couldn't save temporary file", Toast.LENGTH_LONG);
			}
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
				else if (showingLittle && fractalType == FractalType.MANDELBROT && !gestureDetector.isInProgress() 
						&& !fractalView.holdingPin && (touchingPin(evt.getX(), evt.getY())))	{
					// Take hold of the pin, reset the little fractal view.
					fractalView.holdingPin = true;
					updateLittleJulia(evt.getX(), evt.getY());
				}
				else {
					startDragging(evt);
				}
				
				break;
				
				
			case MotionEvent.ACTION_MOVE:
				if(!gestureDetector.isInProgress()) {
					if(currentlyDragging) {
						dragFractal(evt);
					}
					else if (showingLittle && !littleFractalSelected && fractalType == FractalType.MANDELBROT && fractalView.holdingPin)	{
						updateLittleJulia(evt.getX(), evt.getY());
					}
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
				if(currentlyDragging) {
					stopDragging();
				}
				else if (littleFractalSelected) {
					borderView.setBackgroundColor(Color.GRAY);
					littleFractalSelected = false;
					if (evt.getX() <= borderView.getWidth() && evt.getY() <= borderView.getHeight()) {
						if (fractalType == FractalType.MANDELBROT) {
							launchJulia(((JuliaFractalView)littleFractalView).getJuliaParam());
						}
						else if (fractalType == FractalType.JULIA) {
							finish();
						}
					}
				}
				// If holding the pin, drop it, update screen (render won't display while dragging, might've finished in background)
				else if(fractalView.holdingPin) {
					fractalView.holdingPin = false;
					updateLittleJulia(evt.getX(), evt.getY());				
				}
				
				fractalView.holdingPin = false;
				
				break;
		}
		return false;
	}

	
	private boolean touchingPin(float x, float y) {
		if (fractalType == FractalType.JULIA)
			return false;
		
		boolean touchingPin = false;
		float[] pinCoords = ((MandelbrotFractalView)fractalView).getPinCoords();
		float pinX = pinCoords[0];
		float pinY = pinCoords[1];
		
		float radius = ((MandelbrotFractalView)fractalView).largePinRadius;
			
		if(x <= pinX + radius && x >= pinX - radius && y <= pinY + radius && y >= pinY - radius)
			touchingPin = true;
		
		return touchingPin;
}

	private void startDragging(MotionEvent evt) {
		   dragLastX = (int) evt.getX();
		   dragLastY = (int) evt.getY();
		   dragID = evt.getPointerId(0);	
		
		   fractalView.startDragging();
		   currentlyDragging = true;
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
	
	private void stopDragging() {
		   	currentlyDragging = false;
			fractalView.stopDragging(false);	
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

	
	/* Detect a long click, place the Julia pin */
	public boolean onLongClick(View v) {
		// Check that it's not scaling, dragging (check for dragging is a little hacky, but seems to work), or already holding the pin
		if(!gestureDetector.isInProgress() && fractalView.totalDragX < 1 && fractalView.totalDragY < 1 && !fractalView.holdingPin) {
			updateLittleJulia((float)dragLastX, (float)dragLastY);
			if(currentlyDragging) {
				stopDragging();
			}
			return true;
		}
		
		return false;
	}
	
	
	
/*-----------------------------------------------------------------------------------*/
/*Utilities*/
/*-----------------------------------------------------------------------------------*/
	/*A single method for running toasts on the UI thread, rather than 
   	creating new Runnables each time. */
	public void showToastOnUIThread(final String toastText, final int length) {
	    runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), toastText, length).show();
			}
		});
	}
	
	/* Choose a new active pointer from the available ones 
	 * Used during/at the end of scaling to pick the new dragging pointer*/
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
				//Log.d(TAG, "Choosing new active pointer");
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				dragLastX = (int) evt.getX(newPointerIndex);
				dragLastY = (int) evt.getY(newPointerIndex);
				dragID = evt.getPointerId(newPointerIndex);
			}
		} 
		catch (ArrayIndexOutOfBoundsException aie) {}
	}
	
	
	/* Launches a new Julia fractal activity with the given parameters */
	private void launchJulia(double[] juliaParams) {
	   	Intent intent = new Intent(this, FractalActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("FractalType", FractalType.JULIA.toString());
		bundle.putBoolean("ShowLittleAtStart", true);
		bundle.putDoubleArray("LittleMandelbrotLocation", fractalView.graphArea);
		
		bundle.putDouble("JULIA_X", juliaParams[0]);
		bundle.putDouble("JULIA_Y", juliaParams[1]);
		bundle.putDoubleArray("JuliaParams", juliaParams);
		bundle.putDoubleArray("JuliaGraphArea", littleFractalView.graphArea);
		
		intent.putExtras(bundle);
		startActivityForResult(intent, RETURN_FROM_JULIA);
	}
	
	
	private void updateLittleJulia(float x, float y) {
		if(fractalType != FractalType.MANDELBROT)
			return;
		
		fractalView.invalidate();
		
		if(showingLittle) {
			double[] juliaParams = ((MandelbrotFractalView)fractalView).getJuliaParams(x, y);
			((JuliaFractalView)littleFractalView).setJuliaParameter(juliaParams[0], juliaParams[1]);
		}
		else {
			((MandelbrotFractalView)fractalView).getJuliaParams(x, y);
			addLittleView(false);
		}
		
		//fractalView.holdingPin = true;
	}

	
	public void onSharedPreferenceChanged(SharedPreferences prefs, String changedPref) {
		if(changedPref.equals("MANDELBROT_COLOURS")) {
			String mandelbrotScheme = prefs.getString(changedPref, "MandelbrotDefault");
			
			if(fractalType == FractalType.MANDELBROT) {
				fractalView.setColouringScheme(mandelbrotScheme, true);
			}
			else if (showingLittle) {
				littleFractalView.setColouringScheme(mandelbrotScheme, true);
			}
		}
		
		else if(changedPref.equals("JULIA_COLOURS")) {
			String juliaScheme = prefs.getString(changedPref, "JuliaDefault");
			
			if(fractalType == FractalType.JULIA) {
				fractalView.setColouringScheme(juliaScheme, true);
			}
			else if (showingLittle) {
				littleFractalView.setColouringScheme(juliaScheme, true);
			}
		}
		
		else if(changedPref.equals("PIN_COLOUR")) {
			int newColour = Color.parseColor(prefs.getString(changedPref, "blue"));
			
			if(fractalType == FractalType.MANDELBROT) {
				((MandelbrotFractalView)fractalView).setPinColour(newColour);
			}
			else if (showingLittle) {
				((MandelbrotFractalView)littleFractalView).setPinColour(newColour);
			}
		}
	}

	
	public double getDetailFromPrefs(FractalViewSize fractalViewSize) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String keyToUse = mandelbrotDetailKey;
		
		if(fractalType == FractalType.MANDELBROT) {
			if(fractalViewSize == FractalViewSize.LARGE)
				keyToUse = mandelbrotDetailKey;
			else
				keyToUse = juliaDetailKey;
		}
		else {
			if(fractalViewSize == FractalViewSize.LARGE)
				keyToUse = juliaDetailKey;
			else
				keyToUse = mandelbrotDetailKey;
		}
		
		return (double)prefs.getFloat(keyToUse, (float)AbstractFractalView.DEFAULT_DETAIL_LEVEL);
	}


	/* Show the short tutorial/intro dialog */
	private void showIntro() {		
		TextView text = new TextView(this);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(Html.fromHtml(getString(R.string.intro_text)));

		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder	.setCancelable(true)
				.setView(text)
				.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });;
		builder.create().show();

		
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putBoolean(FIRST_TIME_KEY, false);
		editor.commit();
	}
	
	
	/* Show the large help dialog */
	private void showHelpDialog() {
		ScrollView scrollView = new ScrollView(this);
		TextView text = new TextView(this);
        text.setText(Html.fromHtml(getString(R.string.help_text)));
        scrollView.addView(text);

		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder	.setCancelable(true)
				.setView(scrollView)
				.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });;
		builder.create().show();
	}

	
	/* Set the bookmark location in Prefs to the current location
	 * (Proof-of-concept, currently unused)
	 */
	private void setBookmark() {
		MandelbrotJuliaLocation bookmark;
		if(fractalType == FractalType.MANDELBROT) {
			if(littleFractalView != null) {
				Log.d(TAG, "Showing little...");
				bookmark = new MandelbrotJuliaLocation(fractalView.graphArea, littleFractalView.graphArea, 
															((MandelbrotFractalView)fractalView).currentJuliaParams);
			}
			else {
				bookmark = new MandelbrotJuliaLocation(fractalView.graphArea);
			}
		}
		else {
			bookmark = new MandelbrotJuliaLocation(littleFractalView.graphArea, fractalView.graphArea, 
														((MandelbrotFractalView)littleFractalView).currentJuliaParams);
		}
		
		Log.d(TAG, bookmark.toString());
		
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putString("BOOKMARK", bookmark.toString());
		editor.commit();
	}
	
	
	/* Set the current location to the bookmark
	 * (Proof-of-concept, currently unused)
	 */
	private void loadBookmark() {
		String bookmark = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("BOOKMARK", null);
		
		if(bookmark != null) {
			Log.d(TAG,"Loaded bookmark " + bookmark);
			MandelbrotJuliaLocation newLocation = new MandelbrotJuliaLocation(bookmark);
			fractalView.loadLocation(newLocation);
		}
	}
}
