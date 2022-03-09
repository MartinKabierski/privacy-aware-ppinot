package pappi.aggregators.laplace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.LaplaceDistribution;

import es.us.isa.ppinot.evaluation.Aggregator;
import pappi.aggregators.NoisyAggregator;

public class LaplaceAggregator implements NoisyAggregator{
	
    public static final String SUM_LAP = "Sum_lap";
    public static final String AVG_LAP = "Average_lap";
    public static final String MAX_LAP = "Maximum_lap";
    public static final String MIN_LAP = "Minimum_lap";
	
	public LaplaceAggregator() {
	}

	public double aggregate(String function, Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		if (function == SUM_LAP) {
			return sum(values, epsilon, lowerBound, upperBound);
		}
		else if (function == AVG_LAP) {
			return mean(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MAX_LAP) {
			return max(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MIN_LAP) {
			return min(values, epsilon, lowerBound, upperBound);
		}
		else return Double.NaN;
	}
	
	public double mean(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		Double sensitivity = (upperBound-lowerBound)/values.size();
		//catch corner cases, where all cases have same value or bounds are the same
		if(sensitivity == 0 || sensitivity.isNaN())return Double.NaN;
		
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound, 1.0);
    		return lowerBound*values.size();
		}
		
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		
		double mean = new Aggregator(Aggregator.AVG).aggregate(values);

		double noisyMean = mean + laplace.sample();
		noisyMean = Math.max(lowerBound, Math.min(noisyMean, upperBound));
		
        return noisyMean;
	}
	

	public double sum(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound;
		//catch corner cases, where all cases have same value or bounds are the same
		if(sensitivity == 0)sensitivity=1;
		
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		
        double result = 0;
        boolean processed = false;
        for (Double v: values) {
            if (!v.isNaN()) {
                result += v;
                processed = true;
            }
        }
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound * values.size(), 1.0);
    		return lowerBound*values.size();
		}
        
        result+=laplace.sample();
        result = Math.max(lowerBound*values.size(), Math.min(result, upperBound*values.size()));
        
		return processed ? result : Double.NaN;
	}
	
	
	public double min(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound-lowerBound;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);

		
		double min = Double.MAX_VALUE;
        for (Double v : values) {
            if (v < min) {
                min = v;
            }
        }
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound, 1.0);
    		return lowerBound*values.size();
		}
        
        min += (int)laplace.sample();
        min = Math.max(lowerBound, Math.min(min, upperBound));
        return min;
	}
	
	
	public double max(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound-lowerBound;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		
		
		double max = Double.MIN_VALUE;
        for (Double v: values) {
            if (v > max) {
                max = v;
            }
        }
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound, 1.0);
    		return lowerBound*values.size();
		}
        
        max +=(int)laplace.sample();
        max = Math.max(lowerBound, Math.min(max, upperBound));
        return max;
	}
}
