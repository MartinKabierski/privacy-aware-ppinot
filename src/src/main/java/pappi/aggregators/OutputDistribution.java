package pappi.aggregators;

import java.util.Collection;
import java.util.Map;

public interface OutputDistribution {
	Map<Double,Double> getOutputProbability(String function, Collection<Double> values, double epsilon, double lowerBound, double upperBound);
}
