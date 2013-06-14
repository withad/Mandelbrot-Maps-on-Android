package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

public class CubicMandelbrotFractalView extends AbstractFractalView{
	
	
	
	public CubicMandelbrotFractalView(Context context, FractalViewSize size) {
		super(context, size);
		
		setColouringScheme(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("MANDELBROT_COLOURS", "MandelbrotDefault")
							, false);
		
		for(int i = 0; i < noOfThreads; i++) {
			renderThreadList.get(i).setName("Mandelbrot thread " + i);
		}
		
		// Set the "maximum iteration" calculation constants
		// Empirically determined values for Mandelbrot set.
		ITERATION_BASE = 1.24;
		ITERATION_CONSTANT_FACTOR = 54;
		
		// Set home area
		homeGraphArea = new MandelbrotJuliaLocation().getMandelbrotGraphArea();
		
		// How deep a zoom do we allow?
		MAXZOOM_LN_PIXEL = -31; // Beyond -31, "double"s break down(!).
	}
	
	
	// Load a location
	void loadLocation(MandelbrotJuliaLocation _mjLocation) {
		if(pixelSizes != null)
			clearPixelSizes();
		setGraphArea(_mjLocation.getMandelbrotGraphArea(), true);
	}
		
	
	int pixelInSet (int xPixel, int yPixel, int maxIterations) {
		boolean inside = true;
		int iterationNr;
		double newx, newy;
		double x, y;
		
		// Set x0 (real part of c)
		double x0 = xMin + ( (double)xPixel * pixelSize);
		double y0 = yMax - ( (double)yPixel * pixelSize ); //TODO This shouldn't be calculated every time
	
		// Start at x0, y0
		x = x0;
		y = y0;
		
		//Run iterations over this point
		for (iterationNr=0; iterationNr<maxIterations; iterationNr++) {
			// z^2 + c
			/*newx = (x*x) - (y*y) + x0;
			newy = (2 * x * y) + y0;*/
			newx = (x*x*x) - (y*y*x) -(2*x*y*y) + x0;
			newy = (2 * x*x * y + y*x*x -y*y*y) + y0;
		
			x = newx;
			y = newy;
		
			// Well known result: if distance is >2, escapes to infinity...
			if ( (x*x + y*y) > 4) {
				inside = false;
				break;
			}
		}
		
		if(inside)
			return colourer.colourInsidePoint();
		else
			return colourer.colourOutsidePoint(iterationNr, maxIterations);
	}
	
}
