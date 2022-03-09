package pappi.measureDefinitions;

import es.us.isa.ppinot.model.derived.DerivedMultiInstanceMeasure;

public class PrivacyAwareDerivedMultiInstanceMeasure extends DerivedMultiInstanceMeasure{

    public static final String SUBSAMPLE = "SubsampleAggregate";
    public static final String STANDARD = "Standard";

	
	 public String mode = this.STANDARD;
	 public double epsilon;
	 
	 public PrivacyAwareDerivedMultiInstanceMeasure() {
		 super();
		 this.epsilon= 1.0;
	 }
	
	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public String getMode() {
		return this.mode;
	}
	
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public double getEpsilon() {
		return this.epsilon;
	}
	
}
