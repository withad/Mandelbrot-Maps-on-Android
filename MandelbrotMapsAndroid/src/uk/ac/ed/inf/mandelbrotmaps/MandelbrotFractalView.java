package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.FractalViewSize;
import uk.ac.ed.inf.mandelbrotmaps.colouring.ColouringScheme;
import uk.ac.ed.inf.mandelbrotmaps.colouring.RGBWalkColouringScheme;
import uk.ac.ed.inf.mandelbrotmaps.colouring.SpiralRenderer;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class MandelbrotFractalView extends AbstractFractalView{

	private final String TAG = "MMaps";
	
	ColouringScheme colourer = new SpiralRenderer();
	
	public MandelbrotFractalView(Context context, RenderStyle style, FractalViewSize size) {
		super(context, style, size);
		
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
	void loadLocation(MandelbrotJuliaLocation mjLocation) {
		setScaledIterationCount(mjLocation.getMandelbrotContrast());
		setGraphArea(mjLocation.getMandelbrotGraphArea(), true);
	}	
	
	public double[] getJuliaParams(float touchX, float touchY)
	{
		double[] mandelbrotGraphArea = getGraphArea();
		double pixelSize = getPixelSize();
	
		double[] juliaParams = new double[2];
		
		// Mouse position, on the complex plane (translated from pixels)
		juliaParams[0] = mandelbrotGraphArea[0] + ( (double)touchX * pixelSize );
		juliaParams[1] = mandelbrotGraphArea[1] - ( (double)touchY * pixelSize );
		
		return juliaParams;
	}


	
	
	int equationIteration(double x, double y, double x0, double y0, int maxIterations) {
		int iterationNr;
		double newx, newy;
		
		for (iterationNr=0; iterationNr<maxIterations; iterationNr++) {
			// z^2 + c
			newx = (x*x) - (y*y) + x0;
			newy = (2 * x * y) + y0;
		
			x = newx;
			y = newy;
		
			// Well known result: if distance is >2, escapes to infinity...
			if ( (x*x + y*y) > 4) {
				break;
			}
		}
		
		return iterationNr;
	}	
}
