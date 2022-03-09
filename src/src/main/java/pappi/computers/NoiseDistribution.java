package pappi.computers;

import java.util.List;
import java.util.Map;

import es.us.isa.ppinot.evaluation.Measure;

//TODO check if this is redundant with aggregators.OutputDistribution.java
public interface NoiseDistribution {
	public List<? extends Measure> getMeasureDistributions();
}
