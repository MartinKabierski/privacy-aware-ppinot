package pappi.boundary;

public class Bounds {

	double lowerBound;
	double upperBound;
	
	public Bounds(double lowerBound, double upperBound) {
		this.lowerBound=lowerBound;
		this.upperBound=upperBound;
	}
	
	public double getLowerBound() {
		return this.lowerBound;
	}
	
	public void setLowerBound(double lowerBound) {
		this.lowerBound = lowerBound;
	}
	
	public double getUpperBound() {
		return this.upperBound;
	}
	
	public void setUpperBound(double upperBound) {
		this.upperBound = upperBound;
	}
}
