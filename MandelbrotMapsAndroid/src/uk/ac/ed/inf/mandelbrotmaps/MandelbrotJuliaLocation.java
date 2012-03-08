package uk.ac.ed.inf.mandelbrotmaps;


class MandelbrotJuliaLocation {
	//private URL urlBase;
	private double[] mandelbrotGraphArea;
	private double[] juliaGraphArea;
	private double[] juliaParams;
	private int mandelbrotContrast = -1;
	private int juliaContrast = -1;
	
	// Constructor. Defaults - some semi-arbitrary, pretty values
	public MandelbrotJuliaLocation() {
		mandelbrotGraphArea = new double[] {-3.1, 1.45, 5};
		juliaGraphArea = new double[] {-2.2, 1.25, 4.3};
		juliaParams = new double[] {0.152, 0.584};
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
	
	public int getMandelbrotContrast() {
		return mandelbrotContrast;
	}
	
	public int getJuliaContrast() {
		return juliaContrast;
	}
}