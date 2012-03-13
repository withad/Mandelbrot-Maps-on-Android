package uk.ac.ed.inf.mandelbrotmaps;


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
}