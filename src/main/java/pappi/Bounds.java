package pappi;

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
	
	public double getUpperBound() {
		return this.upperBound;
	}
}
