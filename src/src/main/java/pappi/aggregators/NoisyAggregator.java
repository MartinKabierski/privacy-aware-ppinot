package pappi.aggregators;

import java.util.Collection;
import java.util.Map;

/**
 * 
 * @author Martin Kabierski
 *
 */
public interface NoisyAggregator{
	public double aggregate(String function,Collection<Double> values, double epsilon, double lowerBound, double upperBound);
}
