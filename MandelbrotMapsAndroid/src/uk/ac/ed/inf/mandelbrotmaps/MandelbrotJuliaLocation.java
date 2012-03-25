package uk.ac.ed.inf.mandelbrotmaps;

import android.util.Log;


class MandelbrotJuliaLocation {
	private double[] mandelbrotGraphArea;
	private double[] juliaGraphArea;
	private double[] juliaParams;
	
	public double[] defaultMandelbrotGraphArea = new double[] {-3.1, 1.45, 5};
	public double[] defaultJuliaGraphArea = new double[] {-2.2, 1.25, 4.3};
	public double[] defaultJuliaParams = new double[] {-0.6, -0.01875}; //Julia params place it right in the middle of the Mandelbrot home.
	
	// Constructor. Defaults - some semi-arbitrary, pretty values
	public MandelbrotJuliaLocation() {
		mandelbrotGraphArea = defaultMandelbrotGraphArea;
		juliaGraphArea = defaultJuliaGraphArea;
		juliaParams = defaultJuliaParams;
	}
	
	public MandelbrotJuliaLocation(double[] _mandelbrotGraphArea, double[] _juliaGraphArea, double[] _juliaParams) {
		mandelbrotGraphArea = _mandelbrotGraphArea;
		juliaGraphArea = _juliaGraphArea;
		juliaParams = _juliaParams;
	}
	
	
	public MandelbrotJuliaLocation(double[] _juliaGraphArea, double[] _juliaParams) {
		mandelbrotGraphArea = new double[] {-3.1, 1.45, 5};
		juliaGraphArea = _juliaGraphArea;
		juliaParams = _juliaParams;
	}
	
	public MandelbrotJuliaLocation(double[] _mandelbrotGraphArea) {
		mandelbrotGraphArea = _mandelbrotGraphArea;
		juliaGraphArea = defaultJuliaGraphArea;
		juliaParams = defaultJuliaParams;
	}
	
	
	public MandelbrotJuliaLocation(String bookmark) {		
		mandelbrotGraphArea = new double[3];
		juliaGraphArea = new double[3];
		juliaParams = new double[2];
		
		String[] points = bookmark.split(" ");
		
		mandelbrotGraphArea[0] = Double.parseDouble(points[0]);
		mandelbrotGraphArea[1] = Double.parseDouble(points[1]);
		mandelbrotGraphArea[2] = Double.parseDouble(points[2]);
		
		juliaGraphArea[0] = Double.parseDouble(points[3]);
		juliaGraphArea[1] = Double.parseDouble(points[4]);
		juliaGraphArea[2] = Double.parseDouble(points[5]);
		
		juliaParams[0] = Double.parseDouble(points[6]);
		juliaParams[1] = Double.parseDouble(points[7]);
		
		Log.d("MMaps", "Just set the mbrot graph area. It is... " + mandelbrotGraphArea[0] + "");
	}
	
	
	public void setMandelbrotGraphArea(double[] newMandelbrotGraphArea) {
		System.arraycopy(newMandelbrotGraphArea, 0, mandelbrotGraphArea, 0, mandelbrotGraphArea.length);
	}

	public double[] getMandelbrotGraphArea() {
		return mandelbrotGraphArea;
	}
	
	public double[] getJuliaGraphArea() {
		return juliaGraphArea;
	}
	
	public double[] getJuliaParam() {
		return juliaParams;
	}
	
	
	public String toString() {
		String outString = "";
		
		for (double d : mandelbrotGraphArea) {
			outString += (Double.toString(d) + " ");
		}
		
		for (double d : juliaGraphArea) {
			outString += (Double.toString(d) + " ");
		}
		
		for (double d : juliaParams) {
			outString += (Double.toString(d) + " ");
		}
		
		return outString;		
	}
}