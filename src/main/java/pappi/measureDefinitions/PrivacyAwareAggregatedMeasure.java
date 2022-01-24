package pappi.measureDefinitions;

import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;

public class PrivacyAwareAggregatedMeasure extends AggregatedMeasure{
	
	private double epsilon;
	
	//boundary parameter, semantics are based on used base measure type and log.
	private double lowerBound;
	private double upperBound;
	
	private double target;
	private String boundaryEstimation;
	private double falloff;
	private double extensionFactor;
	private int minimalSize;

	
	public PrivacyAwareAggregatedMeasure() {
		super();
		this.epsilon=Double.NaN;
		this.lowerBound=Double.NaN;
		this.upperBound=Double.NaN;
		this.target=Double.NaN;
		this.boundaryEstimation=null;
		this.falloff=Double.NaN;
		this.extensionFactor=Double.NaN;
		this.minimalSize=-1;
	}
	
	public void setEpsilon(double epsilon) {
		this.epsilon=epsilon;
	}
	
	public double getEpsilon() {
		return this.epsilon;
	}
	
	public void setLowerBounds(double lowerBound) {
		this.lowerBound=lowerBound;
	}
	
	public double getLowerBound() {
		return this.lowerBound;
	}
	
	public void setUpperBound(double upperBound) {
		this.upperBound=upperBound;
	}
	
	public double getUpperBound() {
		return this.upperBound;
	}
	
	public void setTarget(double target) {
		this.target=target;
	}
	
	public double getTarget() {
		return this.target;
	}

	public void setBoundaryEstimation(String estimator) {
		this.boundaryEstimation=estimator;
	}
	
	public String getBoundaryEstimation() {
		return this.boundaryEstimation;
	}

	public void setFalloff(double falloff) {
		this.falloff=falloff;
	}
	
	public double getFalloff() {
		return this.falloff;
	}

	public void setMinimalSize(int minimalSize) {
		this.minimalSize=minimalSize;
	}
	
	public int getMinimalSize() {
		return this.minimalSize;
	}

	public void setExtensionFactor(double factor) {
		this.extensionFactor = factor;
	}
	
	public double getExtensionFactor() {
		return this.extensionFactor;
	}

}
